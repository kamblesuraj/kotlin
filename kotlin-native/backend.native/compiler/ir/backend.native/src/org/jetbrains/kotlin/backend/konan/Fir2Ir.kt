package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.phases.Fir2IrOutput
import org.jetbrains.kotlin.backend.konan.driver.phases.FirOutput
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbolsOverFir
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentTypeTransformer
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrResult
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.storage.LockBasedStorageManager

internal val KlibFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer, PlatformDependentTypeTransformer.None)

internal fun PhaseContext.fir2Ir(
        input: FirOutput.Full,
): Fir2IrOutput {
    val fir2IrExtensions = Fir2IrExtensions.Default

    var builtInsModule: KotlinBuiltIns? = null
    val dependencies = mutableListOf<ModuleDescriptorImpl>()

    val resolvedLibraries = config.resolvedLibraries.getFullResolvedList()
    val configuration = config.configuration
    val librariesDescriptors = resolvedLibraries.map { resolvedLibrary ->
        val storageManager = LockBasedStorageManager("ModulesStructure")

        val moduleDescriptor = KlibFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                resolvedLibrary.library,
                configuration.languageVersionSettings,
                storageManager,
                builtInsModule,
                packageAccessHandler = null,
                lookupTracker = LookupTracker.DO_NOTHING
        )
        dependencies += moduleDescriptor

        val isBuiltIns = moduleDescriptor.isNativeStdlib()
        if (isBuiltIns) builtInsModule = moduleDescriptor.builtIns

        moduleDescriptor
    }

    librariesDescriptors.forEach { moduleDescriptor ->
        // Yes, just to all of them.
        moduleDescriptor.setDependencies(ArrayList(dependencies))
    }
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

    val fir2irResult = input.firResult.convertToIrAndActualize(
            fir2IrExtensions,
            IrGenerationExtension.getInstances(config.project),
            linkViaSignatures = false,
            signatureComposerCreator = null,
            irMangler = KonanManglerIr,
            visibilityConverter = Fir2IrVisibilityConverter.Default,
            diagnosticsReporter = diagnosticsReporter,
            languageVersionSettings = configuration.languageVersionSettings,
            kotlinBuiltIns = builtInsModule ?: DefaultBuiltIns.Instance,
    ).also {
        (it.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = librariesDescriptors }
    }

    val symbols = createKonanSymbols(fir2irResult)
    // TODO KT-55580 Invoke CopyDefaultValuesToActualPhase, same as PsiToir phase does.

    if (diagnosticsReporter.hasErrors) {
        val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)
        // TODO: stop the pipeline in case of errors
    }

    return Fir2IrOutput(input.firResult, fir2irResult, symbols)
}

private fun PhaseContext.createKonanSymbols(
        fir2irResult: Fir2IrResult,
): KonanSymbolsOverFir {
    val moduleDescriptor = fir2irResult.irModuleFragment.descriptor
    val symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), fir2irResult.pluginContext.irFactory)
    val descriptorsLookup = DescriptorsLookup(moduleDescriptor.builtIns as KonanBuiltIns)

    return KonanSymbolsOverFir(this, descriptorsLookup, fir2irResult.components.irBuiltIns, symbolTable, symbolTable.lazyWrapper)
}
