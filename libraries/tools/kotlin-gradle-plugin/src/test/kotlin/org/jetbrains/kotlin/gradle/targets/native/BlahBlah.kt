/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.targets.native.PodfileLockParser.LockedPod
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.junit.Test
import org.yaml.snakeyaml.Yaml
import java.io.File


class PodfileLockParser(private val logger: Logger?) {

    data class LockedPod(val key: String, val dependencies: Set<String>)

    private var shouldWarn = false

    fun parse(file: File): List<LockedPod> {
        shouldWarn = false

        return Yaml().load<List<LockedPod>?>(file.inputStream())
            .asMap()["PODS"]
            .asList()
            .mapNotNull { it.toPod() }
            .also {
                if (shouldWarn) {
                    logger?.warn("Unexpected Podfile.lock format")
                }
            }
    }

    private fun reportUnexpectedFormat(): Nothing? {
        shouldWarn = true
        return null
    }

    private fun Any?.asList(): List<*> = safeAs<List<*>>() ?: reportUnexpectedFormat() ?: emptyList<Nothing>()

    private fun Any?.asMap(): Map<*, *> = safeAs<Map<*, *>>() ?: reportUnexpectedFormat() ?: emptyMap<Nothing, Nothing>()

    private fun Any?.toPod(): LockedPod? {
        return when (this) {
            is String -> LockedPod(key = toPodName(), dependencies = emptySet())
            is Map<*, *> -> keys.singleOrNull()
                ?.let { it as? String }
                ?.let { key ->
                    val list = get(key).asList()

                    val deps = list.asSequence()
                        .mapNotNull { it as? String ?: reportUnexpectedFormat() }
                        .map { it.toPodName() }
                        .toSet()

                    LockedPod(key = key.toPodName(), dependencies = deps)
                } ?: reportUnexpectedFormat()
            else -> reportUnexpectedFormat()
        }
    }

    private fun String.toPodName(): String = substringBefore(' ')

}


class BlahBlah {
    @Test
    fun blah() {
        val lock = PodfileLockParser(null).parse(File("/Users/Artem.Daugel-Dauge/Downloads/xcode-repro/ios-app/Podfile.lock"))
            .associateBy(LockedPod::key, LockedPod::dependencies)

        val pods = setOf("AFNetworking", "FirebaseCore", "pod1", "pod2", "SSZipArchive")

        val fullDeps = mutableMapOf<String, Set<String>>()

        fun String.getFullDeps(): Set<String> {
            return fullDeps.getOrPut(this) {
                (lock[this] ?: error("invalid")).asSequence().flatMap { sequenceOf(it) + it.getFullDeps() }.toSet()
            }
        }

        pods.forEach { pod ->
            val depsForPod = pod.getFullDeps().intersect(pods)

            println("$pod:")

            depsForPod.forEach {
                println(" - $it")
            }

            println()

        }
    }

}