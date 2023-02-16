// FILE: Option.java

public interface Option<T> {
    T get();

    final class Some<T> implements Option<T> {
        @Override
        T get() {
            return null;
        }
    }
}

// FILE: test.kt

fun foo(option: Option<Pair<String, String>>?) {
    if (option is Option.Some<*>) {
        val (_, x) = <!COMPONENT_FUNCTION_AMBIGUITY!>option.get()<!>
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(x)
    }
}
