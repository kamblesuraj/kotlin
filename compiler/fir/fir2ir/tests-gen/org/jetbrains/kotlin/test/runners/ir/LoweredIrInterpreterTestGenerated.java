/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.test.generators.GenerateCompilerTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/ir/loweredIr")
@TestDataPath("$PROJECT_ROOT")
public class LoweredIrInterpreterTestGenerated extends AbstractLoweredIrInterpreterTest {
    @Test
    public void testAllFilesPresentInLoweredIr() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/ir/loweredIr"), Pattern.compile("^(.+)\\.kt$"), null, TargetBackend.JVM_IR, true);
    }

    @Nested
    @TestMetadata("compiler/testData/ir/loweredIr/interpreter")
    @TestDataPath("$PROJECT_ROOT")
    public class Interpreter {
        @Test
        public void testAllFilesPresentInInterpreter() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/ir/loweredIr/interpreter"), Pattern.compile("^(.+)\\.kt$"), null, TargetBackend.JVM_IR, true);
        }

        @Test
        @TestMetadata("booleanOperations.kt")
        public void testBooleanOperations() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/booleanOperations.kt");
        }

        @Test
        @TestMetadata("byteOperations.kt")
        public void testByteOperations() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/byteOperations.kt");
        }

        @Test
        @TestMetadata("charOperations.kt")
        public void testCharOperations() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/charOperations.kt");
        }

        @Test
        @TestMetadata("constTrimIndent.kt")
        public void testConstTrimIndent() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/constTrimIndent.kt");
        }

        @Test
        @TestMetadata("constTrimMargin.kt")
        public void testConstTrimMargin() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/constTrimMargin.kt");
        }

        @Test
        @TestMetadata("doubleOperations.kt")
        public void testDoubleOperations() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/doubleOperations.kt");
        }

        @Test
        @TestMetadata("enumName.kt")
        public void testEnumName() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/enumName.kt");
        }

        @Test
        @TestMetadata("enumRecursiveName.kt")
        public void testEnumRecursiveName() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/enumRecursiveName.kt");
        }

        @Test
        @TestMetadata("floatOperations.kt")
        public void testFloatOperations() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/floatOperations.kt");
        }

        @Test
        @TestMetadata("ifConstVal.kt")
        public void testIfConstVal() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/ifConstVal.kt");
        }

        @Test
        @TestMetadata("intOperations.kt")
        public void testIntOperations() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/intOperations.kt");
        }

        @Test
        @TestMetadata("kCallableName.kt")
        public void testKCallableName() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/kCallableName.kt");
        }

        @Test
        @TestMetadata("kt53272.kt")
        public void testKt53272() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/kt53272.kt");
        }

        @Test
        @TestMetadata("longOperations.kt")
        public void testLongOperations() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/longOperations.kt");
        }

        @Test
        @TestMetadata("shortOperations.kt")
        public void testShortOperations() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/shortOperations.kt");
        }

        @Test
        @TestMetadata("stdlibConst.kt")
        public void testStdlibConst() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/stdlibConst.kt");
        }

        @Test
        @TestMetadata("stringOperations.kt")
        public void testStringOperations() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/stringOperations.kt");
        }

        @Test
        @TestMetadata("unsignedConst.kt")
        public void testUnsignedConst() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/unsignedConst.kt");
        }

        @Test
        @TestMetadata("useCorrectToString.kt")
        public void testUseCorrectToString() throws Exception {
            runTest("compiler/testData/ir/loweredIr/interpreter/useCorrectToString.kt");
        }
    }
}
