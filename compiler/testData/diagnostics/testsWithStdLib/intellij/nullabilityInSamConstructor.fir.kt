// FULL_JDK

import java.util.function.Supplier

fun test() {
    val sam = Supplier<String> {
        <!ARGUMENT_TYPE_MISMATCH!>foo()<!>
    }
}

fun foo(): String? = null
