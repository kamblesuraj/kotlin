/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.types.Variance

object FirActualTypeAliasChecker : FirTypeAliasChecker() {
    override fun check(declaration: FirTypeAlias, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isActual) return

        val expandedTypeSymbol = declaration.expandedTypeRef.toClassLikeSymbol(context.session) ?: return

        if (expandedTypeSymbol is FirTypeAliasSymbol) {
            reporter.reportOn(declaration.source, FirErrors.ACTUAL_TYPE_ALIAS_NOT_TO_CLASS, context)
        }

        for (typeParameterSymbol in expandedTypeSymbol.typeParameterSymbols) {
            if (typeParameterSymbol.variance != Variance.INVARIANT) {
                reporter.reportOn(declaration.source, FirErrors.ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE, context)
                break
            }
        }
    }
}