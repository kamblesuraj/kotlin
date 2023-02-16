// FILE: Base.java

public interface Base {}

// FILE: Derived.java

public interface Derived extends Base {}

// FILE: Lists.java

import java.util.ArrayList;
import org.jetbrains.annotations.Nullable;

public class Lists {
    public static <E extends @Nullable Object> ArrayList<E> newArrayList() {
        return new ArrayList<>();
    }
}

// FILE: test.kt

fun foo(arg: Derived?, x: Any?) {
    var arg: Derived? = arg
    val list = Lists.newArrayList<Base>()
    if (arg != null) {
        list.add(arg)
    }
    list.add(<!ARGUMENT_TYPE_MISMATCH!>arg<!>)

    loop@ for (i in 0..9) {
        when {
            i == 3 -> {
                if (arg != null) {
                    list.add(arg)
                }
            }
            i == 5 && x is Derived -> arg = x
            else -> {
                if (i >= 8) {
                    if (arg != null) {
                        list.add(arg)
                    }
                    continue@loop
                }
                if (i == 6) {
                    list.add(<!ARGUMENT_TYPE_MISMATCH!>arg<!>)
                }
            }
        }
    }
}
