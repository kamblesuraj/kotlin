// TARGET_BACKEND: JVM_IR
// Field VS property: case 2.1
// See KT-54393 for details

// FILE: BaseJava.java
public class BaseJava {
    private String a = "FAIL";
}

// FILE: Derived.kt
class Derived : BaseJava() {
    val a = "OK"
}

fun box() = Derived().a
