/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.substitution.chain
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

fun createExpectActualTypeParameterSubstitutor(
    expectedTypeParameters: List<FirTypeParameterSymbol>,
    actualTypeParameters: List<FirTypeParameterSymbol>,
    useSiteSession: FirSession,
    parentSubstitutor: ConeSubstitutor? = null
): ConeSubstitutor {
    val substitution = expectedTypeParameters.zip(actualTypeParameters).associate { (expectedParameterSymbol, actualParameterSymbol) ->
        expectedParameterSymbol to actualParameterSymbol.toLookupTag().constructType(emptyArray(), isNullable = false)
    }
    val substitutor = ConeSubstitutorByMap(
        substitution,
        useSiteSession
    )
    if (parentSubstitutor == null) {
        return substitutor
    }
    return substitutor.chain(parentSubstitutor)
}

fun areCompatibleExpectActualTypes(
    expectedType: ConeKotlinType?,
    actualType: ConeKotlinType?,
    expectSession: FirSession,
    actualSession: FirSession
): Boolean {
    if (expectedType == null) return actualType == null
    if (actualType == null) return false

    val typeCheckerContext = ConeInferenceContextForExpectActual(expectSession, actualSession).newTypeCheckerState(
        errorTypesEqualToAnything = false,
        stubTypesEqualToAnything = true
    )
    return AbstractTypeChecker.equalTypes(
        typeCheckerContext,
        expectedType,
        actualType
    )
}

private class ConeInferenceContextForExpectActual(val expectSession: FirSession, val actualSession: FirSession) : ConeInferenceContext {
    override val session: FirSession
        get() = actualSession

    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        if (c1 !is ConeClassifierLookupTag || c2 !is ConeClassifierLookupTag) {
            return c1 == c2
        }
        return isExpectedClassAndActualTypeAlias(c1, c2) ||
                isExpectedClassAndActualTypeAlias(c2, c1) ||
                c1 == c2
    }

    // For example, expectedTypeConstructor may be the expected class kotlin.text.StringBuilder, while actualTypeConstructor
    // is java.lang.StringBuilder. For the purposes of type compatibility checking, we must consider these types equal here.
    // Note that the case of an "actual class" works as expected though, because the actual class by definition has the same FQ name
    // as the corresponding expected class, so their type constructors are equal as per AbstractClassTypeConstructor#equals
    private fun isExpectedClassAndActualTypeAlias(
        expectLookupTag: ConeClassifierLookupTag,
        actualLookupTag: ConeClassifierLookupTag
    ): Boolean {
        val expectDeclaration = expectLookupTag.toClassLikeDeclaration(expectSession) ?: return false
        val actualDeclaration = actualLookupTag.toClassLikeDeclaration(actualSession) ?: return false

        if (!expectDeclaration.isExpect) return false
        val expectClassId = when (expectDeclaration) {
            is FirRegularClassSymbol -> expectDeclaration.classId
            is FirTypeAliasSymbol -> expectDeclaration.resolvedExpandedTypeRef.coneType.classId
            else -> null
        } ?: return false
        return expectClassId == actualDeclaration.classId
    }

    private fun ConeClassifierLookupTag.toClassLikeDeclaration(session: FirSession): FirClassLikeSymbol<*>? {
        return this.toSymbol(session) as? FirClassLikeSymbol<*>
    }
}