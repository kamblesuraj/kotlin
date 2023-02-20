/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>

#include "Memory.h"
#include "MemorySharedRefs.hpp"
#include "MultiSourceQueue.hpp"
#include "ThreadRegistry.hpp"

namespace kotlin {
namespace mm {

// Registry for all objects that have foreign references created for them (i.e. associated objects)
class ForeignRefRegistry : Pinned {
    using Mutex = SpinLock<MutexThreadStateHandling::kIgnore>;

public:
    class Record {
    public:
        explicit Record(BackRefFromAssociatedObject* owner) noexcept : owner_(owner) {}

        ~Record() {
            auto* owner = owner_.load(std::memory_order_relaxed);
            RuntimeAssert(owner == nullptr, "Record@%p is attached to owner %p during destruction", this, owner);
            auto* next = next_.load(std::memory_order_relaxed);
            RuntimeAssert(next == nullptr, "Record@%p is inside roots list with next %p during destruction", this, next);
        }

        void deinit() noexcept {
            // This happens during weak references invalidation.
            // Conservatively considering that Record might still be in the roots list. It'll be removed from there
            // during the next GC.
            owner_.store(nullptr, std::memory_order_relaxed);
        }

        void promote() noexcept {
            ForeignRefRegistry::instance().promoteToRoots(*this);
        }

        bool canBeSwept() const noexcept {
            // This happens during foreign refs sweeping.
            if (owner_.load(std::memory_order_relaxed))
                // If the record was not deinited, it can't possibly be swept.
                return false;
            // Only allow sweeping if the Record is not in the roots list.
            // If it's there, it'll be cleaned up during the next GC.
            return !next_.load(std::memory_order_relaxed);
        }

        bool needsToBeRoot() const noexcept {
            // This happens during roots scanning.
            auto* owner = owner_.load(std::memory_order_relaxed);
            return owner && owner->isReferenced();
        }

    private:
        friend class ForeignRefRegistry;

        std::atomic<BackRefFromAssociatedObject*> owner_ = nullptr;
        std::atomic<Record*> next_ = nullptr;
    };

    using Node = MultiSourceQueue<Record, Mutex>::Node;

    class ThreadQueue : Pinned {
    public:
        explicit ThreadQueue(ForeignRefRegistry& owner) noexcept : impl_(owner.impl_) {}

        Node* initForeignRef(BackRefFromAssociatedObject* backRef, bool commit) noexcept {
            auto* node = impl_.Emplace(backRef);
            if (commit) {
                (*node)->promote();
            }
            return node;
        }

        void publish() noexcept {
            impl_.Publish();
        }

        void clearForTests() noexcept {
            impl_.ClearForTests();
        }

    private:
        MultiSourceQueue<Record, Mutex>::Producer impl_;
    };

    class RootsIterator {
        inline static ObjHeader* nullObject = nullptr;
    public:
        ObjHeader*& operator*() noexcept {
            // Cleaning up owner can only happen during weak refs processing later.
            auto* owner = node_->owner_.load(std::memory_order_relaxed);
            return owner ? owner->objUnsafe() : nullObject;
        }

        RootsIterator& operator++() noexcept {
            node_ = owner_->nextRoot(node_);
            return *this;
        }

        bool operator==(const RootsIterator& rhs) const noexcept {
            return node_ == rhs.node_;
        }

        bool operator!=(const RootsIterator& rhs) const noexcept {
            return !(*this == rhs);
        }

    private:
        friend class ForeignRefRegistry;

        RootsIterator(ForeignRefRegistry& owner, Record* node) noexcept : owner_(&owner), node_(node) {}

        ForeignRefRegistry* owner_;
        Record* node_;
    };

    class RootsIterable : private MoveOnly {
    public:
        RootsIterator begin() noexcept {
            auto* node = owner_.nextRoot(&owner_.rootsHead_, 1000);
            return RootsIterator(owner_, node);
        }

        RootsIterator end() noexcept {
            return RootsIterator(owner_, &owner_.rootsTail_);
        }

    private:
        friend class ForeignRefRegistry;

        explicit RootsIterable(ForeignRefRegistry& owner) noexcept : owner_(owner) {}

        ForeignRefRegistry& owner_;
    };

    using Iterable = MultiSourceQueue<Record, Mutex>::Iterable;
    using Iterator = MultiSourceQueue<Record, Mutex>::Iterator;

    static ForeignRefRegistry& instance() noexcept;

    ForeignRefRegistry() noexcept {
        rootsHead_.next_.store(&rootsTail_, std::memory_order_relaxed);
    }

    ~ForeignRefRegistry() = default;

    RootsIterable iterateOverRoots() noexcept { return RootsIterable(*this); }

    // Lock registry for safe iteration.
    // TODO: Iteration over `impl_` will be slow, because it's `std_support::list` collected at different times from
    // different threads, and so the nodes are all over the memory. Use metrics to understand how
    // much of a problem is it.
    Iterable lockForIter() noexcept { return impl_.LockForIter(); }

    void clearForTests() noexcept { impl_.ClearForTests(); }

private:
    void promoteToRoots(Record& record) noexcept;
    Record* nextRoot(Record* record, int maxIterations = std::numeric_limits<int>::max()) noexcept;

    MultiSourceQueue<Record, Mutex> impl_;
    Record rootsHead_{nullptr};
    Record rootsTail_{nullptr};
};

} // namespace mm
} // namespace kotlin
