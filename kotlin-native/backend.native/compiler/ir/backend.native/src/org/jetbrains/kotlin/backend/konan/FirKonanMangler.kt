/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmMangleComputer
import org.jetbrains.kotlin.fir.declarations.*

class FirKonanMangler : FirJvmKotlinMangler() {
    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<FirDeclaration> {
        return FirKonanMangleComputer(StringBuilder(256), mode)
    }
}

class FirKonanMangleComputer(
        builder: StringBuilder,
        mode: MangleMode,
) : FirJvmMangleComputer(builder, mode) {
    /**
     *  mimics FunctionDescriptor.platformSpecificFunctionName()
     */
    override fun FirFunction.platformSpecificFunctionName(): String? {
        unwrapPossibleConstructor()?.getObjCMethodInfo(onlyExternal = false)?.let {
            return buildString {
                if (receiverParameter != null) {
                    // TODO KT-56030: mimic `extensionReceiverParameter!!.type.constructor.declarationDescriptor!!.name` for next line
                    append(receiverParameter!!.typeRef.toString())
                    append(".")
                }

                append("objc:")
                append(it.selector)
                if ((this@platformSpecificFunctionName is FirConstructor) && isObjCConstructor) append("#Constructor")

                if (this@platformSpecificFunctionName is FirPropertyAccessor) {
                    append("#Accessor")
                }
            }
        }
        return null
    }
}