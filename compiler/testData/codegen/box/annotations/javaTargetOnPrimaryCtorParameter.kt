// WITH_STDLIB
// WITH_REFLECT
// TARGET_BACKEND: JVM_IR
// FIR_DUMP
// DUMP_IR

// FILE: NoTarget.java
package a;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface NoTarget {
}

// FILE: PropValueField.java
package a;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface PropValueField {
}

// FILE: ParameterOnly.java
package a;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface ParameterOnly {
}

// FILE: FieldOnly.java
package a;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface FieldOnly {
}

// FILE: test.kt
package a

import kotlin.reflect.full.declaredMemberProperties

class Foo(
    @NoTarget
    @PropValueField
    @ParameterOnly
    @FieldOnly
    var param: Int
)

fun box(): String {
    val clazz = Foo::class

    val parameterAnnotations = clazz.constructors.single().parameters.single().annotations.joinToString()
    val fieldAnnotations = Foo::class.java.getDeclaredField("param").annotations.joinToString()

    if (parameterAnnotations != "@a.NoTarget(), @a.PropValueField(), @a.ParameterOnly()") return "Parameters:" + parameterAnnotations
    if (fieldAnnotations != "@a.FieldOnly()") return "Field:" + fieldAnnotations

    return "OK"
}
