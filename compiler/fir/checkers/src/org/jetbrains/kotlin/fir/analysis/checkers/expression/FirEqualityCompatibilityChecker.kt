/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.isValueClass
import org.jetbrains.kotlin.fir.declarations.utils.isClass
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.resolve.isSubclassOf
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds

object FirEqualityCompatibilityChecker : FirEqualityOperatorCallChecker() {
    override fun check(expression: FirEqualityOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val arguments = expression.argumentList.arguments
        if (arguments.size != 2) error("Equality operator call with non-2 arguments")

        val (lArgument, lType) = arguments[0].selfWithMostOriginalTypeIfSmartCast
        val (rArgument, rType) = arguments[1].selfWithMostOriginalTypeIfSmartCast

        val lSmartCastType = lArgument.typeRef.coneType
        val rSmartCastType = rArgument.typeRef.coneType

        checkSensibleness(lSmartCastType, rSmartCastType, context, expression, reporter)

        val checkApplicability = when (expression.operation) {
            FirOperation.EQ, FirOperation.NOT_EQ -> ::checkEqualityApplicability
            FirOperation.IDENTITY, FirOperation.NOT_IDENTITY -> ::checkIdentityApplicability
            else -> error("Invalid operator of FirEqualityOperatorCall")
        }

        checkApplicability(lType, rType, context).ifInapplicable {
            return reporter.reportOn(
                expression, it, expression.operation,
                isWarning = false, lType, rType, context,
            )
        }

        if (lArgument !is FirSmartCastExpression && rArgument !is FirSmartCastExpression) {
            return
        }

        checkApplicability(lSmartCastType, rSmartCastType, context).ifInapplicable {
            return reporter.reportOn(
                expression, it, expression.operation,
                isWarning = true, lSmartCastType, rSmartCastType, context,
            )
        }
    }

    private val FirExpression.selfWithMostOriginalTypeIfSmartCast
        get() = this to mostOriginalTypeIfSmartCast

    private val FirExpression.mostOriginalTypeIfSmartCast: ConeKotlinType
        get() = when (this) {
            is FirSmartCastExpression -> originalExpression.mostOriginalTypeIfSmartCast
            else -> typeRef.coneType
        }

    private fun checkEqualityApplicability(
        lType: ConeKotlinType,
        rType: ConeKotlinType,
        context: CheckerContext,
    ): Applicability {
        val oneIsBuiltin = lType.isBuiltin(context) || rType.isBuiltin(context)
        val oneIsNotNull = !lType.isNullable || !rType.isNullable

        // The compiler should only check comparisons
        // when builtins are involved.

        return when {
            !oneIsBuiltin || !oneIsNotNull -> Applicability.APPLICABLE
            !shouldReportAsPerRules1(lType, rType, context) -> Applicability.APPLICABLE
            // Note: FE1.0 reports INCOMPATIBLE_ENUM_COMPARISON_ERROR only when TypeIntersector.isIntersectionEmpty() thinks the
            // given types are compatible. Exactly mimicking the behavior of FE1.0 is difficult and does not seem to provide any
            // value. So instead, we deterministically output INCOMPATIBLE_ENUM_COMPARISON_ERROR if at least one of the value is an
            // enum.
            lType.isEnumType(context) || rType.isEnumType(context) -> Applicability.INAPPLICABLE_AS_ENUMS
            else -> Applicability.GENERALLY_INAPPLICABLE
        }
    }

    private fun ConeKotlinType.isBuiltin(context: CheckerContext) = isPrimitiveOrNullablePrimitive || isString || isEnumType(context)

    private fun checkIdentityApplicability(
        lType: ConeKotlinType,
        rType: ConeKotlinType,
        context: CheckerContext,
    ): Applicability {
        // The compiler should only check comparisons
        // when identity-less types are involved.

        return if (lType.isIdentityLess(context) || rType.isIdentityLess(context)) {
            Applicability.INAPPLICABLE_AS_IDENTITY_LESS
        } else {
            Applicability.APPLICABLE
        }
    }

    private fun ConeKotlinType.isIdentityLess(context: CheckerContext) = isPrimitive || !isNullable && isValueClass(context.session)

    private fun shouldReportAsPerRules1(lType: ConeKotlinType, rType: ConeKotlinType, context: CheckerContext): Boolean {
        val lClass = lType.representativeClassType(context)
        val rClass = rType.representativeClassType(context)

        return areUnrelatedClasses(lClass, rClass, context)
                || areInterfaceAndUnrelatedFinalClassAccordingly(lClass, rClass, context)
                || areInterfaceAndUnrelatedFinalClassAccordingly(rClass, lClass, context)
    }

