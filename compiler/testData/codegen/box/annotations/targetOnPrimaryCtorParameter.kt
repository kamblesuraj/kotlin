// WITH_STDLIB
// WITH_REFLECT
// TARGET_BACKEND: JVM_IR
// FIR_DUMP
// DUMP_IR

package a

import kotlin.reflect.full.declaredMemberProperties
import kotlin.annotation.AnnotationTarget.*

annotation class NoTarget

@Target(kotlin.annotation.AnnotationTarget.PROPERTY, VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class PropValueField

@Target(allowedTargets = [AnnotationTarget.PROPERTY])
annotation class PropertyOnly

@Target(allowedTargets = arrayOf(AnnotationTarget.VALUE_PARAMETER))
annotation class ParameterOnly

@Target(allowedTargets = *arrayOf(AnnotationTarget.FIELD))
annotation class FieldOnly

@Target(*[AnnotationTarget.PROPERTY])
annotation class PropertyOnly2

class Foo(
    @NoTarget
    @PropValueField
    @PropertyOnly
    @PropertyOnly2
    @ParameterOnly
    @FieldOnly
    var param: Int
)

fun box(): String {
    val clazz = Foo::class

    val parameterAnnotations = clazz.constructors.single().parameters.single().annotations.joinToString()
    val propertyAnnotations = clazz.declaredMemberProperties.single().annotations.joinToString()
    val fieldAnnotations = Foo::class.java.getDeclaredField("param").annotations.joinToString()

    if (parameterAnnotations != "@a.NoTarget(), @a.PropValueField(), @a.ParameterOnly()") return "Parameters:" + parameterAnnotations
    if (propertyAnnotations != "@a.PropertyOnly(), @a.PropertyOnly2()") return "Property:" + propertyAnnotations
    if (fieldAnnotations != "@a.FieldOnly()") return "Field:" + fieldAnnotations

    return "OK"
}
