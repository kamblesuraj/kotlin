/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name


internal val FirConstructor.isObjCConstructor get() = this.annotations.hasAnnotation(ClassId.topLevel(objCConstructorFqName), moduleData.session)

internal fun FirFunction.unwrapPossibleConstructor(): FirFunction? =
        if (this is FirConstructor && this.annotations.hasAnnotation(ClassId.topLevel(objCConstructorFqName), moduleData.session))
            getObjCInitMethod()
        else
            this

/**
 *  mimics FunctionDescriptor.getObjCMethodInfo()
 */
internal fun FirFunction.getObjCMethodInfo(onlyExternal: Boolean): ObjCMethodInfo? {
    if (!isSubstitutionOrIntersectionOverride) {
        this.decodeObjCMethodAnnotation()?.let { return it }

        if (onlyExternal) {
            return null
        }
    }

    // TODO KT-56030: to fix test `interop_objc_smoke` for K2, implement ObjCMethod annotation search among overridden functions,
    //   like K1 did in `FunctionDescriptor.getObjCMethodInfo()`:
    // return overriddenDescriptors.firstNotNullOfOrNull { it.getObjCMethodInfo(onlyExternal) }
    //   Approx. plan for K2 implementation (to discuss on K2 sync meetings):
    // get `session: FirSession` and `scopeSession: ScopeSession` from somewhere in space/time
    // get scope as:  `classSymbol.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false)`
    // from scope, invoke `getDirectOverriddenFunctions()`
    return null
}

/**
 * mimics ConstructorDescriptor.getObjCInitMethod()
 */
private fun FirConstructor.getObjCInitMethod(): FirFunction? {
    val session = moduleData.session // FIXME KT-56030 wrong session! needed session for currently compiled module
    this.annotations.getAnnotationByClassId(ClassId.topLevel(objCConstructorFqName), session)?.let {
        val initSelector: String = it.constStringArgument("initSelector")
        val containingClass = this.getContainingClass(session) ?: error("Expected containingClass for constructor $this")
        // FIXME KT-56030 iteration below does not involve constructors of super classes (btw, do we need them here?)
        // Approx.plan for K2 implementation (to discuss on K2 sync meetings):
        // get FirSession and ScopeSession from somewhere in space/time
        // klass.scopeForClass(ConeSubstitutor.Empty, FirSession, ScopeSession, klass.symbol.toLookupTag()).processAllFunctions {...}
        return containingClass.declarations.mapNotNull { it as? FirFunction }
                .find { function ->
                    function.getObjCMethodInfo(onlyExternal = true)?.selector == initSelector
                } ?: error("Cannot find ObjInitMethod for $this")
    }
    return null
}

/**
 * mimics FunctionDescriptor.decodeObjCMethodAnnotation()
 */
private fun FirFunction.decodeObjCMethodAnnotation(): ObjCMethodInfo? {
    assert(!isSubstitutionOrIntersectionOverride)
    return annotations.getAnnotationByClassId(ClassId.topLevel(objCMethodFqName), moduleData.session)?.toObjCMethodInfo()
}

private fun FirAnnotation.toObjCMethodInfo() = ObjCMethodInfo(
        selector = constStringArgument("selector"),
        encoding = constStringArgument("encoding"),
        isStret = constBooleanArgumentOrNull("isStret") ?: false
)

private fun FirAnnotation.constStringArgument(argumentName: String): String =
        constArgument(argumentName) as? String ?: error("Expected string constant value of argument '$argumentName' at annotation $this")

private fun FirAnnotation.constBooleanArgumentOrNull(argumentName: String): Boolean? =
        constArgument(argumentName) as? Boolean

private fun FirAnnotation.constArgument(argumentName: String) =
        (argumentMapping.mapping[Name.identifier(argumentName)] as? FirConstExpression<*>)?.value
