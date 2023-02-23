fun foo(f: () -> Unit): String {
    f()
    return "OK"
}

class Wrapper(var s: String)

fun box(): String {
    val w: Wrapper? = Wrapper("Test")

    val lambda = {
        w?.s = "X"
    }

    return foo(lambda)
}
