/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.types.FirTypeRef

open class FirAnnotationArgumentsResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    resolvePhase: FirResolvePhase,
    outerBodyResolveContext: BodyResolveContext? = null,
    returnTypeCalculator: ReturnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve,
) : FirAbstractBodyResolveTransformerDispatcher(
    session,
    resolvePhase,
    implicitTypeOnly = false,
    scopeSession,
    outerBodyResolveContext = outerBodyResolveContext,
    returnTypeCalculator = returnTypeCalculator,
) {
    final override val expressionsTransformer: FirExpressionsResolveTransformer = FirExpressionsResolveTransformerForSpecificAnnotations(this)

    final override val declarationsTransformer: FirDeclarationsResolveTransformer = FirDeclarationsResolveTransformerForArgumentAnnotations(this)
}

private class FirDeclarationsResolveTransformerForArgumentAnnotations(
    transformer: FirAbstractBodyResolveTransformerDispatcher
) : FirDeclarationsResolveTransformer(transformer) {
    override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): FirStatement {
        regularClass.transformAnnotations(this, data)
        withRegularClass(regularClass) {
            regularClass
                .transformTypeParameters(transformer, data)
                .transformSuperTypeRefs(transformer, data)
                .transformDeclarations(transformer, data)
        }
        return regularClass
    }

    override fun withRegularClass(regularClass: FirRegularClass, action: () -> FirRegularClass): FirRegularClass {
        return context.withContainingClass(regularClass) {
            context.withRegularClass(regularClass, components) {
                action()
            }
        }
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: ResolutionMode
    ): FirAnonymousInitializer {
        return anonymousInitializer
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: ResolutionMode
    ): FirSimpleFunction {
        simpleFunction
            .transformReturnTypeRef(transformer, data)
            .transformReceiverParameter(transformer, data)
            .transformValueParameters(transformer, data)
            .transformAnnotations(transformer, data)
        return simpleFunction
    }

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): FirConstructor {
        constructor
            .transformReturnTypeRef(transformer, data)
            .transformReceiverParameter(transformer, data)
            .transformValueParameters(transformer, data)
            .transformAnnotations(transformer, data)
        return constructor
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: ResolutionMode): FirStatement {
        valueParameter
            .transformAnnotations(transformer, data)
            .transformReturnTypeRef(transformer, data)
        return valueParameter
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): FirProperty {
        property
            .transformAnnotations(transformer, data)
            .transformReceiverParameter(transformer, data)
            .transformReturnTypeRef(transformer, data)
            .transformGetter(transformer, data)
            .transformSetter(transformer, data)
            .transformTypeParameters(transformer, data)
        return property
    }

    override fun transformPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        data: ResolutionMode
    ): FirPropertyAccessor {
        propertyAccessor
            .transformValueParameters(transformer, data)
            .transformReturnTypeRef(transformer, data)
            .transformReceiverParameter(transformer, data)
            .transformReturnTypeRef(transformer, data)
            .transformAnnotations(transformer, data)
        return propertyAccessor
    }

    override fun transformDeclarationStatus(declarationStatus: FirDeclarationStatus, data: ResolutionMode): FirDeclarationStatus {
        return declarationStatus
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: ResolutionMode): FirEnumEntry {
        context.forEnumEntry {
            enumEntry
                .transformAnnotations(transformer, data)
                .transformReceiverParameter(transformer, data)
                .transformReturnTypeRef(transformer, data)
                .transformTypeParameters(transformer, data)
        }
        return enumEntry
    }

    override fun transformReceiverParameter(receiverParameter: FirReceiverParameter, data: ResolutionMode): FirReceiverParameter {
        return receiverParameter.transformAnnotations(transformer, data).transformTypeRef(transformer, data)
    }

    override fun transformField(field: FirField, data: ResolutionMode): FirField {
        return field.transformAnnotations(transformer, data)
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: ResolutionMode): FirTypeAlias {
        typeAlias.transformAnnotations(transformer, data)
        typeAlias.expandedTypeRef.transform<FirTypeRef, _>(transformer, data)
        return typeAlias
    }

    override fun transformScript(script: FirScript, data: ResolutionMode): FirScript {
        return script
    }
}

private class FirExpressionsResolveTransformerForSpecificAnnotations(
    transformer: FirAbstractBodyResolveTransformerDispatcher
) : FirExpressionsResolveTransformer(transformer) {

    override fun transformAnnotation(annotation: FirAnnotation, data: ResolutionMode): FirStatement {
        dataFlowAnalyzer.enterAnnotation()
        annotation.transformChildren(transformer, ResolutionMode.ContextDependent)
        dataFlowAnalyzer.exitAnnotation()
        return annotation
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: ResolutionMode): FirStatement {
        return transformAnnotation(annotationCall, data)
    }

    override fun transformErrorAnnotationCall(errorAnnotationCall: FirErrorAnnotationCall, data: ResolutionMode): FirStatement {
        return transformAnnotation(errorAnnotationCall, data)
    }

    override fun transformExpression(expression: FirExpression, data: ResolutionMode): FirStatement {
        return expression.transformChildren(transformer, data) as FirStatement
    }

    override fun FirQualifiedAccessExpression.isAcceptableResolvedQualifiedAccess(): Boolean {
        return calleeReference !is FirErrorNamedReference
    }

    override fun resolveQualifiedAccessAndSelectCandidate(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        callSite: FirElement,
    ): FirStatement {
        return callResolver.resolveOnlyEnumOrQualifierAccessAndSelectCandidate(qualifiedAccessExpression, isUsedAsReceiver)
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: ResolutionMode): FirStatement {
        return functionCall
    }

    override fun transformBlock(block: FirBlock, data: ResolutionMode): FirStatement {
        return block
    }

    override fun transformThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: ResolutionMode
    ): FirStatement {
        return thisReceiverExpression
    }

    override fun transformComparisonExpression(
        comparisonExpression: FirComparisonExpression,
        data: ResolutionMode
    ): FirStatement {
        return comparisonExpression
    }

    override fun transformTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: ResolutionMode
    ): FirStatement {
        return typeOperatorCall
    }

    override fun transformCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: ResolutionMode
    ): FirStatement {
        return checkNotNullCall
    }

    override fun transformBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: ResolutionMode
    ): FirStatement {
        return binaryLogicExpression
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: ResolutionMode
    ): FirStatement {
        return variableAssignment
    }

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: ResolutionMode
    ): FirStatement {
        return callableReferenceAccess
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: ResolutionMode
    ): FirStatement {
        return delegatedConstructorCall
    }

    override fun transformAugmentedArraySetCall(
        augmentedArraySetCall: FirAugmentedArraySetCall,
        data: ResolutionMode
    ): FirStatement {
        return augmentedArraySetCall
    }

    override fun transformArrayOfCall(arrayOfCall: FirArrayOfCall, data: ResolutionMode): FirStatement {
        arrayOfCall.transformChildren(transformer, data)
        return arrayOfCall
    }

    override fun shouldComputeTypeOfGetClassCallWithNotQualifierInLhs(getClassCall: FirGetClassCall): Boolean {
        return false
    }
}
