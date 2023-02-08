// TARGET_BACKEND: JVM_IR

// MODULE: m1

@RequiresOptIn
annotation class SomeOptIn

interface Some {
    @SomeOptIn
    val foo: String
}

// MODULE: m2(m1)

@OptIn(SomeOptIn::class)
class Impl : Some {
    override val foo: String
        get() = "OK"
}

fun box(): String {
    return test(Impl())
}

fun test(base: Some): String {
    return base.foo
}
