// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// IGNORE_BACKEND_K1: JVM_IR, NATIVE
// IGNORE_FIR_DIAGNOSTICS
// !DIAGNOSTICS: -UNINITIALIZED_ENUM_ENTRY

enum class TestEnum(val testNaming: String) {
    OK(OK.name),
}

const val name = TestEnum.OK.name

fun box(): String {
    return name
}
