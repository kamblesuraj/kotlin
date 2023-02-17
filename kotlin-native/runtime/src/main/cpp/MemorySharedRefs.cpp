/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Exceptions.h"
#include "MemorySharedRefs.hpp"
#include "Runtime.h"
#include "Types.h"

extern "C" {
// Returns a string describing object at `address` of type `typeInfo`.
OBJ_GETTER(DescribeObjectForDebugging, KConstNativePtr typeInfo, KConstNativePtr address);
}  // extern "C"

namespace {

inline bool isForeignRefAccessible(ObjHeader* object, ForeignRefContext context) {
    // If runtime has not been initialized on this thread, then the object is either unowned or shared.
    // In the former case initialized runtime is required to throw exceptions
    // in the latter case -- to provide proper execution context for caller.
    // TODO: this probably can't be called in uninitialized state in the new MM.
    Kotlin_initRuntimeIfNeeded();

    return IsForeignRefAccessible(object, context);
}

RUNTIME_NORETURN inline void throwIllegalSharingException(ObjHeader* object) {
  // TODO: add some info about the context.
  // Note: retrieving 'type_info()' is supposed to be correct even for unowned object.
  ThrowIllegalObjectSharingException(object->type_info(), object);
}

RUNTIME_NORETURN inline void terminateWithIllegalSharingException(ObjHeader* object) {
#if KONAN_NO_EXCEPTIONS
  // This will terminate.
  throwIllegalSharingException(object);
#else
  try {
    throwIllegalSharingException(object);
  } catch (...) {
    // A trick to terminate with unhandled exception. This will print a stack trace
    // and write to iOS crash log.
    std::terminate();
  }
#endif
}

template <ErrorPolicy errorPolicy>
bool ensureRefAccessible(ObjHeader* object, ForeignRefContext context) {
  static_assert(errorPolicy != ErrorPolicy::kIgnore, "Must've been handled by specialization");

  if (isForeignRefAccessible(object, context)) {
    return true;
  }

  switch (errorPolicy) {
    case ErrorPolicy::kDefaultValue:
      return false;
    case ErrorPolicy::kThrow:
      throwIllegalSharingException(object);
    case ErrorPolicy::kTerminate:
      terminateWithIllegalSharingException(object);
  }
}

template <>
bool ensureRefAccessible<ErrorPolicy::kIgnore>(ObjHeader* object, ForeignRefContext context) {
  return true;
}

}  // namespace

void KRefSharedHolder::initLocal(ObjHeader* obj) {
  RuntimeAssert(obj != nullptr, "must not be null");
  if (CurrentMemoryModel == MemoryModel::kExperimental) {
    stablePointer_ = nullptr;
    obj_ = obj;
    return;
  }

  context_ = InitLocalForeignRef(obj);
  obj_ = obj;
}

void KRefSharedHolder::init(ObjHeader* obj) {
  RuntimeAssert(obj != nullptr, "must not be null");
  if (CurrentMemoryModel == MemoryModel::kExperimental) {
    stablePointer_ = CreateStablePointer(obj);
    obj_ = obj;
    return;
  }

  context_ = InitForeignRefLegacyMM(obj);
  obj_ = obj;
}

template <ErrorPolicy errorPolicy>
ObjHeader* KRefSharedHolder::ref() const {
  if (CurrentMemoryModel == MemoryModel::kExperimental) {
    kotlin::AssertThreadState(kotlin::ThreadState::kRunnable);
    return obj_;
  }

  if (!ensureRefAccessible<errorPolicy>(obj_, context_)) {
    return nullptr;
  }

  AdoptReferenceFromSharedVariable(obj_);
  return obj_;
}

template ObjHeader* KRefSharedHolder::ref<ErrorPolicy::kDefaultValue>() const;
template ObjHeader* KRefSharedHolder::ref<ErrorPolicy::kThrow>() const;
template ObjHeader* KRefSharedHolder::ref<ErrorPolicy::kTerminate>() const;

