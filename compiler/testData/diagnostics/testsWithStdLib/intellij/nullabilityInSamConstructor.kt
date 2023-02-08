// FULL_JDK

import java.util.function.Supplier

fun test() {
    val sam = Supplier<String> {
        foo()
    }
}

fun foo(): String? = null
