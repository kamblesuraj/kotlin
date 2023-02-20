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

class Wrapper2(val w: Wrapper?)

fun baz(w2: Wrapper2?) {
    val lambda = {
        w2?.w?.s = "X"
    }
    foo(lambda)
}

object Indexible {
    operator fun get(index: Int) = "$index"
    operator fun set(index: Int, value: String) {}
}
class IndexibleRef(val ind: Indexible)
class IndexibleRefRef(val ref: IndexibleRef?)

fun ban(refRef: IndexibleRefRef?, ref: IndexibleRef?) {
    val lambda = {
        ref?.ind[1] = "X"
    }
    foo(lambda)

    val lambda2 = {
        refRef?.ref?.ind[1] = "X"
    }
    foo(lambda2)
}

