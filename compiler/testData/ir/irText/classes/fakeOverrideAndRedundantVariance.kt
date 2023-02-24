interface Out<out T>

open class Base<SuperTP> {
    open fun foo(i: Out<out SuperTP>) {}
}

class Derived<SubTP>: Base<SubTP>() {
//    fake override fun foo(i: Out<SubTP>)
}
