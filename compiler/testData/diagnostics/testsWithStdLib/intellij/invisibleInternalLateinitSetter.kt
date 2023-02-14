// MODULE: m1
// FILE: base.kt

open class Base {
    lateinit var foo: String
        internal set
}

// MODULE: m2(m1)
// FILE: derived.kt

class Derived(foo: String) : Base() {
    init {
        this.foo = foo
    }
}
