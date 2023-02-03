// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// IGNORE_BACKEND_K1: JVM_IR, NATIVE

// FILE: 1.kt

const val name = E.OK.name
fun box(): String = name

// FILE: 2.kt

enum class E(val parent: E?) {
    X(null),
    OK(X),
}