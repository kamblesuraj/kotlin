/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemorySharedRefs.hpp"

#include "ObjCBackRef.hpp"
#include "StableRef.hpp"

using namespace kotlin;

void KRefSharedHolder::initLocal(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    ref_ = nullptr;
    obj_ = obj;
}

void KRefSharedHolder::init(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    ref_ = static_cast<mm::RawSpecialRef*>(mm::StableRef::create(obj));
    obj_ = obj;
}

template <ErrorPolicy errorPolicy>
ObjHeader* KRefSharedHolder::ref() const {
    AssertThreadState(ThreadState::kRunnable);
    // stablePointer_ may be null if created with initLocal.
    return obj_;
}

template ObjHeader* KRefSharedHolder::ref<ErrorPolicy::kDefaultValue>() const;
template ObjHeader* KRefSharedHolder::ref<ErrorPolicy::kThrow>() const;
template ObjHeader* KRefSharedHolder::ref<ErrorPolicy::kTerminate>() const;

void KRefSharedHolder::dispose() const {
    // Handles the case when it is not initialized. See [KotlinMutableSet/Dictionary dealloc].
    if (!ref_) {
        return;
    }
    mm::StableRef(ref_).dispose();
    // obj_ and stablePointer_ are dangling now.
}

void BackRefFromAssociatedObject::initAndAddRef(ObjHeader* obj) {
    ref_ = static_cast<mm::RawSpecialRef*>(mm::ObjCBackRef::create(obj));
}

template <ErrorPolicy errorPolicy>
void BackRefFromAssociatedObject::addRef() {
    mm::ObjCBackRef(ref_).retain();
}

template void BackRefFromAssociatedObject::addRef<ErrorPolicy::kThrow>();
template void BackRefFromAssociatedObject::addRef<ErrorPolicy::kTerminate>();

template <ErrorPolicy errorPolicy>
bool BackRefFromAssociatedObject::tryAddRef() {
    return mm::ObjCBackRef(ref_).tryRetain();
}

template bool BackRefFromAssociatedObject::tryAddRef<ErrorPolicy::kThrow>();
template bool BackRefFromAssociatedObject::tryAddRef<ErrorPolicy::kTerminate>();

void BackRefFromAssociatedObject::releaseRef() {
    mm::ObjCBackRef(ref_).release();
}

void BackRefFromAssociatedObject::detach() {
    RuntimeFail("Legacy MM only");
}

void BackRefFromAssociatedObject::dealloc() {
    mm::ObjCBackRef(ref_).dispose();
}

template <ErrorPolicy errorPolicy>
ObjHeader* BackRefFromAssociatedObject::ref() const {
    return *mm::ObjCBackRef(ref_);
}

template ObjHeader* BackRefFromAssociatedObject::ref<ErrorPolicy::kDefaultValue>() const;
template ObjHeader* BackRefFromAssociatedObject::ref<ErrorPolicy::kThrow>() const;
template ObjHeader* BackRefFromAssociatedObject::ref<ErrorPolicy::kTerminate>() const;
