import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCAction

import platform.Foundation.*
import platform.XCTest.*
import platform.objc.*

import kotlin.native.internal.test.BaseClassSuite
import kotlin.native.internal.test.GeneratedSuites
import kotlin.native.internal.test.TopLevelSuite

@ExportObjCClass(name = "Kotlin/Native @Test")
class TestCaseRunner(
    invocation: NSInvocation,
    private val testName: String,
    private val beforeTest: Collection<() -> Unit>,
    private val method: () -> Unit = { },
    private val afterTest: Collection<() -> Unit>
) : XCTestCase(invocation) {
    // TODO: ignored TestCases should be skipped
    //  as for now these are defaultTest created by the testCaseWithInvocation
    private val shouldSkip: Boolean
        get() {
            return testName == "defaultTest"
        }

    override fun continueAfterFailure(): Boolean = false

    @ObjCAction
    fun run() {
        try {
            if (shouldSkip) {
                // TODO: XCTFail should be used instead, but https://youtrack.jetbrains.com/issue/KT-43719
                //  or maybe a wrapper as we need file name and line
                _XCTSkipHandler("FILE", 0, "Test is ignored")
            } else {
                method()
            }
        } catch (throwable: Throwable) {
            val issue = XCTIssue(
                type = XCTIssueTypeUncaughtException,
                compactDescription = "${throwable.message} in $testName",
                detailedDescription = "Caught exception ${throwable.message} in $testName",
                sourceCodeContext = XCTSourceCodeContext(
                    callStackAddresses = throwable.getStackTraceAddresses(),
                    location = XCTSourceCodeLocation() // TODO: provide with file path and line from stacktrace[1]
                ),
                associatedError = NSError.errorWithDomain("???", 10, null),
                attachments = emptyList<XCTAttachment>()
            )
            _XCTSkipFailureException
            testRun?.recordIssue(issue)
            // TODO: Should we also do something here?
            //
            //  XCTFail() is not available, see https://youtrack.jetbrains.com/issue/KT-43719
            // _XCTSkipHandler("FILE", 100, "Failure recorded")
            // throw throwable
        }
    }

    override fun setUp() {
        super.setUp()
        beforeTest.forEach { it() }
    }

    override fun tearDown() {
        afterTest.forEach { it() }
        super.tearDown()
    }

    override fun description() = "@Test fun ${name()}()"

    // TODO: file an issue for null in this::class.simpleName
    //  "${this::class.simpleName}::$testName" leads to "null::$testName"
    // TODO: should the name be "-[$className $testName]" or not?
    override fun name() = testName

    companion object : XCTestCaseMeta(), XCTestSuiteExtensionsProtocolMeta {
        //region: XCTestSuiteExtensionsProtocolMeta extensions
        /**
         * These are from the XCTestCase extension and are not available by default.
         * See `@interface XCTestCase (XCTestSuiteExtensions)` in `XCTestCase.h` header file.
         * Issue: https://youtrack.jetbrains.com/issue/KT-40426
         */

        override fun defaultTestSuite(): XCTestSuite? {
            return defaultTestSuite
        }

        // TODO: setUp() and tearDown() methods are required for tests with @Before/AfterClass annotations
        //  testSuites should be generated one-to-one with each suite run by the own TestCaseRunner
        override fun setUp() {
//            MyTest.beforeClass()
        }

        override fun tearDown() {
//            MyTest.afterClass()
            disposeRunMethods()
        }
        //endregion

        /**
         * Used if the test suite is generated as a default one from methods extracted by the XCTest from the
         * runner that extends XCTestCase and is exported to ObjC
         *
         * probably not needed
         */
        override fun testCaseWithInvocation(invocation: NSInvocation?): XCTestCase {
            println(
                "Got invocation: ${invocation?.description} " +
                        "@sel(${NSStringFromSelector(invocation?.selector)})"
            )
            // FIXME: creates default test, but should get one from testInvocations
            val inv = invocation ?: NSInvocation()
            return TestCaseRunner(
                invocation = inv,
                testName = "defaultTest",
                beforeTest = listOf({ println("Default before test was invoked") }),
                method = { println("Default test was invoked") },
                afterTest = listOf({ println("Default after test was invoked") }),
            )
        }

        // region: Dynamic run methods creation
        private fun createRunMethod(selector: SEL) {
            // Note: must be disposed off with imp_removeBlock
            val result = class_addMethod(
                cls = this.`class`(),
                name = selector,
                imp = imp_implementationWithBlock(this::runner),
                types = "v@:"
            )
            check(result) {
                "Was unable to add method with selector $selector"
            }
        }

        private fun dispose(selector: SEL) {
            val imp = class_getMethodImplementation(
                cls = this.`class`(),
                name = selector
            )
            imp_removeBlock(imp)
        }

        private fun disposeRunMethods() {
            createTestMethodsNames().forEach {
                val selector = NSSelectorFromString(it)
                dispose(selector)
            }
        }

        @Suppress("UNUSED_PARAMETER")
        private fun runner(runner: TestCaseRunner, _sel: SEL) {
            runner.run()
        }
        //endregion

        @OptIn(ExperimentalStdlibApi::class)
        private fun createTestMethodsNames(): List<String> = GeneratedSuites.suites
            .flatMap { testSuite ->
                testSuite.testCases.values
                    .filterNot { it.ignored }
                    .map { "$testSuite.${it.name}" }
            }

        /**
         * Create Test invocations for each test method to make them resolvable by the XCTest's
         * @see NSInvocation
         */
        override fun testInvocations(): List<NSInvocation> = createTestMethodsNames().map {
            val selector = NSSelectorFromString(it)
            createRunMethod(selector)
            this.instanceMethodSignatureForSelector(selector)?.let { signature ->
                val invocation = NSInvocation.invocationWithMethodSignature(signature)
                invocation.setSelector(selector)
                invocation
            } ?: error("Not able to create NSInvocation for method $it")
        }
    }
}

