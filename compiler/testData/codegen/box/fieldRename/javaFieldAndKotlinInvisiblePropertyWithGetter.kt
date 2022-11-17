// TARGET_BACKEND: JVM_IR
// Field VS property: case 1.2
// See KT-54393 for details

// FILE: BaseJava.java
public class BaseJava {
    public String a = "OK";
}

// FILE: Derived.kt
class Derived : BaseJava() {
    private val a get() = "FAIL"
}

fun box() = Derived().a