void KRefSharedHolder::dispose() const {
  if (obj_ == nullptr) {
    // To handle the case when it is not initialized. See [KotlinMutableSet/Dictionary dealloc].
    return;
  }
  if (CurrentMemoryModel == MemoryModel::kExperimental) {
    DisposeStablePointer(stablePointer_);
    return;
  }

  DeinitForeignRefLegacyMM(obj_, context_);
}

OBJ_GETTER0(KRefSharedHolder::describe) const {
  // Note: retrieving 'type_info()' is supposed to be correct even for unowned object.
  RETURN_RESULT_OF(DescribeObjectForDebugging, obj_->type_info(), obj_);
}

void BackRefFromAssociatedObject::initAndAddRef(ObjHeader* obj, bool commit) {
  RuntimeAssert(obj != nullptr, "must not be null");
  obj_ = obj;
  if (CurrentMemoryModel == MemoryModel::kExperimental) {
    refCount = 1;
    context_ = InitForeignRef(this, commit);
    return;
  }

  // Generally a specialized addRef below:
  context_ = InitForeignRefLegacyMM(obj);
  refCount = 1;
}

void BackRefFromAssociatedObject::commit() {
  if (CurrentMemoryModel == MemoryModel::kExperimental) {
    ForeignRefPromote(context_);
  }
}

template <ErrorPolicy errorPolicy>
void BackRefFromAssociatedObject::addRef() {
  static_assert(errorPolicy != ErrorPolicy::kDefaultValue, "Cannot use default return value here");

  // Can be called both from Native state (if ObjC or Swift code adds RC)
  // and from Runnable state (Kotlin_ObjCExport_refToObjC).

  if (atomicAdd(&refCount, 1) == 1) {
    if (obj_ == nullptr) return; // E.g. after [detach].

    if (CurrentMemoryModel == MemoryModel::kExperimental) {
      kotlin::CalledFromNativeGuard guard(/* reentrant */ true);
      // Important for the changes to refCount to be visible inside this call.
      ForeignRefPromote(context_);
      return;
    }

    // There are no references to the associated object itself, so Kotlin object is being passed from Kotlin,
    // and it is owned therefore.
    ensureRefAccessible<errorPolicy>(obj_, context_); // TODO: consider removing explicit verification.

    // Foreign reference has already been deinitialized (see [releaseRef]).
    // Create a new one:
    context_ = InitForeignRefLegacyMM(obj_);
  }
}

template void BackRefFromAssociatedObject::addRef<ErrorPolicy::kThrow>();
template void BackRefFromAssociatedObject::addRef<ErrorPolicy::kTerminate>();

template <ErrorPolicy errorPolicy>
bool BackRefFromAssociatedObject::tryAddRef() {
  static_assert(errorPolicy != ErrorPolicy::kDefaultValue, "Cannot use default return value here");
  kotlin::CalledFromNativeGuard guard;

  if (obj_ == nullptr) return false; // E.g. after [detach].

  if (CurrentMemoryModel == MemoryModel::kExperimental) {
      ObjHolder holder;
      ObjHeader* obj = TryRef(obj_, holder.slot());
      // Failed to lock weak reference.
      if (obj == nullptr) return false;
      RuntimeAssert(obj == obj_, "Mismatched locked weak. obj=%p obj_=%p", obj, obj_);
      // TODO: This is a very weird way to ask for "unsafe" addRef.
      addRef<ErrorPolicy::kIgnore>();
      return true;
  } else {
      // Suboptimal but simple:
      ensureRefAccessible<errorPolicy>(obj_, context_);

      ObjHeader* obj = obj_;

      if (!TryAddHeapRef(obj)) return false;
      RuntimeAssert(isForeignRefAccessible(obj_, context_), "Cannot be inaccessible because of the check above");
      // TODO: This is a very weird way to ask for "unsafe" addRef.
      addRef<ErrorPolicy::kIgnore>();
      ReleaseHeapRefNoCollect(obj); // Balance TryAddHeapRef.
      // TODO: consider optimizing for non-shared objects.

      return true;
  }
}

