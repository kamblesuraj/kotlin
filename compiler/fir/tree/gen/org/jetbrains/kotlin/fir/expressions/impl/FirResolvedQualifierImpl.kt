/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirResolvedQualifierImpl(
    override val source: KtSourceElement?,
    override var typeRef: FirTypeRef,
    override val annotations: MutableList<FirAnnotation>,
    override var packageFqName: FqName,
    override var relativeClassFqName: FqName?,
    override val symbol: FirClassLikeSymbol<*>?,
    override var isNullableLHSForCallableReference: Boolean,
    override val nonFatalDiagnostics: MutableList<ConeDiagnostic>,
    override val typeArguments: MutableList<FirTypeProjection>,
) : FirResolvedQualifier() {
    override val classId: ClassId? get() = relativeClassFqName?.let {
    ClassId(packageFqName, it, false)
}
    override var resolvedToCompanionObject: Boolean = (symbol?.fir as? FirRegularClass)?.companionObjectSymbol != null

    override val elementKind get() = FirElementKind.ResolvedQualifier

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceIsNullableLHSForCallableReference(newIsNullableLHSForCallableReference: Boolean) {
        isNullableLHSForCallableReference = newIsNullableLHSForCallableReference
    }

    override fun replaceResolvedToCompanionObject(newResolvedToCompanionObject: Boolean) {
        resolvedToCompanionObject = newResolvedToCompanionObject
    }

    override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>) {
        typeArguments.clear()
        typeArguments.addAll(newTypeArguments)
    }
}
