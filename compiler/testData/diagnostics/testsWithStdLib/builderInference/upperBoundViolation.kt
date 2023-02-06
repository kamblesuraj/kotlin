// !RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-55055
// FIR_DUMP
fun <T : Number> printGenericNumber(t: T) = println("Number is $t")

fun main() {
    buildList { // inferred into MutableList<String>
        <!UPPER_BOUND_VIOLATION_IN_CONSTRAINT!>add("Boom")<!>
        <!UPPER_BOUND_VIOLATION_IN_CONSTRAINT!>printGenericNumber(<!UPPER_BOUND_VIOLATION_IN_CONSTRAINT!>this[0]<!>)<!>
    }
}
