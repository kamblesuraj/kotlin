// FIR_IDENTICAL
// FILE: I.kt

open class I : <!CYCLIC_INHERITANCE_HIERARCHY!>K<!>() {
    fun foo() {}
}

// FILE: J.java

class J extends I {
    void bar() {}
}

// FILE: K.kt

open class K : <!EXPOSED_SUPER_CLASS!>J<!>() {
    fun baz() {}
}
