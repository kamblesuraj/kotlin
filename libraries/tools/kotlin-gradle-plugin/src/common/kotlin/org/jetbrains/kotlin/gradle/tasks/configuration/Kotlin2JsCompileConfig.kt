/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.Project
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.targets.js.internal.LibraryFilterCachingService
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.utils.klibModuleName
import java.io.File
import javax.inject.Inject

internal typealias Kotlin2JsCompileConfig = BaseKotlin2JsCompileConfig<Kotlin2JsCompile>

internal open class BaseKotlin2JsCompileConfig<TASK : Kotlin2JsCompile>(
    compilation: KotlinCompilationInfo
) : AbstractKotlinCompileConfig<TASK>(compilation) {

    init {
        val libraryFilterCachingService = LibraryFilterCachingService.registerIfAbsent(project)

        configureTask { task ->

            registerTransformsOnce(project)
            task.incremental = propertiesProvider.incrementalJs ?: true
            task.incrementalJsKlib = propertiesProvider.incrementalJsKlib ?: true

            configureAdditionalFreeCompilerArguments(task, compilation)

            task.compilerOptions.moduleName.convention(compilation.moduleName)

            @Suppress("DEPRECATION")
            task.outputFileProperty.value(
                task.destinationDirectory.flatMap { dir ->
                    if (task.compilerOptions.outputFile.orNull != null) {
                        task.compilerOptions.outputFile.map { File(it) }
                    } else {
                        task.compilerOptions.moduleName.map { name ->
                            dir.file(name + compilation.platformType.fileExtension).asFile
                        }
                    }
                }
            )

            if (propertiesProvider.useK2 == true) {
                task.kotlinOptions.useK2 = true
            }

            task.destinationDirectory
                .convention(
                    project.objects.directoryProperty().fileProvider(
                        task.defaultDestinationDirectory.map {
                            val freeArgs = task.enhancedFreeCompilerArgs.get()
                            if (task.compilerOptions.outputFile.orNull != null) {
                                if (freeArgs.contains(PRODUCE_UNZIPPED_KLIB)) {
                                    val file = File(task.compilerOptions.outputFile.get())
                                    if (file.extension == "") file else file.parentFile
                                } else {
                                    File(task.compilerOptions.outputFile.get()).parentFile
                                }
                            } else {
                                it.asFile
                            }
                        }
                    )
                )

            task.libraryFilterCacheService.value(libraryFilterCachingService).disallowChanges()
        }
    }

    protected open fun configureAdditionalFreeCompilerArguments(
        task: TASK,
        compilation: KotlinCompilationInfo
    ) {
        task.enhancedFreeCompilerArgs.value(
            task.compilerOptions.freeCompilerArgs.map { freeArgs ->
                freeArgs.toMutableList().apply {
                    commonJsAdditionalCompilerFlags(compilation)
                }
            }
        ).disallowChanges()
    }

    protected fun MutableList<String>.commonJsAdditionalCompilerFlags(
        compilation: KotlinCompilationInfo
    ) {
        if (contains(DISABLE_PRE_IR) &&
            !contains(PRODUCE_UNZIPPED_KLIB) &&
            !contains(PRODUCE_ZIPPED_KLIB)
        ) {
            add(PRODUCE_UNZIPPED_KLIB)
        }

        if (contains(PRODUCE_JS) ||
            contains(PRODUCE_UNZIPPED_KLIB) ||
            contains(PRODUCE_ZIPPED_KLIB)
        ) {
            // Configure FQ module name to avoid cyclic dependencies in klib manifests (see KT-36721).
            val baseName = if (compilation.isMain) {
                project.name
            } else {
                "${project.name}_${compilation.compilationName}"
            }
            if (none { it.startsWith(KLIB_MODULE_NAME) }) {
                add("$KLIB_MODULE_NAME=${project.klibModuleName(baseName)}")
            }
        }
    }

    companion object {
        fun registerTransformsOnce(project: Project) {
            if (project.extensions.extraProperties.has("hello")) {
                return
            }
            project.extensions.extraProperties["hello"] = true

            project.dependencies.registerTransform(Unzip::class.java) {
                it.from.attribute(BaseKotlinCompileConfig.ARTIFACT_TYPE_ATTRIBUTE, BaseKotlinCompileConfig.JAR_ARTIFACT_TYPE)
                it.to.attribute(
                    BaseKotlinCompileConfig.ARTIFACT_TYPE_ATTRIBUTE,
                    BaseKotlinCompileConfig.DIRECTORY_ARTIFACT_TYPE
                )
            }

            project.dependencies.registerTransform(Unzip::class.java) {
                it.from.attribute(BaseKotlinCompileConfig.ARTIFACT_TYPE_ATTRIBUTE, "klib")
                it.to.attribute(
                    BaseKotlinCompileConfig.ARTIFACT_TYPE_ATTRIBUTE,
                    BaseKotlinCompileConfig.DIRECTORY_ARTIFACT_TYPE
                )
            }
        }
    }
}

abstract class Unzip : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    override
    fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        val unzipDir = outputs.dir(input.name)
        unzipTo(input, unzipDir)
    }

    private fun unzipTo(zipFile: File, unzipDir: File) {
        fs.copy {
            it.from(archiveOperations.zipTree(zipFile))
            it.into(unzipDir)
        }
    }
}