    private fun areUnrelatedClasses(lClass: FirClassSymbol<*>, rClass: FirClassSymbol<*>, context: CheckerContext): Boolean {
        fun FirClassSymbol<*>.isSubclassOf(other: FirClassSymbol<*>) =
            isSubclassOf(other.toLookupTag(), context.session, isStrict = false, lookupInterfaces = true)

        return when {
            !lClass.isClass || !rClass.isClass -> false
            lClass.isNothing || rClass.isNothing -> false
            lClass.isFinal && rClass.isFinal && lClass != rClass -> true
            else -> !lClass.isSubclassOf(rClass) && !rClass.isSubclassOf(lClass)
        }
    }

    private fun areInterfaceAndUnrelatedFinalClassAccordingly(
        lClass: FirClassSymbol<*>,
        rClass: FirClassSymbol<*>,
        context: CheckerContext,
    ): Boolean {
        return lClass.isInterface && rClass.isFinalClass && !rClass.isNothing && !rClass.isSubclassOf(
            lClass.toLookupTag(), context.session, isStrict = false, lookupInterfaces = true,
        )
    }

    private val FirClassSymbol<*>.isNothing get() = classId == StandardClassIds.Nothing

    private val FirClassSymbol<*>.isFinalClass get() = isClass && isFinal

    private fun ConeKotlinType.representativeClassType(context: CheckerContext): FirClassSymbol<*> {
        val symbol = toSymbol(context.session)

        if (symbol is FirClassSymbol<*> && (symbol.isClass || symbol.isInterface)) {
            return symbol
        }

        return representativeClassType(context.session, mutableMapOf())
    }

    private fun ConeKotlinType.representativeClassType(
        session: FirSession,
        cache: MutableMap<ConeKotlinType, FirClassSymbol<*>>,
    ): FirClassSymbol<*> = cache.getOrPut(this) {
        when (val symbol = toSymbol(session)) {
            is FirClassSymbol<*> -> when {
                symbol.isClass -> symbol
                else -> symbol.resolvedSuperTypes.firstNonAnyRepresentativeClass(session, cache)
            }
            is FirTypeParameterSymbol -> symbol.resolvedBounds.map { it.type }.firstNonAnyRepresentativeClass(session, cache)
            is FirTypeAliasSymbol -> symbol.resolvedExpandedTypeRef.type.representativeClassType(session, cache)
            else -> session.anyClassSymbol
        }
    }

    private fun List<ConeKotlinType>.firstNonAnyRepresentativeClass(
        session: FirSession,
        cache: MutableMap<ConeKotlinType, FirClassSymbol<*>>,
    ): FirClassSymbol<*> {
        return firstNotNullOfOrNull { type ->
            type.representativeClassType(session, cache).takeIf {
                it != session.anyClassSymbol
            }
        } ?: session.anyClassSymbol
    }

    private val FirSession.anyClassSymbol
        get() = builtinTypes.anyType.coneType.toSymbol(this) as? FirClassSymbol<*>
            ?: error("Any type symbol is not a class symbol")

    private enum class Applicability {
        APPLICABLE,
        GENERALLY_INAPPLICABLE,
        INAPPLICABLE_AS_ENUMS,
        INAPPLICABLE_AS_IDENTITY_LESS,
    }

    private inline fun Applicability.ifInapplicable(block: (Applicability) -> Unit) = when (this) {
        Applicability.APPLICABLE -> {}
        else -> block(this)
    }

    private fun getGeneralInapplicabilityDiagnostic(isWarning: Boolean) = when {
        isWarning -> FirErrors.EQUALITY_NOT_APPLICABLE_WARNING
        else -> FirErrors.EQUALITY_NOT_APPLICABLE
    }

