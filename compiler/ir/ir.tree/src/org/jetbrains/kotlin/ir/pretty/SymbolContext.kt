/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl

class SymbolContext {

    private class SymbolMap<Symbol : IrSymbol> {
        private val string2SymbolMap = mutableMapOf<String, Symbol>()
        private val symbol2StringMap = mutableMapOf<Symbol, String>()

        inline fun getOrCreateSymbol(
            name: String,
            symbolConstructor: () -> Symbol,
        ): Symbol {
            string2SymbolMap[name]?.let { return it }
            val symbol = symbolConstructor()
            string2SymbolMap[name] = symbol
            symbol2StringMap[symbol] = name
            return symbol
        }
    }

    private val classSymbolMap = SymbolMap<IrClassSymbol>()
    internal fun classSymbol(name: String): IrClassSymbol = classSymbolMap.getOrCreateSymbol(name, ::IrClassSymbolImpl)
}
