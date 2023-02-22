/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

fun IrWorldBuilder.playground() {
    irModuleFragment {
        irFile("/47424.kt") {
            packageName("com.example")
            irClass("Aa") {
                debugInfo(39, 51)
                modalityAbstract()
                visibilityInternal()
            }
            irClass("Ab") {
                debugInfo(53, 81)
                modalityAbstract()
            }
            irClass("Ba") {
                debugInfo(84, 96)
                modalityAbstract()
            }
            irClass("Bb") {
                debugInfo(98, 133)
                modalityAbstract()
            }
            irClass("Ca") {
                debugInfo(136, 166)
                modalityAbstract()
            }
            irClass("Cb") {
                debugInfo(168, 201)
                modalityAbstract()
            }
            irClass("C") {
                debugInfo(203, 223)
                modalityAbstract()
            }
        }
    }
}
