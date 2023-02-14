// FULL_JDK

import java.util.function.Supplier

class Panel(supplier: Supplier<String>?)

class Wrapper(private val s: String?) {
    val panel: Panel

    init {
        panel = Panel(s?.let { <!ARGUMENT_TYPE_MISMATCH!>{ it }<!> })
    }
}
