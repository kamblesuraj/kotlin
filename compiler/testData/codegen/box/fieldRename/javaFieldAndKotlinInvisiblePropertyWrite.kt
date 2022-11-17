// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// Field VS property: case 1.1 with writing
// See KT-54393 for details

// FILE: BaseJava.java
public class BaseJava {
    public String a = "FAIL2";
}

// FILE: Derived.kt
class Derived : BaseJava() {
    private var a = "FAIL"
}

fun box(): String {
    val d = Derived()
    d.a = "OK"
    return d.a
}
