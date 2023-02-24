/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.util

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.JsCallChecker.Companion.isJsCall
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.source.getPsi

fun FunctionDescriptor.hasValidJsCodeBody(bindingContext: BindingContext): Boolean {
    val psi = source.getPsi() as? KtNamedFunction ?: return false
    return psi.hasValidJsCodeBody(bindingContext)
}

private fun KtDeclarationWithBody.hasValidJsCodeBody(bindingContext: BindingContext): Boolean {
    if (!hasBody()) return true
    val body = bodyExpression!!
    return when {
        !hasBlockBody() -> body.isJsCall(bindingContext)
        body is KtBlockExpression -> {
            val statement = body.statements.singleOrNull() ?: return false
            statement.isJsCall(bindingContext)
        }
        else -> false
    }
}

private fun KtExpression.isJsCall(bindingContext: BindingContext): Boolean {
    return getResolvedCall(bindingContext)?.isJsCall() ?: false
}