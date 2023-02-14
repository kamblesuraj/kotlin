// FIR_IDENTICAL
// FULL_JDK
// FILE: Function.java

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface Function<F extends @Nullable Object, T extends @Nullable Object> extends java.util.function.Function<F, T> {
    @Override
    // @ParametricNullness
    T apply(/* @ParametricNullness */ F input);
}

// FILE: Store.kt

class Store : Function<Store.Key<*>?, Any?> {
    data class Key<T>(val expectedClass: Class<T>)

    override fun apply(input: Key<*>?): Any? = null
}
