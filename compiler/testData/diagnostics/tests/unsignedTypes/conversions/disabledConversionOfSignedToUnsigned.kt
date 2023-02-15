// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: -ImplicitSignedToUnsignedIntegerConversion

const val IMPLICIT_INT = 255
const val EXPLICIT_INT: Int = 255
const val LONG_CONST = 255L
val NON_CONST = 255
const val BIGGER_THAN_UBYTE = 256
const val UINT_CONST = 42u

fun takeUByte(u: UByte) {}
fun takeUShort(u: UShort) {}
fun takeUInt(u: UInt) {}
fun takeULong(u: ULong) {}

fun takeUBytes(vararg u: UByte) {}

fun takeLong(l: Long) {}

fun takeUIntWithoutAnnotaion(u: UInt) {}

fun takeIntWithoutAnnotation(i: Int) {}

fun test() {
    takeUByte(<!TYPE_MISMATCH, UNSUPPORTED_FEATURE!>IMPLICIT_INT<!>)
    takeUByte(<!TYPE_MISMATCH, UNSUPPORTED_FEATURE!>EXPLICIT_INT<!>)

    takeUShort(<!TYPE_MISMATCH, UNSUPPORTED_FEATURE!>IMPLICIT_INT<!>)
    takeUShort(<!TYPE_MISMATCH, UNSUPPORTED_FEATURE!>BIGGER_THAN_UBYTE<!>)

    takeUInt(<!TYPE_MISMATCH, UNSUPPORTED_FEATURE!>IMPLICIT_INT<!>)

    takeULong(<!TYPE_MISMATCH, UNSUPPORTED_FEATURE!>IMPLICIT_INT<!>)

    takeUBytes(<!TYPE_MISMATCH!>IMPLICIT_INT<!>, <!TYPE_MISMATCH!>EXPLICIT_INT<!>, 42u)

    takeLong(<!TYPE_MISMATCH!>IMPLICIT_INT<!>)

    takeIntWithoutAnnotation(IMPLICIT_INT)

    takeUIntWithoutAnnotaion(UINT_CONST)

    takeUByte(<!TYPE_MISMATCH, UNSUPPORTED_FEATURE!>LONG_CONST<!>)
    takeUByte(<!TYPE_MISMATCH!>NON_CONST<!>)
    takeUByte(<!TYPE_MISMATCH, UNSUPPORTED_FEATURE!>BIGGER_THAN_UBYTE<!>)
    takeUByte(<!TYPE_MISMATCH!>UINT_CONST<!>)
    takeUIntWithoutAnnotaion(<!TYPE_MISMATCH, UNSUPPORTED_FEATURE!>IMPLICIT_INT<!>)
}
