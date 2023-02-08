// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR
// IGNORE_BACKEND_K1: JVM_IR, NATIVE, JS_IR, JS_IR_ES6
// WITH_STDLIB

const val trimIndent = "123".trimIndent()
const val complexTrimIndent =
    """
            ABC
            123
            456
        """.trimIndent()

fun box(): String {
    if (trimIndent != "123") return "Fail 1"
    if (complexTrimIndent != "ABC\n123\n456") return "Fail 2"
    return "OK"
}