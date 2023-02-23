/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.scopes.PlatformSpecificOverridabilityRules
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class ObjCOverridabilityRules(val session: FirSession) : PlatformSpecificOverridabilityRules {

    override fun isOverriddenFunction(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean? {
        if (baseDeclaration.name == overrideCandidate.name) { // Slow path:
            baseDeclaration.getObjCMethodInfo(onlyExternal = false)?.let { superInfo ->
                val subInfo = overrideCandidate.getObjCMethodInfo(onlyExternal = false)
                if (subInfo != null) {
                    // Overriding Objective-C method by Objective-C method in interop stubs.
                    // Don't even check method signatures:
                    return superInfo.selector == subInfo.selector
                } else {
                    // Overriding Objective-C method by Kotlin method.
                    if (!parameterNamesMatch(baseDeclaration, overrideCandidate)) {
                        return false
                    }
                }
            }
        }
        return null
    }

    override fun isOverriddenProperty(overrideCandidate: FirCallableDeclaration, baseDeclaration: FirProperty): Boolean? {
        if (baseDeclaration.name == overrideCandidate.symbol.name) { // Slow path:
            if (baseDeclaration.isExternalObjCClassProperty() && overrideCandidate.isExternalObjCClassProperty()) {
                return true
            }
        }
        return null
    }
}

private fun FirCallableDeclaration.isExternalObjCClassProperty() = this is FirProperty &&
        true // FIXME must fully mimic CallableDescriptor.isExternalObjCClassProperty()
        // (this.containingDeclaration as? ClassDescriptor)?.isExternalObjCClass() == true

// mimics ObjCInteropKt.parameterNamesMatch()
private fun parameterNamesMatch(first: FirSimpleFunction, second: FirSimpleFunction): Boolean {
    // The original Objective-C method selector is represented as
    // function name and parameter names (except first).
    if (first.valueParameters.size != second.valueParameters.size) {
        return false
    }
    first.valueParameters.forEachIndexed { index, parameter ->
        if (index > 0 && parameter.name != second.valueParameters[index].name) {
            return false
        }
    }
    return true
}


internal fun FirFunction.getObjCMethodInfo(onlyExternal: Boolean): ObjCMethodInfo? {
    val isReal = true  // TODO KT-56030: mimic "this.kind.isReal" as K1 did in `FunctionDescriptor.getObjCMethodInfo()`
    if (isReal) {
        this.decodeObjCMethodAnnotation()?.let { return it }

        if (onlyExternal) {
            return null
        }
    }
    return null
}

private fun FirFunction.decodeObjCMethodAnnotation(): ObjCMethodInfo? {
    // TODO KT-56030: mimic `assert (this.kind.isReal)`
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
