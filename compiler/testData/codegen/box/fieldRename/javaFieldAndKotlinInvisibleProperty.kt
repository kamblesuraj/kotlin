// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

// FILE: BaseJava.java
public class BaseJava {
    public String a = "OK";
}

// FILE: Derived.kt
class Derived : BaseJava() {
    private val a = "FAIL"
}

fun box() = Derived().a
