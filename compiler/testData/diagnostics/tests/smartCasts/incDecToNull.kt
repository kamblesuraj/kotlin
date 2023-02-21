// FIR_IDENTICAL
class IncDec {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc(): Unit {}
}

fun foo(): IncDec {
    var x = IncDec()
    x = x<!ASSIGNMENT_TYPE_MISMATCH!>++<!>
    x<!ASSIGNMENT_TYPE_MISMATCH!>++<!>
    return x
}
