// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR
// IGNORE_BACKEND_K1: JVM_IR, NATIVE, JS_IR, JS_IR_ES6
// IGNORE_FIR_DIAGNOSTICS
// !DIAGNOSTICS: -UNINITIALIZED_ENUM_ENTRY

enum class TestEnum(val testNaming: String) {
    OK(OK.name),
}

const val name = TestEnum.OK.name

fun box(): String {
    return name
}
