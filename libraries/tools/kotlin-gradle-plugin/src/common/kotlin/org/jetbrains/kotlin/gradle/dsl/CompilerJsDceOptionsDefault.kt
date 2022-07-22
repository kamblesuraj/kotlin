// DO NOT EDIT MANUALLY!
// Generated by org/jetbrains/kotlin/generators/arguments/GenerateGradleOptions.kt
// To regenerate run 'generateGradleOptions' task
@file:Suppress("RemoveRedundantQualifierName", "Deprecation", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.dsl

internal class CompilerJsDceOptionsDefault @javax.inject.Inject constructor(
    objectFactory: org.gradle.api.model.ObjectFactory
) : org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptionsDefault(objectFactory), org.jetbrains.kotlin.gradle.dsl.CompilerJsDceOptions {

    override val devMode: org.gradle.api.provider.Property<kotlin.Boolean> =
        objectFactory.property(kotlin.Boolean::class.java).convention(false)

    @Deprecated(message = "Use task 'destinationDirectory' to configure output directory", level = DeprecationLevel.WARNING)
    override val outputDirectory: org.gradle.api.provider.Property<kotlin.String> =
        objectFactory.property(kotlin.String::class.java)

    internal fun toCompilerArguments(args: org.jetbrains.kotlin.cli.common.arguments.K2JSDceArguments) {
        super.toCompilerArguments(args)
        args.devMode = devMode.get()
        args.outputDirectory = outputDirectory.orNull
    }

    internal fun fillDefaultValues(args: org.jetbrains.kotlin.cli.common.arguments.K2JSDceArguments) {
        super.fillDefaultValues(args)
        args.devMode = false
        args.outputDirectory = null
    }
}
