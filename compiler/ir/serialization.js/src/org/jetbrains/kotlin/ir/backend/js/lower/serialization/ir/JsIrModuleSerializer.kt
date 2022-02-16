/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.IrModuleSerializer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.StringSignature

class JsIrModuleSerializer(
    messageLogger: IrMessageLogger,
    private val irBuiltIns: IrBuiltIns,
    private val expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    compatibilityMode: CompatibilityMode,
    val skipExpects: Boolean,
    normalizeAbsolutePaths: Boolean,
    sourceBaseDirs: Collection<String>
) : IrModuleSerializer<JsIrFileSerializer>(messageLogger, compatibilityMode, normalizeAbsolutePaths, sourceBaseDirs) {

//    private val globalDeclarationTable = JsGlobalDeclarationTable(irBuiltIns)
//    private val globalDeclarationTable = JsGlobalDeclarationTable(irBuiltIns)
    private val globalTable = mutableMapOf<IrDeclaration, StringSignature>()
    private val declarationTable = JsDeclarationTable(globalTable, irBuiltIns)



    override fun createSerializerForFile(file: IrFile): JsIrFileSerializer =
        JsIrFileSerializer(
            messageLogger,
            declarationTable,
            expectDescriptorToSymbol,
            compatibilityMode = compatibilityMode,
            skipExpects = skipExpects,
            normalizeAbsolutePaths = normalizeAbsolutePaths,
            sourceBaseDirs = sourceBaseDirs
        )
}