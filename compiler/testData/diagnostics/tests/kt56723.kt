// FIR_IDENTICAL

fun foo(f: () -> Unit) {
    f()
}

class Wrapper(var s: String)

fun bar(w: Wrapper?) {
    // K1: type is () -> Unit, K2: type is () -> Unit?
    val lambda = {
        w?.s = "X"
    }
    // K1: Ok, K2: ARGUMENT_TYPE_MISMATCH
    foo(lambda)
}
