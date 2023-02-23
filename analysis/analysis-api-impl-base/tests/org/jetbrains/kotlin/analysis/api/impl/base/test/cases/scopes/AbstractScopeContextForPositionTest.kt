/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.scopes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.KtScopeContext
import org.jetbrains.kotlin.analysis.api.components.KtScopeKind
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KtRendererAnnotationsFilter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KtDeclarationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers.KtRendererModifierFilter
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractScopeContextForPositionTest : AbstractAnalysisApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val element = testServices.expressionMarkerProvider.getSelectedElementOfType<KtElement>(ktFile)

        analyseForTest(element) { elementToAnalyze ->
            val scopeContext = ktFile.getScopeContextForPosition(elementToAnalyze)

            val scopeContextStringRepresentation = renderForTests(elementToAnalyze, scopeContext)
            val scopeContextStringRepresentationPretty = renderForTests(elementToAnalyze, scopeContext, printPretty = true)

            testServices.assertions.assertEqualsToTestDataFileSibling(scopeContextStringRepresentation)
            testServices.assertions.assertEqualsToTestDataFileSibling(scopeContextStringRepresentationPretty, extension = ".pretty.txt")
        }
    }

    private fun KtAnalysisSession.renderForTests(
        element: KtElement,
        scopeContext: KtScopeContext,
        printPretty: Boolean = false
    ): String = prettyPrint {
        appendLine("element: ${element.text}")
        appendLine("implicit receivers:")

        withIndent {
            for (implicitReceiver in scopeContext.implicitReceivers) {
                val type = implicitReceiver.type
                appendLine("type: ${if (printPretty) type.render(position = Variance.INVARIANT) else debugRenderer.renderType(type)}")
                appendLine("owner symbol: ${implicitReceiver.ownerSymbol::class.simpleName}")
                appendLine()
            }
        }
        appendLine("scopes:")
        withIndent {
            for ((scope, scopeKind) in scopeContext.scopes) {
                appendLine(renderForTests(scope, scopeKind, printPretty))
            }
        }
    }

    private fun KtAnalysisSession.renderForTests(scope: KtScope, scopeKind: KtScopeKind, printPretty: Boolean): String = prettyPrint {
        append("${scopeKind::class.simpleName}, index = ${scopeKind.indexInTower}")

        if (scopeKind is KtScopeKind.DefaultSimpleImportingScope || scopeKind is KtScopeKind.DefaultStarImportingScope) {
            appendLine()
            return@prettyPrint
        }

        val callables = scope.getCallableSymbols().toList()
        val classifiers = scope.getClassifierSymbols().toList()
        val isEmpty = callables.isEmpty() && classifiers.isEmpty()
        if (isEmpty) {
            appendLine(", empty")
        } else {
            appendLine()
            withIndent {
                appendLine("classifiers: ${classifiers.size}")
                withIndent { classifiers.forEach { appendLine(renderSymbol(it, printPretty)) } }
                appendLine("callables: ${callables.size}")
                withIndent { callables.forEach { appendLine(renderSymbol(it, printPretty)) } }
            }
        }
    }


    private fun KtAnalysisSession.renderSymbol(
        symbol: KtDeclarationSymbol,
        printPretty: Boolean
    ): String = if (printPretty) symbol.render(prettyPrintSymbolRenderer) else debugRenderer.render(symbol)

    companion object {
        private val debugRenderer = DebugSymbolRenderer()
        private val prettyPrintSymbolRenderer = KtDeclarationRendererForSource.WITH_QUALIFIED_NAMES.with {
            annotationRenderer = annotationRenderer.with { annotationFilter = KtRendererAnnotationsFilter.NONE }
            modifiersRenderer = modifiersRenderer.with { modifierFilter = KtRendererModifierFilter.NONE }
        }
    }
}
