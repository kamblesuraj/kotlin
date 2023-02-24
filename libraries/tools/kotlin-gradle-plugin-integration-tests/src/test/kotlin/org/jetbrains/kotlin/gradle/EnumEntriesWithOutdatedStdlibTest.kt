/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("EnumEntries feature test when no kotlin.enums.EnumEntries found on the classpath")
@JvmGradlePluginTests
class EnumEntriesWithOutdatedStdlibTest : KGPBaseTest() {
    @DisplayName("EnumEntries should not be accessible without kotlin.enums.EnumEntries")
    @GradleTest
    fun enumEntriesNotAccessible(gradleVersion: GradleVersion) {
        project("enumEntriesNotAccessible", gradleVersion) {
            buildAndFail(":k1:compileKotlin") {
                assertOutputContains("MainK1.kt:9:30 Unresolved reference: entries")
            }

            buildAndFail(":k2:compileKotlin") {
                assertOutputContains("MainK2.kt:9:31 Unresolved reference: entries")
            }
        }
    }
}
