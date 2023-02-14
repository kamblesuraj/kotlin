/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.ref

import kotlinx.cinterop.COpaquePointer
import kotlin.native.internal.*

/**
 *   Theory of operations:
 *
 *  Weak references in Kotlin/Native are implemented in the following way. Whenever weak reference to an
 * object is created, we atomically modify type info pointer in the object to point into a metaobject.
 * This metaobject contains a strong reference to the counter object (instance of WeakReferenceCounter class).
 * Every other weak reference contains a strong reference to the counter object.
 *
 *         [weak1]  [weak2]
 *             \      /
 *             V     V
 *     .......[Counter] <----
 *     .                     |
 *     .                     |
 *      ->[Object] -> [Meta]-
 *
 *   References from weak reference objects to the counter and from the metaobject to the counter are strong,
 *  and from the counter to the object is nullably weak. So whenever an object dies, if it has a metaobject,
 *  it is traversed to find a counter object, and atomically nullify reference to the object. Afterward, all attempts
 *  to get the object would yield null.
 */

// Clear holding the counter object, which refers to the actual object.
@NoReorderFields
@Frozen
@OptIn(FreezingIsDeprecated::class)
internal class WeakReferenceCounterLegacyMM(var referred: COpaquePointer?) : WeakReferenceImpl() {
    // Spinlock, potentially taken when materializing or removing 'referred' object.
    var lock: Int = 0

    // Optimization for concurrent access.
    var cookie: Int = 0

    @GCUnsafeCall("Konan_WeakReferenceCounterLegacyMM_get")
    external override fun get(): Any?
}

@NoReorderFields
@ExportTypeInfo("theWeakReferenceCounterTypeInfo")
@HasFinalizer // TODO: Consider just using Cleaners.
internal class WeakReferenceCounter(
    val weakRef: COpaquePointer,
    val referred: COpaquePointer, // TODO: This exists only for the ExtraObjectData's sake. Refactor and remove.
) : WeakReferenceImpl() {
    @GCUnsafeCall("Konan_WeakReferenceCounter_get")
    external override fun get(): Any?
}

@PublishedApi
internal abstract class WeakReferenceImpl {
    abstract fun get(): Any?
}

// Get a counter from non-null object.
@GCUnsafeCall("Konan_getWeakReferenceImpl")
@Escapes(0b01) // referent escapes.
external internal fun getWeakReferenceImpl(referent: Any): WeakReferenceImpl

// Create a counter object for legacy MM.
@ExportForCppRuntime
internal fun makeWeakReferenceCounterLegacyMM(referred: COpaquePointer) = WeakReferenceCounterLegacyMM(referred)

// Create a counter object.
@ExportForCppRuntime
internal fun makeWeakReferenceCounter(weakRef: COpaquePointer, referred: COpaquePointer) = WeakReferenceCounter(weakRef, referred)

internal class PermanentWeakReferenceImpl(val referred: Any): kotlin.native.ref.WeakReferenceImpl() {
    override fun get(): Any? = referred
}

// Create a reference to the permanent object.
@ExportForCppRuntime
internal fun makePermanentWeakReferenceImpl(referred: Any) = PermanentWeakReferenceImpl(referred)
