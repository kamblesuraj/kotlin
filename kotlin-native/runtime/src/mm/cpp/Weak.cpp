/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Weak.h"

#include "ExtraObjectData.hpp"
#include "WeakRef.hpp"
#include "ThreadState.hpp"
#include "Types.h"

using namespace kotlin;

extern "C" {
OBJ_GETTER(makeWeakReferenceCounter, void*, void*);
}

namespace {

// TODO: Consider moving weak ref disposable to the finalizer thread
//       to avoid doing this fixed layout hack.
struct WeakReferenceCounter {
    ObjHeader header;
    mm::RawSpecialRef* weakRef;
    void* referred;
};

WeakReferenceCounter* asWeakReferenceCounter(ObjHeader* counter) noexcept {
    return reinterpret_cast<WeakReferenceCounter*>(counter);
}

[[nodiscard]] mm::WeakRef weakRefForCounter(ObjHeader* counter) noexcept {
    return mm::WeakRef(asWeakReferenceCounter(counter)->weakRef);
}

} // namespace

OBJ_GETTER(mm::createWeakReferenceCounter, ObjHeader* object) noexcept {
    auto* thread = mm::ThreadRegistry::Instance().CurrentThreadData();
    AssertThreadState(thread, ThreadState::kRunnable);

    auto& extraObject = mm::ExtraObjectData::GetOrInstall(object);
    if (auto* counter = extraObject.GetWeakReferenceCounter()) {
        RETURN_OBJ(counter);
    }
    ObjHolder holder;
    auto* counter = makeWeakReferenceCounter(static_cast<mm::RawSpecialRef*>(mm::WeakRef::create(object)), object, holder.slot());
    auto* setCounter = extraObject.GetOrSetWeakReferenceCounter(object, counter);
    RETURN_OBJ(setCounter);
}

void mm::disposeWeakReferenceCounter(ObjHeader* counter) noexcept {
    weakRefForCounter(counter).dispose();
}

OBJ_GETTER(mm::derefWeakReferenceCounter, ObjHeader* counter) noexcept {
    RETURN_RESULT_OF0(weakRefForCounter(counter).tryRef);
}

ObjHeader* mm::weakReferenceCounterBaseObjectUnsafe(ObjHeader* counter) noexcept {
    return static_cast<ObjHeader*>(asWeakReferenceCounter(counter)->referred);
}
