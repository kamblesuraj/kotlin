import platform.Foundation.NSBundle
import platform.XCTest.*

fun main() {
    val defaultTestSuite = defaultTestSuiteRunner()
    println(":::: Run tests ::::")
    defaultTestSuite.runTest()
}

fun defaultTestSuiteRunner(): XCTestSuite {
    XCTestObservationCenter.sharedTestObservationCenter.addTestObserver(XCSimpleTestListener())
    val bundlePath = NSBundle.mainBundle.bundlePath
    println(":::: Bundle path = $bundlePath")
    val nativeTestSuite = XCTestSuite.testSuiteWithName("Kotlin/Native test suite")

    println(":::: Create test suites ::::")
    createTestSuites().forEach {
        println("[${it.name}] Tests: " + it.tests().joinToString(", ", "[", "]"))
        nativeTestSuite.addTest(it)
    }
    @Suppress("UNCHECKED_CAST")
    (nativeTestSuite.tests as List<XCTest>).forEach {
        println("${it.name} with ${it.testCaseCount} tests")
    }
    return nativeTestSuite
}

// TODO: report a bug with error reporting