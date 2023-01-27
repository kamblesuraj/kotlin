/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.junit.Test
import org.yaml.snakeyaml.Yaml
import java.io.File


data class Dep(val key: String, val deps: Set<String>) {
    override fun toString(): String {
        return deps.joinToString(prefix = "\n$key\n", separator = "\n") { "    - $it" }
    }
}

fun Any?.asList(): List<*> = (this as? List<*>) ?: reportUnexpectedFormat() ?: emptyList<Nothing>()

fun reportUnexpectedFormat(): Nothing? {
    println("Unexpected Podfile.lock format")
    return null
}

fun Any?.toDep(): Dep? {
    return when (this) {
        is String -> Dep(key = toPodName(), deps = emptySet())
        is Map<*, *> -> keys.singleOrNull()
            ?.let { it as? String }
            ?.let { key ->
                val list = get(key).asList()

                val deps = list.asSequence()
                    .mapNotNull { it as? String ?: reportUnexpectedFormat() }
                    .map { it.toPodName() }
                    .toSet()

                Dep(key = key.toPodName(), deps = deps)
            } ?: reportUnexpectedFormat()
        else -> reportUnexpectedFormat()
    }
}

private fun String.toPodName(): String = substringBefore(' ')


class BlahBlah {
    @Test
    fun blah() {
        val lock = Yaml().load<Map<String, Any>>(File("/Users/Artem.Daugel-Dauge/Downloads/xcode-repro/ios-app/Podfile.lock").inputStream())
            .get("PODS")
            .asList()
            .mapNotNull { it.toDep() }
            .associateBy(Dep::key, Dep::deps)

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