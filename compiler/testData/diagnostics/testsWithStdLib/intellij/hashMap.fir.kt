// FULL_JDK
// FIR_DUMP
// FILE: some/HashMap.java

package some;

public class HashMap<K, V> extends java.util.HashMap<K, V> {}

// FILE: test.kt
import java.util.*
import some.*

fun foo(): Any? = null

fun test() {
    val map = foo() as <!OVERLOAD_RESOLUTION_AMBIGUITY!>HashMap<String, String><!>
    val map2 = HashMap<String, String>()
}
