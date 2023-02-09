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
        baz(<!ARGUMENT_TYPE_MISMATCH!>s<!>)
    }
    SomeClass.fooFoo().forEach { (k: String?, v: String?) ->
        baz(<!ARGUMENT_TYPE_MISMATCH!>k<!>)
        baz(<!ARGUMENT_TYPE_MISMATCH!>v<!>)
    }
}

fun baz(s: String) {}
