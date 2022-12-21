// SKIP_TXT

// FILE: testFunctions.kt

package functions

// TESTCASE NUMBER: 1
fun f1(x1: Byte) = x1

// TESTCASE NUMBER: 2
fun f2(x1: Short) = x1

// TESTCASE NUMBER: 3, 4
fun f3(x1: Int) = x1

// FILE: main.kt

import functions.*

// TESTCASE NUMBER: 1
fun case_1() {
    f1(<!ARGUMENT_TYPE_MISMATCH!>128<!>)
    f1(<!ARGUMENT_TYPE_MISMATCH!>-129<!>)
    f1(<!ARGUMENT_TYPE_MISMATCH!>32767<!>)
    f1(<!ARGUMENT_TYPE_MISMATCH!>-32768<!>)
    f1(<!ARGUMENT_TYPE_MISMATCH!>2147483647<!>)
    f1(<!ARGUMENT_TYPE_MISMATCH!>-2147483648<!>)
    f1(<!ARGUMENT_TYPE_MISMATCH!>9223372036854775807<!>)
    f1(<!ARGUMENT_TYPE_MISMATCH!>-9223372036854775807<!>)
    f1(<!INT_LITERAL_OUT_OF_RANGE!>1000000000000000000000000000000000000000000000000<!>)
    f1(<!OVERLOAD_RESOLUTION_AMBIGUITY!>-<!><!INT_LITERAL_OUT_OF_RANGE!>1000000000000000000000000000000000000000000000000<!>)
}

// TESTCASE NUMBER: 2
fun case_2() {
    f2(<!ARGUMENT_TYPE_MISMATCH!>32768<!>)
    f2(<!ARGUMENT_TYPE_MISMATCH!>-32769<!>)
    f2(<!ARGUMENT_TYPE_MISMATCH!>2147483647<!>)
    f2(<!ARGUMENT_TYPE_MISMATCH!>-2147483648<!>)
    f2(<!ARGUMENT_TYPE_MISMATCH!>9223372036854775807<!>)
    f2(<!ARGUMENT_TYPE_MISMATCH!>-9223372036854775807<!>)
    f2(<!INT_LITERAL_OUT_OF_RANGE!>1000000000000000000000000000000000000000000000000<!>)
    f2(<!OVERLOAD_RESOLUTION_AMBIGUITY!>-<!><!INT_LITERAL_OUT_OF_RANGE!>1000000000000000000000000000000000000000000000000<!>)
}

// TESTCASE NUMBER: 3
fun case_3() {
    f3(<!ARGUMENT_TYPE_MISMATCH!>9223372036854775807<!>)
    f3(<!ARGUMENT_TYPE_MISMATCH!>-9223372036854775807<!>)
    f3(<!INT_LITERAL_OUT_OF_RANGE!>1000000000000000000000000000000000000000000000000<!>)
    f3(<!OVERLOAD_RESOLUTION_AMBIGUITY!>-<!><!INT_LITERAL_OUT_OF_RANGE!>1000000000000000000000000000000000000000000000000<!>)
}

// TESTCASE NUMBER: 4
fun case_4() {
    f3(<!INT_LITERAL_OUT_OF_RANGE!>1000000000000000000000000000000000000000000000000<!>)
    f3(<!OVERLOAD_RESOLUTION_AMBIGUITY!>-<!><!INT_LITERAL_OUT_OF_RANGE!>1000000000000000000000000000000000000000000000000<!>)
}
