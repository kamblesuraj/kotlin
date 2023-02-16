// FULL_JDK
// FILE: ContainerUtil.java

import org.jetbrains.annotations.NotNull;
import java.util.Set;

public class ContainerUtil {
    public static @NotNull <T> Set<T> set(T @NotNull ... items) {
        return null;
    }
}

// FILE: test.kt

class Some

fun foo(some: Some) {
    ContainerUtil.set(some)
}
