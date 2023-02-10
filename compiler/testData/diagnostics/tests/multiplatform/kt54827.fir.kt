// MODULE: m1-common
// FILE: common.kt
expect class SomeClass<<!NO_ACTUAL_FOR_EXPECT{JVM}!>T<!>> {
    fun foo()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
<!ACTUAL_WITHOUT_EXPECT!>actual class SomeClass {
    actual fun foo() {}
}<!>
