/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtTypeScope
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

public abstract class KtScopeProvider : KtAnalysisSessionComponent() {
    public abstract fun getMemberScope(classSymbol: KtSymbolWithMembers): KtScope

    public abstract fun getDeclaredMemberScope(classSymbol: KtSymbolWithMembers): KtScope

    public abstract fun getDelegatedMemberScope(classSymbol: KtSymbolWithMembers): KtScope

    public abstract fun getStaticMemberScope(symbol: KtSymbolWithMembers): KtScope

    public abstract fun getEmptyScope(): KtScope

    public abstract fun getFileScope(fileSymbol: KtFileSymbol): KtScope

    public abstract fun getPackageScope(packageSymbol: KtPackageSymbol): KtScope

    public abstract fun getCompositeScope(subScopes: List<KtScope>): KtScope

    public abstract fun getTypeScope(type: KtType): KtTypeScope?

    public abstract fun getSyntheticJavaPropertiesScope(type: KtType): KtTypeScope?

    public abstract fun getScopeContextForPosition(
        originalFile: KtFile,
        positionInFakeFile: KtElement
    ): KtScopeContext
}

public interface KtScopeProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Creates [KtScope] containing members of [KtDeclaration].
     * Returned [KtScope] doesn't include synthetic Java properties. To get such properties use [getSyntheticJavaPropertiesScope].
     */
    public fun KtSymbolWithMembers.getMemberScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getMemberScope(this) }

    public fun KtSymbolWithMembers.getDeclaredMemberScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getDeclaredMemberScope(this) }

    public fun KtSymbolWithMembers.getDelegatedMemberScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getDelegatedMemberScope(this) }

    public fun KtSymbolWithMembers.getStaticMemberScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getStaticMemberScope(this) }

    public fun KtFileSymbol.getFileScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getFileScope(this) }

    public fun KtPackageSymbol.getPackageScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getPackageScope(this) }

    public fun List<KtScope>.asCompositeScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getCompositeScope(this) }

    /**
     * Return a [KtTypeScope] for a given [KtType].
     * The type scope will include all members which are declared and callable on a given type.
     *
     * Comparing to the [KtScope], in the [KtTypeScope] all use-site type parameters are substituted.
     *
     * Consider the following code
     * ```
     * fun foo(list: List<String>) {
     *      list // get KtTypeScope for it
     * }
     *```
     *
     * Inside the `LIST_KT_ELEMENT.getKtType().getTypeScope()` would contain the `get(i: Int): String` method with substituted type `T = String`
     *
     * @return type scope for the given type if given `KtType` is not error type, `null` otherwise.
     * Returned [KtTypeScope] doesn't include synthetic Java properties. To get such properties use [getSyntheticJavaPropertiesScope].
     *
     * @see KtTypeScope
     * @see KtTypeProviderMixIn.getKtType
     */
    public fun KtType.getTypeScope(): KtTypeScope? =
        withValidityAssertion { analysisSession.scopeProvider.getTypeScope(this) }

    /**
     * Returns a [KtTypeScope] with synthetic Java properties created for a given [KtType].
     */
    public fun KtType.getSyntheticJavaPropertiesScope(): KtTypeScope? =
        withValidityAssertion { analysisSession.scopeProvider.getSyntheticJavaPropertiesScope(this) }

    /**
     * Scopes in returned [KtScopeContext] don't include synthetic Java properties.
     * To get such properties use [getSyntheticJavaPropertiesScope].
     */
    public fun KtFile.getScopeContextForPosition(positionInFakeFile: KtElement): KtScopeContext =
        withValidityAssertion { analysisSession.scopeProvider.getScopeContextForPosition(this, positionInFakeFile) }

    public fun KtFile.getScopeContextForFile(): KtScopeContext =
        withValidityAssertion { analysisSession.scopeProvider.getScopeContextForPosition(this, this) }

    public fun KtScopeContext.getCompositeScope(filter: (KtScopeKind) -> Boolean = { true }): KtScope = withValidityAssertion {
        val subScopes = scopes.filter { (_, scopeType) -> filter(scopeType) }.map { it.first }
        subScopes.asCompositeScope()
    }
}

public class KtScopeContext(
    private val _scopes: List<Pair<KtScope, KtScopeKind>>,
    private val _implicitReceivers: List<KtImplicitReceiver>,
    override val token: KtLifetimeToken
) : KtLifetimeOwner {
    public val implicitReceivers: List<KtImplicitReceiver> get() = withValidityAssertion { _implicitReceivers }
    public val scopes: List<Pair<KtScope, KtScopeKind>> get() = withValidityAssertion { _scopes }
}

public class KtImplicitReceiver(
    override val token: KtLifetimeToken,
    private val _type: KtType,
    private val _ownerSymbol: KtSymbol
) : KtLifetimeOwner {
    public val ownerSymbol: KtSymbol get() = withValidityAssertion { _ownerSymbol }
    public val type: KtType get() = withValidityAssertion { _type }
}

public sealed class KtScopeKind {
    /**
     * @param indexInTower index in scope tower. The index of the closest local scope is 0.
     */
    public class LocalScope(public val indexInTower: Int) : KtScopeKind()

    /**
     * @param receiverIndex index of the implicit receiver, which corresponds to type scope. For example:
     * ```
     * fun f(a: A, b: B) {
     *     with(a) {       // type scope for A: receiverIndex = 1
     *         with(b) {   // type scope for B: receiverIndex = 0
     *             <caret>
     *         }
     *     }
     * }
     * ```
     */
    public sealed class TypeScope : KtScopeKind() {
        public abstract val receiverIndex: Int
    }

    public class SimpleTypeScope(override val receiverIndex: Int) : TypeScope()
    public class SyntheticJavaPropertiesScope(override val receiverIndex: Int) : TypeScope()

    /**
     * @param indexInTower index in scope tower. The order in the scope tower depends on priorities of scopes.
     */
    public sealed class NonLocalScope : KtScopeKind() {
        public abstract val indexInTower: Int
    }

    public class TypeParameterScope(override val indexInTower: Int) : NonLocalScope()
    public class PackageMemberScope(override val indexInTower: Int) : NonLocalScope()

    public sealed class ImportingScope : NonLocalScope()
    public class ExplicitSimpleImportingScope(override val indexInTower: Int) : ImportingScope()
    public class ExplicitStarImportingScope(override val indexInTower: Int) : ImportingScope()
    public class DefaultSimpleImportingScope(override val indexInTower: Int) : ImportingScope()
    public class DefaultStarImportingScope(override val indexInTower: Int) : ImportingScope()

    /**
     * Non-local names-aware scope that can't be attributed to any of the listed above kinds.
     */
    public class NamesAwareScope(override val indexInTower: Int) : NonLocalScope()

    public companion object {
        public val UNKNOWN_INDEX: Int = Int.MAX_VALUE
    }
}