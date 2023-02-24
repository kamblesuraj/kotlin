val prop: Int =
    js("1")

fun funExprBody(x: Int): Int =
    js("x")

fun funBlockBody(x: Int): Int {
    js("return x;")
}

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>returnTypeNotSepcified<!>() = js("1")
val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>valTypeNotSepcified<!> = js("1")

val a = "1"
fun nonConst(): String = "1"

val p0: Int = js(a)
val p1: Int = js(("1"))
val p2: Int = js("$a")
val p3: Int = js("${1}")
val p4: Int = js("${a}${a}")
val p5: Int = js(a + a)
val p6: Int = js("1" + "1")
val p7: Int = js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>nonConst()<!>)

fun foo0(b: Boolean): Int =
    if (b) <!JSCODE_WRONG_CONTEXT!>js<!>("1") else <!JSCODE_WRONG_CONTEXT!>js<!>("2")

fun foo1(): Int {
    println()
    <!JSCODE_WRONG_CONTEXT!>js<!>("return x;")
}

fun foo11() {
    fun local1(): Int = <!JSCODE_WRONG_CONTEXT!>js<!>("1")
    fun local2(): Int {
        <!JSCODE_WRONG_CONTEXT!>js<!>("return 1;")
    }
    fun local3(): Int {
        println()
        <!JSCODE_WRONG_CONTEXT!>js<!>("return 1;")
    }
}

class C {
    fun memberFun1(): Int = <!JSCODE_WRONG_CONTEXT!>js<!>("1")
    fun memberFun2(): Int {
        <!JSCODE_WRONG_CONTEXT!>js<!>("return 1;")
    }

    constructor() <!UNREACHABLE_CODE!>{
        <!JSCODE_WRONG_CONTEXT!>js<!>("1;")
    }<!>

    init {
        <!JSCODE_WRONG_CONTEXT!>js<!>("1")
    }

    <!UNREACHABLE_CODE!>val memberProperty: Int = <!JSCODE_WRONG_CONTEXT!>js<!>("1")<!>
}

fun withDefault(x: Int = <!JSCODE_WRONG_CONTEXT!>js<!>("1")) {
    println(x)
}

suspend fun suspendFun(): Int = <!JSCODE_UNSUPPORTED_FUNCTION_KIND!>js<!>("1")

inline fun inlineFun(f: () -> Int): Int = <!JSCODE_UNSUPPORTED_FUNCTION_KIND!>js<!>("f()")

fun Int.extensionFun(): Int = <!JSCODE_UNSUPPORTED_FUNCTION_KIND!>js<!>("1")

var propertyWithAccessors: Int
    get(): Int = <!JSCODE_WRONG_CONTEXT!>js<!>("1")
    set(<!UNUSED_PARAMETER!>value<!>: Int) {
        <!JSCODE_WRONG_CONTEXT!>js<!>("console.log(value);")
    }


fun invalidNames(
    <!JSCODE_INVALID_PARAMETER_NAME!>`a b`: Int<!>,
    <!JSCODE_INVALID_PARAMETER_NAME!>`1b`: Int<!>,
    `ab$`: Int
): Int = js("1")
