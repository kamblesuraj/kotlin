enum class SimpleEnum {
    ONE,
    TWO
}

class X(val num: Int)

private var globalX: X? = null

fun getX(num: Int): X {
    val x = X(num)
    globalX = x
    return x
}

fun scheduleGC() = kotlin.native.internal.GC.schedule()
