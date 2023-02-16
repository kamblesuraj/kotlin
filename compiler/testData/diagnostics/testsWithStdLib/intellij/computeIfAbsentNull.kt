// FIR_IDENTICAL
// FULL_JDK
// FILE: Table.java

import org.jetbrains.annotations.Nullable;

public interface Table<R extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object> {
    Map<K, V> row(R r);

    String getPackageName();
}

// FILE: ResourceType.java

public enum ResourceType {
    SOME;
}

// test.kt

fun foo(table: Table<ResourceType, String, String>, flag: Boolean) {
    table.row(ResourceType.SOME).computeIfAbsent("123") {
        if (flag) {
            return@computeIfAbsent table.packageName
        }
        null
    }
}
