/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.konan.blackboxtest.support.group.K2Pipeline;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.GenerateNativeTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("native/native.tests/testData/klibContents")
@TestDataPath("$PROJECT_ROOT")
@K2Pipeline()
public class NativeK2LibContentsTestGenerated extends AbstractNativeKlibContentsTest {
    @Test
    public void testAllFilesPresentInKlibContents() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("native/native.tests/testData/klibContents"), Pattern.compile("^([^_](.+)).kt$"), null, false);
    }

    @Test
    @TestMetadata("annotations.kt")
    public void testAnnotations() throws Exception {
        runTest("native/native.tests/testData/klibContents/annotations.kt");
    }

    @Test
    @TestMetadata("kt55464_serializeTypeAnnotation.kt")
    public void testKt55464_serializeTypeAnnotation() throws Exception {
        runTest("native/native.tests/testData/klibContents/kt55464_serializeTypeAnnotation.kt");
    }
}
