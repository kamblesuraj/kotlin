/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.DisplayName

@DisplayName("EnumEntries feature test when no kotlin.enums.EnumEntries found on the classpath")
@JvmGradlePluginTests
class EnumEntriesWithOutdatedStdlibTest : KGPBaseTest() {
    @DisplayName("EnumEntries should not be accessible without kotlin.enums.EnumEntries")
    @GradleTest
    fun enumEntriesNotAccessible(gradleVersion: GradleVersion) {
        project("enumEntriesNotAccessible", gradleVersion) {
            buildAndFail(":compileKotlin") {
                assertOutputContains("Main.kt:13:20 Unresolved reference: entries")
                assertOutputContains("Main.kt:14:30 Unresolved reference: entries")
            }

            buildGradleKts.replaceText(
                "kotlinOptions.languageVersion = \"1.9\"",
                "kotlinOptions.languageVersion = \"2.0\"",
            )

            buildAndFail(":compileKotlin") {
                assertOutputContains("Main.kt:13:20 Unresolved reference: entries")
                assertOutputContains("Main.kt:14:30 Unresolved reference: entries")
            }
        }
    }

    @DisplayName("Code should compile normally if `entries` is never referenced")
    @GradleTest
    fun codeCompilesWithoutReferencingEntries(gradleVersion: GradleVersion) {
        project("enumEntriesNotAccessible", gradleVersion) {
            projectPath.resolve("src/main/kotlin/Main.kt")
                .replaceText("entries", "values()")

            build(":compileKotlin")

            buildGradleKts.replaceText(
                "kotlinOptions.languageVersion = \"1.9\"",
                "kotlinOptions.languageVersion = \"2.0\"",
            )

            build(":compileKotlin")
        }
    }
}