    private fun getIdentityLessInapplicabilityDiagnostic(
        lType: ConeKotlinType,
        rType: ConeKotlinType,
        isWarning: Boolean,
        context: CheckerContext,
    ): KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> {
        val areBothPrimitives = lType.isPrimitiveOrNullablePrimitive && rType.isPrimitiveOrNullablePrimitive
        val isAnyPrimitive = lType.isPrimitiveOrNullablePrimitive || rType.isPrimitiveOrNullablePrimitive
        val onlyOneIsPrimitive = isAnyPrimitive && !areBothPrimitives
        val areSameTypes = lType.classId == rType.classId
        val shouldProperlyReportError = context.languageVersionSettings.supportsFeature(LanguageFeature.ReportErrorsForComparisonOperators)

        // K1 reports DEPRECATED_IDENTITY_EQUALS for pairs of primitives,
        // and IMPLICIT_BOXING_IN_IDENTITY_EQUALS when only one type
        // is a primitive.
        val shouldRelaxDiagnostic = (areSameTypes && isAnyPrimitive || onlyOneIsPrimitive) && !shouldProperlyReportError

        return when {
            isWarning || shouldRelaxDiagnostic -> FirErrors.FORBIDDEN_IDENTITY_EQUALS_WARNING
            else -> FirErrors.FORBIDDEN_IDENTITY_EQUALS
        }
    }

    private fun getSourceLessInapplicabilityDiagnostic(isWarning: Boolean) = when {
        isWarning -> FirErrors.INCOMPATIBLE_TYPES_WARNING
        else -> FirErrors.INCOMPATIBLE_TYPES
    }

    private fun getEnumInapplicabilityDiagnostic(
        isWarning: Boolean,
        context: CheckerContext,
    ): KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> {
        val shouldProperlyReportError = context.languageVersionSettings.supportsFeature(LanguageFeature.ReportErrorsForComparisonOperators)

        return when {
            isWarning || !shouldProperlyReportError -> FirErrors.INCOMPATIBLE_ENUM_COMPARISON
            else -> FirErrors.INCOMPATIBLE_ENUM_COMPARISON_ERROR
        }
    }

    private fun DiagnosticReporter.reportOn(
        expression: FirEqualityOperatorCall,
        applicability: Applicability,
        operation: FirOperation,
        isWarning: Boolean,
        lType: ConeKotlinType,
        rType: ConeKotlinType,
        context: CheckerContext,
    ): Unit = when {
        applicability == Applicability.INAPPLICABLE_AS_IDENTITY_LESS -> reportOn(
            expression.source, getIdentityLessInapplicabilityDiagnostic(lType, rType, isWarning, context),
            lType, rType, context,
        )
        applicability == Applicability.INAPPLICABLE_AS_ENUMS -> reportOn(
            expression.source, getEnumInapplicabilityDiagnostic(isWarning, context),
            lType, rType, context,
        )
        expression.source?.kind !is KtRealSourceElementKind -> reportOn(
            expression.source, getSourceLessInapplicabilityDiagnostic(isWarning),
            lType, rType, context,
        )
        applicability == Applicability.GENERALLY_INAPPLICABLE -> reportOn(
            expression.source, getGeneralInapplicabilityDiagnostic(isWarning),
            operation.operator, lType, rType, context,
        )
        else -> error("Shouldn't be here")
    }

    private fun ConeKotlinType.isEnumType(
        context: CheckerContext
    ): Boolean {
        if (isEnum) return true
        val firRegularClassSymbol = (this as? ConeClassLikeType)?.lookupTag?.toFirRegularClassSymbol(context.session) ?: return false
        return firRegularClassSymbol.isEnumClass
    }

    private fun checkSensibleness(
        lType: ConeKotlinType,
        rType: ConeKotlinType,
        context: CheckerContext,
        expression: FirEqualityOperatorCall,
        reporter: DiagnosticReporter
    ) {
        val type = when {
            rType.isNullableNothing -> lType
            lType.isNullableNothing -> rType
            else -> return
        }
        if (type is ConeErrorType) return
        val isPositiveCompare = expression.operation == FirOperation.EQ || expression.operation == FirOperation.IDENTITY
        val compareResult = with(context.session.typeContext) {
            when {
                // `null` literal has type `Nothing?`
                type.isNullableNothing -> isPositiveCompare
                !type.isNullableType() -> !isPositiveCompare
                else -> return
            }
        }
        // We only report `SENSELESS_NULL_IN_WHEN` if `lType = type` because `lType` is the type of the when subject. This diagnostic is
        // only intended for cases where the branch condition contains a null. Also, the error message for SENSELESS_NULL_IN_WHEN
        // says the value is *never* equal to null, so we can't report it if the value is *always* equal to null.
        if (expression.source?.elementType != KtNodeTypes.BINARY_EXPRESSION && type === lType && !compareResult) {
            reporter.reportOn(expression.source, FirErrors.SENSELESS_NULL_IN_WHEN, context)
        } else {
            reporter.reportOn(expression.source, FirErrors.SENSELESS_COMPARISON, expression, compareResult, context)
        }
    }
}
