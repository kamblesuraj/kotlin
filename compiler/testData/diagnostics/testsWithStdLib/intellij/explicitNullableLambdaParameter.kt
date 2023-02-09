// FILE: SomeClass.java

import java.util.*;

public class SomeClass {
    public static List<String> foo() {
        return null;
    }

    public static Map<String, String> fooFoo() {
        return null;
    }
}

// FILE: test.kt

fun bar() {
    SomeClass.foo().forEach { s: String? ->
        baz(<!TYPE_MISMATCH!>s<!>)
    }
    SomeClass.fooFoo().forEach { (k: String?, v: String?) ->
        baz(k)
        baz(v)
    }
}

fun baz(s: String) {}
