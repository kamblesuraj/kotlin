// FILE: Base.java

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Base {

    @NotNull String getFoo();

    void setFoo(String arg);
}

// FILE: SomeClass.java

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SomeClass implements Base {

    @Override
    @NotNull
    public String getFoo() {
        return "";
    }

    @Override
    public void setFoo(@Nullable String arg) {}
}

// FILE: test.kt

fun test(s: String) {
    val d = SomeClass()
    d.<!VAL_REASSIGNMENT!>foo<!> = s
}
