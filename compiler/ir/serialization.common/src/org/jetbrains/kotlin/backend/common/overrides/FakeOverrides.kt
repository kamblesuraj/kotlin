/*
 * Copyright 2010-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.common.overrides

import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.GlobalDeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartiallyLinkedDeclarationOrigin
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildTypeParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.overrides.FakeOverrideBuilderStrategy
import org.jetbrains.kotlin.ir.overrides.IrOverridingUtil
import org.jetbrains.kotlin.ir.overrides.IrUnimplementedOverridesStrategy
import org.jetbrains.kotlin.ir.overrides.IrUnimplementedOverridesStrategy.Customization
import org.jetbrains.kotlin.ir.overrides.IrUnimplementedOverridesStrategy.ProcessAsFakeOverrides
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

class FakeOverrideGlobalDeclarationTable(
    mangler: KotlinMangler.IrMangler
) : GlobalDeclarationTable(mangler) {
    fun clear() = table.clear()
}

open class FakeOverrideDeclarationTable(
    mangler: KotlinMangler.IrMangler,
    globalTable: FakeOverrideGlobalDeclarationTable = FakeOverrideGlobalDeclarationTable(mangler),
    signatureSerializerFactory: (PublicIdSignatureComputer, DeclarationTable) -> IdSignatureSerializer
) : DeclarationTable(globalTable) {
    override val globalDeclarationTable: FakeOverrideGlobalDeclarationTable = globalTable
    override val signaturer: IdSignatureSerializer = signatureSerializerFactory(globalTable.publicIdSignatureComputer, this)

    fun clear() {
        this.table.clear()
        globalDeclarationTable.clear()
    }
}

interface FakeOverrideClassFilter {
    fun needToConstructFakeOverrides(clazz: IrClass): Boolean
}

interface FileLocalAwareLinker {
    fun tryReferencingSimpleFunctionByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrSimpleFunctionSymbol?
    fun tryReferencingPropertyByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrPropertySymbol?
}

object DefaultFakeOverrideClassFilter : FakeOverrideClassFilter {
    override fun needToConstructFakeOverrides(clazz: IrClass): Boolean = true
}

private object ImplementAsErrorThrowingStubs : IrUnimplementedOverridesStrategy {
    override fun <T : IrOverridableMember> computeCustomization(overridableMember: T, parent: IrClass) =
        if (overridableMember.modality == Modality.ABSTRACT
            && parent.modality != Modality.ABSTRACT
            && parent.modality != Modality.SEALED
        ) {
            Customization(
                origin = PartiallyLinkedDeclarationOrigin.UNIMPLEMENTED_ABSTRACT_CALLABLE_MEMBER,
                modality = parent.modality // Use modality of class for implemented callable member.
            )
        } else
            Customization.NO
}

class FakeOverrideBuilder(
    val linker: FileLocalAwareLinker,
    val symbolTable: SymbolTable,
    mangler: KotlinMangler.IrMangler,
    typeSystem: IrTypeSystemContext,
    friendModules: Map<String, Collection<String>>,
    private val partialLinkageEnabled: Boolean,
    val platformSpecificClassFilter: FakeOverrideClassFilter = DefaultFakeOverrideClassFilter,
    private val fakeOverrideDeclarationTable: DeclarationTable = FakeOverrideDeclarationTable(mangler) { builder, table ->
        IdSignatureSerializer(builder, table)
    }
) : FakeOverrideBuilderStrategy(
    friendModules = friendModules,
    unimplementedOverridesStrategy = if (partialLinkageEnabled) ImplementAsErrorThrowingStubs else ProcessAsFakeOverrides
) {
    private val haveFakeOverrides = mutableSetOf<IrClass>()

    private val irOverridingUtil = IrOverridingUtil(typeSystem, this)
    private val irBuiltIns = typeSystem.irBuiltIns

    // TODO: The declaration table is needed for the signaturer.
//    private val fakeOverrideDeclarationTable = FakeOverrideDeclarationTable(mangler, signatureSerializerFactory)

    val fakeOverrideCandidates = mutableMapOf<IrClass, CompatibilityMode>()
    fun enqueueClass(clazz: IrClass, signature: IdSignature, compatibilityMode: CompatibilityMode) {
        fakeOverrideDeclarationTable.assumeDeclarationSignature(clazz, signature)
        fakeOverrideCandidates[clazz] = compatibilityMode
    }

    private fun buildFakeOverrideChainsForClass(clazz: IrClass, compatibilityMode: CompatibilityMode): Boolean {
        if (haveFakeOverrides.contains(clazz)) return true

        val superTypes = clazz.superTypes

        val superClasses = superTypes.map {
            it.getClass() ?: error("Unexpected super type: $it")
        }

        superClasses.forEach { superClass ->
            val mode = fakeOverrideCandidates[superClass] ?: compatibilityMode
            if (buildFakeOverrideChainsForClass(superClass, mode))
                haveFakeOverrides.add(superClass)
        }

        if (!platformSpecificClassFilter.needToConstructFakeOverrides(clazz)) return false

        fakeOverrideDeclarationTable.run {
            inFile(clazz.fileOrNull) {
                irOverridingUtil.buildFakeOverridesForClass(clazz, compatibilityMode.oldSignatures)
            }
        }
        return true
    }

    override fun linkFunctionFakeOverride(function: IrFunctionWithLateBinding, compatibilityMode: Boolean) {
        val (signature, symbol) = computeFunctionFakeOverrideSymbol(function, compatibilityMode)

        symbolTable.declareSimpleFunction(signature, { symbol }) {
            assert(it === symbol)
            function.acquireSymbol(it)
        }
    }

    override fun linkPropertyFakeOverride(property: IrPropertyWithLateBinding, compatibilityMode: Boolean) {
        // To compute a signature for a property with type parameters,
        // we must have its accessor's correspondingProperty pointing to the property's symbol.
        // See IrMangleComputer.mangleTypeParameterReference() for details.
        // But to create and link that symbol we should already have the signature computed.
        // To break this loop we use temp symbol in correspondingProperty.

        val tempSymbol = IrPropertySymbolImpl().also {
            it.bind(property as IrProperty)
        }
        property.getter?.let { getter ->
            getter.correspondingPropertySymbol = tempSymbol
        }
        property.setter?.let { setter ->
            setter.correspondingPropertySymbol = tempSymbol
        }

        val (signature, symbol) = computePropertyFakeOverrideSymbol(property, compatibilityMode)
        symbolTable.declareProperty(signature, { symbol }) {
            assert(it === symbol)
            property.acquireSymbol(it)
        }

        property.getter?.let { getter ->
            getter.correspondingPropertySymbol = property.symbol
            linkFunctionFakeOverride(
                getter as? IrFunctionWithLateBinding ?: error("Unexpected fake override getter: $getter"),
                compatibilityMode
            )
        }
        property.setter?.let { setter ->
            setter.correspondingPropertySymbol = property.symbol
            linkFunctionFakeOverride(
                setter as? IrFunctionWithLateBinding ?: error("Unexpected fake override setter: $setter"),
                compatibilityMode
            )
        }
    }

    private fun composeSignature(declaration: IrDeclaration, compatibleMode: Boolean) =
        fakeOverrideDeclarationTable.signaturer.composeSignatureForDeclaration(declaration, compatibleMode)

    private fun computeFunctionFakeOverrideSymbol(
        function: IrFunctionWithLateBinding,
        compatibilityMode: Boolean
    ): Pair<IdSignature, IrSimpleFunctionSymbol> {
        require(function is IrSimpleFunction) { "Unexpected fake override function: $function" }
        val parent = function.parentAsClass

        val signature = composeSignature(function, compatibilityMode)
        val symbol = linker.tryReferencingSimpleFunctionByLocalSignature(parent, signature)
            ?: symbolTable.referenceSimpleFunction(signature)

        if (!partialLinkageEnabled
            || !symbol.isBound
            || symbol.owner.let { it.isSuspend == function.isSuspend || it.parent != parent }
        ) {
            return signature to symbol
        }

        // In old KLIB signatures we don't distinguish between suspend and non-suspend functions. So we need to manually patch
        // the signature of the fake override to avoid clash with the existing function with the different `isSuspend` flag state.
        // This signature is not supposed to be ever serialized (as fake overrides are not serialized in KLIBs).
        // In new KLIB signatures `isSuspend` flag will be taken into account as a part of the signature.
        val irFactory = function.factory

        val functionWithDisambiguatedSignature = irFactory.buildFun {
            updateFrom(function)
            name = function.name
            returnType = irBuiltIns.unitType // Does not matter.
        }.apply {
            this.parent = parent
            copyAnnotationsFrom(function)
            copyParameterDeclarationsFrom(function)

            typeParameters = typeParameters + buildTypeParameter(this) {
                name = Name.identifier("disambiguation type parameter")
                index = typeParameters.size
                superTypes += irBuiltIns.nothingType // This is something that can't be expressed in the source code.
            }
        }

        val disambiguatedSignature = composeSignature(functionWithDisambiguatedSignature, compatibilityMode)
        assert(disambiguatedSignature != signature) { "Failed to compute disambiguated signature for fake override $function" }
        val symbolWithDisambiguatedSignature = linker.tryReferencingSimpleFunctionByLocalSignature(parent, disambiguatedSignature)
            ?: symbolTable.referenceSimpleFunction(disambiguatedSignature)

        return disambiguatedSignature to symbolWithDisambiguatedSignature
    }

    private fun computePropertyFakeOverrideSymbol(
        property: IrPropertyWithLateBinding,
        compatibilityMode: Boolean
    ): Pair<IdSignature, IrPropertySymbol> {
        val parent = property.parentAsClass

        val signature = composeSignature(property, compatibilityMode)
        val symbol = linker.tryReferencingPropertyByLocalSignature(parent, signature)
            ?: symbolTable.referenceProperty(signature)

        return signature to symbol
    }

    fun provideFakeOverrides(klass: IrClass, compatibleMode: CompatibilityMode) {
        buildFakeOverrideChainsForClass(klass, compatibleMode)
        irOverridingUtil.clear()
        haveFakeOverrides.add(klass)
    }

    fun provideFakeOverrides() {
        val entries = fakeOverrideCandidates.entries
        while (entries.isNotEmpty()) {
            val candidate = entries.last()
            entries.remove(candidate)
            provideFakeOverrides(candidate.key, candidate.value)
        }
    }
}
