// FIR_DUMP

fun foo(f: () -> Unit) {
    f()
}

class Wrapper(var s: String)

fun bar(w: Wrapper?) {
    val lambda = {
        w?.s = "X"
    }
    foo(lambda)
}
