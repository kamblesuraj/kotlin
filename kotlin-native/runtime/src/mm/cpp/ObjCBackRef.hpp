/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Memory.h"
#include "RawPtr.hpp"
#include "SpecialRefRegistry.hpp"
#include "Utils.hpp"

namespace kotlin::mm {

// Reference from an ObjC associated object back into a Kotlin object.
// GC automatically tracks references with refcount > 0 as roots, and invalidates references with refcount = 0 when the Kotlin object is
// collected. Use `create` and `dispose` to create and destroy the back reference.
class ObjCBackRef : private MoveOnly {
public:
    ObjCBackRef() noexcept = default;

    // Cast raw ref into a back reference.
    explicit ObjCBackRef(RawSpecialRef* raw) noexcept : node_(SpecialRefRegistry::Node::fromRaw(raw)) {}

    // Cast back reference into a raw ref
    [[nodiscard("must be manually disposed")]] explicit operator RawSpecialRef*() && noexcept {
        // Make sure to move out from node_.
        auto node = std::move(node_);
        return node->asRaw();
    }

    // Create new back reference for `obj`.
    [[nodiscard("must be manually disposed")]] static ObjCBackRef create(ObjHeader* obj) noexcept;

    // Dispose back reference.
    void dispose() && noexcept {
        // Make sure to move out from node_.
        auto node = std::move(node_);
        // Can be safely called with any thread state.
        node->dispose();
    }

    // Increment refcount.
    void retain() noexcept {
        // Can be safely called with any thread state.
        node_->retainRef();
    }

    // Decrement refcount.
    void release() noexcept {
        // Can be safely called with any thread state.
        node_->releaseRef();
    }

    // Try incrementing refcount. Will fail if the underlying object is not alive.
    [[nodiscard("refcount change must be processed")]] bool tryRetain() noexcept {
        CalledFromNativeGuard guard;
        return tryRetainForTests();
    }

    // Get the underlying object.
    // The result is only safe to use only with refcount > 0.
    [[nodiscard("expensive pure function")]] ObjHeader* operator*() noexcept { return node_->ref(); }

    bool tryRetainForTests() noexcept {
        ObjHolder holder;
        if (auto* obj = node_->tryRef(holder.slot())) {
            node_->retainRef();
            return true;
        }
        return false;
    }

private:
    raw_ptr<SpecialRefRegistry::Node> node_;
};

} // namespace kotlin::mm
