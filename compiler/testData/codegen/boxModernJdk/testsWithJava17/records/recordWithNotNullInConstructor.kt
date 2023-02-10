// TARGET_BACKEND: JVM_IR

// MODULE: m1
// FILE: SomeWrapper.java

import org.jetbrains.annotations.NotNull;

public final class SomeWrapper {
    public record SomeRecord(@NotNull String a, @NotNull String b) {}
}

// MODULE: m2(m1)
// FILE: test.kt

import SomeWrapper.SomeRecord

fun test(a: Any?) {
    if (a !is String) return
    SomeRecord(a, "b")
}

fun box(): String {
    test("OK")
    return "OK"
}
