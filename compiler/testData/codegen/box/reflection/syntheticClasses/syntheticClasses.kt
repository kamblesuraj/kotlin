// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: syntheticClasses.kt

package test

import kotlin.reflect.*
import kotlin.test.*

fun check(
    x: KClass<*>,
    expectedSimpleName: String?,
    expectedQualifiedName: String?,
    expectedVisibility: KVisibility? = KVisibility.PUBLIC,
) {
    // isAnonymousClass/simpleName behavior is different for JDK 1.8 and 9+, see KT-23072.
    if (x.java.isAnonymousClass) {
        assertEquals(null, x.simpleName)
    } else {
        assertEquals(expectedSimpleName, x.simpleName)
    }

    assertEquals(expectedQualifiedName, x.qualifiedName)

    assertEquals(setOf("equals", "hashCode", "toString"), x.members.mapTo(hashSetOf()) { it.name })

    assertEquals(emptyList(), x.annotations)
    assertEquals(emptyList(), x.constructors)
    assertEquals(emptyList(), x.nestedClasses)
    assertEquals(null, x.objectInstance)
    assertEquals(listOf(typeOf<Any>()), x.supertypes)
    assertEquals(emptyList(), x.sealedSubclasses)

    assertEquals(expectedVisibility, x.visibility)
    assertTrue(x.isFinal)
    assertFalse(x.isOpen)
    assertFalse(x.isAbstract)
    assertFalse(x.isSealed)
    assertFalse(x.isData)
    assertFalse(x.isInner)
    assertFalse(x.isCompanion)
    assertFalse(x.isFun)
    assertFalse(x.isValue)

    assertFalse(x.isInstance(42))
}

fun checkLambda() {
    val lambda = {}
    val klass = lambda::class
    check(klass, "lambda\$1", null)

    assertTrue(klass.isInstance(lambda))
    assertNotEquals(klass, {}::class)
    val equals = klass.members.single { it.name == "equals" } as KFunction<Boolean>
    assertTrue(equals.call(lambda, lambda))
}

fun checkKotlinAnonymousObject() {
    val anonymousObject = object {}
    val klass = anonymousObject::class
    check(
        klass, "anonymousObject\$1", null,
        // Kotlin anonymous object have "local" visibility in the metadata, which is null in kotlin-reflect because it can't be represented
        // as a Kotlin modifier (see `KClass.visibility`).
        expectedVisibility = null
    )

    assertTrue(klass.isInstance(anonymousObject))
    assertNotEquals<KClass<*>>(klass, object {}::class)
    val equals = klass.members.single { it.name == "equals" } as KFunction<Boolean>
    assertTrue(equals.call(anonymousObject, anonymousObject))
}

fun checkJavaAnonymousObject() {
    val anonymousObject = JavaClass.anonymousObject()
    val klass = anonymousObject::class
    check(klass, null, null)

    assertTrue(klass.isInstance(anonymousObject))
    val equals = klass.members.single { it.name == "equals" } as KFunction<Boolean>
    assertTrue(equals.call(anonymousObject, anonymousObject))
}

fun checkFileClass() {
    val fileClass = Class.forName("test.SyntheticClassesKt").kotlin
    check(fileClass, "SyntheticClassesKt", "test.SyntheticClassesKt")
}

fun box(): String {
    checkLambda()
    checkKotlinAnonymousObject()
    checkJavaAnonymousObject()
    checkFileClass()
    return "OK"
}

// FILE: JavaClass.java

public class JavaClass {
    public static Object anonymousObject() {
        return new Object() {};
    }
}
