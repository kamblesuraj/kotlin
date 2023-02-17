package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.work.NormalizeLineEndings
import java.io.File

interface LinkSyncTask : Task {
    @get:InputFiles
    @get:NormalizeLineEndings
    @get:IgnoreEmptyDirectories
    @get:SkipWhenEmpty
    val from: ConfigurableFileCollection

    @get:OutputDirectory
    val destinationDir: Property<File>
}