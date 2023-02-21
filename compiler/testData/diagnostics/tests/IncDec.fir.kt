class IncDec() {
  operator fun inc() : IncDec = this
  operator fun dec() : IncDec = this
}

fun testIncDec() {
  var x = IncDec()
  x++
  ++x
  x--
  --x
  x = x++
  x = x--
  x = ++x
  x = --x
}

class WrongIncDec() {
  <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc() : Int = 1
  <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun dec() : Int = 1
}

fun testWrongIncDec() {
  var x = WrongIncDec()
  x<!ASSIGNMENT_TYPE_MISMATCH!>++<!>
  <!ASSIGNMENT_TYPE_MISMATCH!>++x<!>
  x<!ASSIGNMENT_TYPE_MISMATCH!>--<!>
  <!ASSIGNMENT_TYPE_MISMATCH!>--x<!>
}

class UnitIncDec() {
  <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc() : Unit {}
  <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun dec() : Unit {}
}

fun testUnitIncDec() {
  var x = UnitIncDec()
  x<!ASSIGNMENT_TYPE_MISMATCH!>++<!>
  <!ASSIGNMENT_TYPE_MISMATCH!>++x<!>
  x<!ASSIGNMENT_TYPE_MISMATCH!>--<!>
  <!ASSIGNMENT_TYPE_MISMATCH!>--x<!>
  x = x<!ASSIGNMENT_TYPE_MISMATCH!>++<!>
  x = x<!ASSIGNMENT_TYPE_MISMATCH!>--<!>
  x = <!ASSIGNMENT_TYPE_MISMATCH!><!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>x<!>
  x = <!ASSIGNMENT_TYPE_MISMATCH!><!INC_DEC_SHOULD_NOT_RETURN_UNIT!>--<!>x<!>
}
