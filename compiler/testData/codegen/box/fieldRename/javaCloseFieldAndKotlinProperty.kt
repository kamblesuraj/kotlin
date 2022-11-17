// TARGET_BACKEND: JVM_IR
// Field VS property: case 3.1
// See KT-54393 for details

// FILE: VeryBase.kt
open class VeryBase {
    val some = "FAIL"
}

// FILE: Base.java
public class Base extends VeryBase {
    public String some = "OK";
}

// FILE: Test.kt
class Derived : Base()

fun box() = Derived().some