internal typealias SEL = COpaquePointer?

@OptIn(ExperimentalStdlibApi::class)
internal fun createTestSuites(): List<XCTestSuite> {
    val testInvocations = TestCaseRunner.testInvocations()
    return GeneratedSuites.suites
        .filterNot { it.ignored }
        .map {
            val suite = XCTestSuite.testSuiteWithName(it.name)
            when (it) {
                is TopLevelSuite -> {
                    it.testCases.values.map { testCase ->
                        testInvocations
                            .filter { nsInvocation ->
                                NSStringFromSelector(nsInvocation.selector) == "${suite.name}.${testCase.name}"
                            }
                            .map { inv ->
                                TestCaseRunner(
                                    invocation = inv,
                                    testName = "${suite.name}.${testCase.name}",
                                    beforeTest = it.before,
                                    method = testCase.testFunction,
                                    afterTest = it.after
                                )
                            }.single()
                    }.forEach { t ->
                        suite.addTest(t)
                    }
                }

                is BaseClassSuite<*, *> -> {
                    // Something wrong is going on here with type parameters
                    //  ¯\_(ツ)_/¯
                    @Suppress("UNCHECKED_CAST")
                    val testSuite = it as BaseClassSuite<Any, Any>
                    val testInstance = testSuite.createInstance()

                    it.testCases.values.map { testCase ->
                        testInvocations
                            .filter { inv ->
                                NSStringFromSelector(inv.selector) == "${suite.name}.${testCase.name}"
                            }
                            .map { inv ->
                                TestCaseRunner(
                                    invocation = inv,
                                    testName = "${suite.name}.${testCase.name}",
                                    beforeTest = testSuite.before.map { b -> { b.invoke(testInstance) } },
                                    method = { testCase.testFunction.invoke(testInstance) },
                                    afterTest = testSuite.after.map { a -> { a.invoke(testInstance) } }
                                )
                            }.single() // there is only one run method
                    }.forEach { t ->
                        suite.addTest(t)
                    }
                }

                else -> error("This is unknown type of test suite ${it.name} of ${it::class.simpleName}")
            }
            suite
        }
}
