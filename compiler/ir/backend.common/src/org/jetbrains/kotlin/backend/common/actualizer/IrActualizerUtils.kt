/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.backend.common.CommonBackendErrors
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.module

fun checkParameters(
    expectFunction: IrFunction,
    actualFunction: IrFunction,
    expectActualTypesMap: Map<IrSymbol, IrSymbol>
): Boolean {
    fun checkParameter(expectParameter: IrValueParameter?, actualParameter: IrValueParameter?): Boolean {
        if (expectParameter == null) {
            return actualParameter == null
        }
        if (actualParameter == null) {
            return false
        }

        val expectParameterTypeSymbol = expectParameter.type.classifierOrFail
        val actualizedParameterTypeSymbol = expectActualTypesMap[expectParameterTypeSymbol] ?: expectParameterTypeSymbol
        if (actualizedParameterTypeSymbol != actualParameter.type.classifierOrFail) {
            return false
        }
        return true
    }

    if (expectFunction.valueParameters.size != actualFunction.valueParameters.size ||
        !checkParameter(expectFunction.extensionReceiverParameter, actualFunction.extensionReceiverParameter)
    ) {
        return false
    }
    for ((expectParameter, actualParameter) in expectFunction.valueParameters.zip(actualFunction.valueParameters)) {
        if (!checkParameter(expectParameter, actualParameter)) {
            return false
        }
    }

    return true
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun KtDiagnosticReporterWithImplicitIrBasedContext.reportMissingActual(irDeclaration: IrDeclaration) {
    at(irDeclaration).report(CommonBackendErrors.NO_ACTUAL_FOR_EXPECT, irDeclaration.module)
}

fun KtDiagnosticReporterWithImplicitIrBasedContext.reportManyInterfacesMembersNotImplemented(declaration: IrClass, actualMember: IrDeclarationWithName) {
    at(declaration).report(CommonBackendErrors.MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED, actualMember.name.asString())
}