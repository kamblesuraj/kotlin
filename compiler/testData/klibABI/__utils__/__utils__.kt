@file:Suppress("PackageDirectoryMismatch", "unused")

package abitestutils

import abitestutils.TestMode.*
import abitestutils.ThrowableKind.*

/** API **/

interface TestBuilder {
    val testMode: TestMode

    fun skipHashes(message: String): ErrorMessagePattern
    fun nonImplementedCallable(callableTypeAndName: String, classifierTypeAndName: String): ErrorMessagePattern

    fun expectFailure(errorMessagePattern: ErrorMessagePattern, block: Block<Any?>)
    fun expectNoWhenBranchFailure(block: Block<Any?>)
    fun expectSuccess(block: Block<String>) // OK is expected
    fun <T : Any> expectSuccess(expectedOutcome: T, block: Block<T>)
}

sealed interface ErrorMessagePattern

private typealias Block<T> = () -> T

enum class TestMode {
    JS_NO_IC,
    JS_WITH_IC,
    NATIVE_CACHE_NO,
    NATIVE_CACHE_STATIC_ONLY_DIST,
    NATIVE_CACHE_STATIC_EVERYWHERE,
}

fun abiTest(init: TestBuilder.() -> Unit): String {
    val builder = TestBuilderImpl()
    builder.init()
    builder.check()
    return builder.runTests()
}

/** Implementation **/

private const val OK_STATUS = "OK"

private class TestBuilderImpl : TestBuilder {
    override val testMode = TestMode.__UNKNOWN__

    private val tests = mutableListOf<Test>()

    override fun skipHashes(message: String) = ErrorMessageWithSkippedSignatureHashes(message)

    override fun nonImplementedCallable(callableTypeAndName: String, classifierTypeAndName: String) =
        NonImplementedCallable(callableTypeAndName, classifierTypeAndName)

    override fun expectFailure(errorMessagePattern: ErrorMessagePattern, block: Block<Any?>) {
        tests += FailingWithLinkageErrorTest(errorMessagePattern, block)
    }

    override fun expectNoWhenBranchFailure(block: Block<Any?>) {
        tests += FailingWithNoWhenBranchErrorTest(block)
    }

    override fun expectSuccess(block: Block<String>) = expectSuccess(OK_STATUS, block)

    override fun <T : Any> expectSuccess(expectedOutcome: T, block: Block<T>) {
        tests += SuccessfulTest(expectedOutcome, block)
    }

    fun check() {
        check(tests.isNotEmpty()) { "No ABI tests configured" }
    }

    fun runTests(): String {
        val testFailures: List<TestFailure> = tests.mapIndexedNotNull { serialNumber, test ->
            val testFailureDetails: TestFailureDetails? = when (test) {
                is FailingWithLinkageErrorTest -> {
                    try {
                        test.block()
                        TestSuccessfulButMustFail
                    } catch (t: Throwable) {
                        when (t.throwableKind) {
                            IR_LINKAGE_ERROR -> test.checkIrLinkageErrorMessage(t)
                            NO_WHEN_BRANCH_ERROR, EXCEPTION -> TestFailedWithException(t)
                            NON_EXCEPTION -> throw t // Something totally unexpected. Rethrow.
                        }
                    }
                }

                is FailingWithNoWhenBranchErrorTest -> {
                    try {
                        test.block()
                        TestSuccessfulButMustFail
                    } catch (t: Throwable) {
                        when (t.throwableKind) {
                            NO_WHEN_BRANCH_ERROR -> null // Success.
                            IR_LINKAGE_ERROR, EXCEPTION -> TestFailedWithException(t)
                            NON_EXCEPTION -> throw t // Something totally unexpected. Rethrow.
                        }
                    }
                }

                is SuccessfulTest -> {
                    try {
                        val result = test.block()
                        if (result == test.expectedOutcome)
                            null // Success.
                        else
                            TestMismatchedExpectation(test.expectedOutcome, result)
                    } catch (t: Throwable) {
                        if (t.throwableKind == NON_EXCEPTION)
                            throw t // Something totally unexpected. Rethrow.
                        else
                            TestFailedWithException(t)
                    }
                }
            }

            if (testFailureDetails != null) TestFailure(serialNumber, test.sourceLocation, testFailureDetails) else null
        }

        return if (testFailures.isEmpty()) OK_STATUS else testFailures.joinToString(prefix = "\n", separator = "\n", postfix = "\n")
    }
}

private sealed interface AbstractErrorMessagePattern : ErrorMessagePattern {
    fun checkIrLinkageErrorMessage(errorMessage: String?): TestFailureDetails?
}

