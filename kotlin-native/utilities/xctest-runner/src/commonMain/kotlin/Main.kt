import platform.XCTest.*

fun main() {
    val defaultTestSuite = defaultTestSuiteRunner()
    println(":::: Run tests ::::")
    defaultTestSuite.runTest()
}

// TODO: report a bug with error reporting