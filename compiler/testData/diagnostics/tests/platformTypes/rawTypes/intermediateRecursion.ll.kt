// LL_FIR_DIVERGENCE
// Workaround for KT-56630
// LL_FIR_DIVERGENCE
// FILE: Boo.java
public class Boo<N> {}

// FILE: Foo.java
public class Foo<T extends Boo<K>, K extends Boo<X>, X extends Boo<K>> {
    T test2() { return null; }
    static Foo test1() { return null; }
}

// FILE: main.kt
fun main() {
    val x = <!DEBUG_INFO_EXPRESSION_TYPE("Foo<*, *, *>..Foo<*, *, *>?!")!>Foo.test1()<!>.test2()
}