private class ErrorMessageWithSkippedSignatureHashes(private val expectedMessage: String) : AbstractErrorMessagePattern {
    init {
        check(expectedMessage.isNotBlank()) { "Message is blank: [$expectedMessage]" }
    }

    override fun checkIrLinkageErrorMessage(errorMessage: String?) =
        if (errorMessage?.replace(SIGNATURE_WITH_HASH) { it.groupValues[1] } == expectedMessage)
            null // Success.
        else
            TestMismatchedExpectation(expectedMessage, errorMessage)

    companion object {
        val SIGNATURE_WITH_HASH = Regex("(symbol /[\\da-zA-Z.<>_\\-]+)(\\|\\S+)")
    }
}

private class NonImplementedCallable(callableTypeAndName: String, classifierTypeAndName: String) : AbstractErrorMessagePattern {
    init {
        check(callableTypeAndName.isNotBlank() && ' ' in callableTypeAndName) { "Invalid callable type & name: [$callableTypeAndName]" }
        check(classifierTypeAndName.isNotBlank() && ' ' in classifierTypeAndName) { "Invalid classifier type & name: [$classifierTypeAndName]" }
    }

    private val fullMessage = "Abstract $callableTypeAndName is not implemented in non-abstract $classifierTypeAndName"

    override fun checkIrLinkageErrorMessage(errorMessage: String?) =
        if (errorMessage == fullMessage)
            null // Success.
        else
            TestMismatchedExpectation(fullMessage, errorMessage)
}

private sealed class Test {
    val sourceLocation: String? = computeSourceLocation()
}

private class FailingWithLinkageErrorTest(val errorMessagePattern: ErrorMessagePattern, val block: Block<Any?>) : Test()
private class FailingWithNoWhenBranchErrorTest(val block: Block<Any?>) : Test()
private class SuccessfulTest(val expectedOutcome: Any, val block: Block<Any>) : Test()

private class TestFailure(val serialNumber: Int, val sourceLocation: String?, val details: TestFailureDetails) {
    override fun toString() = buildString {
        append('#').append(serialNumber)
        if (sourceLocation != null) append(" (").append(sourceLocation).append(")")
        append(": ").append(details.description)
    }
}

private sealed class TestFailureDetails(val description: String)
private object TestSuccessfulButMustFail : TestFailureDetails("Test is successful but was expected to fail.")
private class TestFailedWithException(t: Throwable) : TestFailureDetails("Test unexpectedly failed with exception: $t")
private class TestMismatchedExpectation(expectedOutcome: Any, actualOutcome: Any?) :
    TestFailureDetails("EXPECTED: $expectedOutcome, ACTUAL: $actualOutcome")

private enum class ThrowableKind { NO_WHEN_BRANCH_ERROR, IR_LINKAGE_ERROR, EXCEPTION, NON_EXCEPTION }

private val Throwable.throwableKind: ThrowableKind
    get() = when {
        this is NoWhenBranchMatchedException -> NO_WHEN_BRANCH_ERROR
        this::class.simpleName == "IrLinkageError" -> IR_LINKAGE_ERROR
        this is Exception -> EXCEPTION
        else -> NON_EXCEPTION
    }

private fun FailingWithLinkageErrorTest.checkIrLinkageErrorMessage(t: Throwable) =
    (errorMessagePattern as AbstractErrorMessagePattern).checkIrLinkageErrorMessage(t.message)

fun computeSourceLocation(): String? {
    fun extractSourceLocation(stackTraceLine: String): String? {
        return stackTraceLine.substringAfterLast('(', missingDelimiterValue = "")
            .substringBefore(')', missingDelimiterValue = "")
            .takeIf { it.isNotEmpty() }
            ?.split(':', limit = 2)
            ?.takeIf { it.size == 2 && it[0].isNotEmpty() && it[1].isNotEmpty() }
            ?.let { "${it[0].substringAfterLast('/').substringAfterLast('\\')}:${it[1]}" }
    }

    var beenInTestBuilderImpl = false

    // Capture the stack trace to find out the line number where the test was exactly configured.
    return Throwable().stackTraceToString()
        .lineSequence()
        .dropWhile { stackTraceLine ->
            val isInTestBuilderImpl = TestBuilderImpl::class.simpleName!! in stackTraceLine
            if (isInTestBuilderImpl) {
                beenInTestBuilderImpl = true
                true
            } else {
                !beenInTestBuilderImpl
            }
        }
        .mapNotNull(::extractSourceLocation)
        .firstOrNull()
}
