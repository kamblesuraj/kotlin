// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: deprecated.kt

package foo

@JsExport
@Deprecated("deprecated")
fun foo() {}

@JsExport
@Deprecated("deprecated")
val bar: String = "Test"

@JsExport
@Deprecated("deprecated")
class TestClass

@JsExport
class AnotherClass @Deprecated("deprecated") constructor(val value: String) {
    @JsName("fromNothing")
    @Deprecated("deprecated") constructor(): this("Test")

    @JsName("fromInt")
    constructor(value: Int): this(value.toString())

    @Deprecated("deprecated")
    fun foo() {}

    fun baz() {}

    @Deprecated("deprecated")
    val bar: String = "Test"
}

@JsExport
interface TestInterface {
    @Deprecated("deprecated")
    fun foo()
    fun bar()
    @Deprecated("deprecated")
    val baz: String
}

@JsExport
object TestObject {
    @Deprecated("deprecated")
    fun foo() {}
    fun bar() {}
    @Deprecated("deprecated")
    val baz: String = "Test"
}
