/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*

/**
 * Describes a reason why an [IrDeclaration] or an [IrExpression] is partially linked. Subclasses represent various causes of the p.l.
 */
internal sealed interface PartialLinkageCase {
    /**
     * There is no real owner declaration for the symbol, only synthetic stub created by [MissingDeclarationStubGenerator].
     * Likely the declaration has been deleted in newer version of the library.
     *
     * Applicable to: Declarations.
     */
    class MissingDeclaration(val missingDeclarationSymbol: IrSymbol) : PartialLinkageCase

    /**
     * The annotation class has unacceptable classifier as one of its parameters. This may happen if the class representing this
     * parameter was an annotation class before, but then it was converted to a non-annotation class.
     *
     * Applicable to: Declarations (annotation classes).
     */
    class AnnotationWithUnacceptableParameter(
        val annotationClassSymbol: IrClassSymbol,
        val unacceptableClassifierSymbol: IrClassifierSymbol
    ) : PartialLinkageCase

    /**
     * Declaration's signature uses partially linked classifier symbol.
     *
     * Applicable to: Declarations.
     */
    class DeclarationUsesPartiallyLinkedClassifier(
        val declarationSymbol: IrSymbol,
        val cause: ExploredClassifier.Unusable
    ) : PartialLinkageCase

    /**
     * Unimplemented abstract callable member in non-abstract class.
     *
     * Applicable to: Declarations (functions, properties).
     */
    class UnimplementedAbstractCallable(val callable: IrOverridableDeclaration<*>) : PartialLinkageCase

    /**
     * Expression references a missing IR declaration (IR declaration)
     * Example: An [IrCall] references unlinked [IrSimpleFunctionSymbol].
     *
     * Applicable to: Expressions.
     */
    class ExpressionUsesMissingDeclaration(
        val expression: IrExpression,
        val missingDeclarationSymbol: IrSymbol
    ) : PartialLinkageCase

    /**
     * Expression uses partially linked classifier symbol.
     * Example: An [IrTypeOperatorCall] that casts an argument to a type with unlinked symbol.
     *
     * Applicable to: Expressions.
     */
    class ExpressionUsesPartiallyLinkedClassifier(
        val expression: IrExpression,
        val cause: ExploredClassifier.Unusable
    ) : PartialLinkageCase

    /**
     * Expression refers an IR declaration with a signature that uses partially linked classifier symbol
     *
     * Applicable to: Expressions.
     */
    class ExpressionUsesDeclarationThatUsesPartiallyLinkedClassifier(
        val expression: IrExpression,
        val referencedDeclarationSymbol: IrSymbol,
        val cause: ExploredClassifier.Unusable
    ) : PartialLinkageCase

    /**
     * Expression refers an IR declaration with the wrong type.
     * Example: An [IrEnumConstructorCall] that refers an [IrConstructor] of a regular class.
     *
     * Applicable to: Expressions.
     */
    class ExpressionUsesWrongTypeOfDeclaration(
        val expression: IrExpression,
        val actualDeclarationSymbol: IrSymbol,
        val expectedDeclarationDescription: String
    ) : PartialLinkageCase

    /**
     * Expression that refers to an IR function has an excessive or a missing dispatch receiver parameter.
     *
     * Applicable to: Expressions.
     */
    class ExpressionDispatchReceiverMismatch(
        val expression: IrMemberAccessExpression<IrFunctionSymbol>,
        val excessiveDispatchReceiver: Boolean
    ) : PartialLinkageCase {
        // val missingDispatchReceiver get() = !excessiveDispatchReceiver
    }

    /**
     * Expression refers an IR declaration that is not accessible at the use site.
     * Example: An [IrCall] that refers a private [IrSimpleFunction] from another module.
     *
     * Applicable to: Expressions.
     */
    class ExpressionsUsesInaccessibleDeclaration(
        val expression: IrExpression,
        val referencedDeclarationSymbol: IrSymbol,
        val declaringModule: PartialLinkageUtils.Module,
        val useSiteModule: PartialLinkageUtils.Module
    ) : PartialLinkageCase
}
