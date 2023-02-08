@RequiresOptIn
annotation class SomeOptIn

interface Some {
    @SomeOptIn
    val foo: String
}