template bool BackRefFromAssociatedObject::tryAddRef<ErrorPolicy::kThrow>();
template bool BackRefFromAssociatedObject::tryAddRef<ErrorPolicy::kTerminate>();

void BackRefFromAssociatedObject::releaseRef() {
  if (CurrentMemoryModel == MemoryModel::kExperimental) {
    atomicAdd(&refCount, -1);
    return;
  }

  ForeignRefContext context = context_;
  if (atomicAdd(&refCount, -1) == 0) {
    if (obj_ == nullptr) return; // E.g. after [detach].

    kotlin::CalledFromNativeGuard guard;

    // Note: by this moment "subsequent" addRef may have already happened and patched context_.
    // So use the value loaded before refCount update:
    DeinitForeignRefLegacyMM(obj_, context);
    // From this moment [context] is generally a dangling pointer.
    // This is handled in [IsForeignRefAccessible] and [addRef].
    // TODO: This probably isn't fine in new MM. Make sure it works.
  }
}

void BackRefFromAssociatedObject::detach() {
  RuntimeAssert(atomicGet(&refCount) == 0, "unexpected refCount");
  // TODO: Racy with concurrent extra objects sweep.
  obj_ = nullptr; // Handled in addRef/tryAddRef/releaseRef/ref.
  if (CurrentMemoryModel == MemoryModel::kExperimental) {
    auto* context = context_;
    context_ = nullptr;
    DeinitForeignRef(context);
  }
}

ALWAYS_INLINE void BackRefFromAssociatedObject::assertDetached() {
  if (CurrentMemoryModel == MemoryModel::kExperimental) {
    RuntimeAssert(obj_ == nullptr && context_ == nullptr, "Expecting this=%p to be detached, but found obj_=%p context_=%p", this, obj_, context_);
  } else {
    RuntimeAssert(obj_ == nullptr, "Expecting this=%p to be detached, but found obj_=%p", this, obj_);
  }
}

template <ErrorPolicy errorPolicy>
ObjHeader* BackRefFromAssociatedObject::ref() const {
  kotlin::AssertThreadState(kotlin::ThreadState::kRunnable);
  RuntimeAssert(obj_ != nullptr, "no valid Kotlin object found");
  if (CurrentMemoryModel == MemoryModel::kExperimental) {
    return obj_;
  }

  if (!ensureRefAccessible<errorPolicy>(obj_, context_)) {
    return nullptr;
  }

  AdoptReferenceFromSharedVariable(obj_);
  return obj_;
}

template ObjHeader* BackRefFromAssociatedObject::ref<ErrorPolicy::kDefaultValue>() const;
template ObjHeader* BackRefFromAssociatedObject::ref<ErrorPolicy::kThrow>() const;
template ObjHeader* BackRefFromAssociatedObject::ref<ErrorPolicy::kTerminate>() const;

bool BackRefFromAssociatedObject::isReferenced() const {
  return atomicGetAcquire(&refCount) > 0;
}

extern "C" {
RUNTIME_NOTHROW void KRefSharedHolder_initLocal(KRefSharedHolder* holder, ObjHeader* obj) {
  holder->initLocal(obj);
}

RUNTIME_NOTHROW void KRefSharedHolder_init(KRefSharedHolder* holder, ObjHeader* obj) {
  holder->init(obj);
}

RUNTIME_NOTHROW void KRefSharedHolder_dispose(const KRefSharedHolder* holder) {
  holder->dispose();
}

RUNTIME_NOTHROW ObjHeader* KRefSharedHolder_ref(const KRefSharedHolder* holder) {
  return holder->ref<ErrorPolicy::kTerminate>();
}
} // extern "C"
