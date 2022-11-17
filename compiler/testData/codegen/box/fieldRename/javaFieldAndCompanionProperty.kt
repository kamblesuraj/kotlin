// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// See KT-52338

// FILE: Base.java
public class Base {
    protected String TAG = "OK";
}

// FILE: Sub.kt

class Sub : Base() {
    companion object {
        val TAG = "FAIL"
    }

    fun log() = TAG
}

fun box() = Sub().log()
