/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>

#include "Memory.h"
#include "RawPtr.hpp"
#include "ThreadRegistry.hpp"
#include "std_support/List.hpp"

namespace kotlin::mm {

class ObjCBackRef;
class StableRef;
class WeakRef;

// Registry for all special references to objects:
// * stable references (i.e. always part of the root set)
// * weak references
// * ObjC back references. A mix between stable and weak references
//   created for ObjC part of Kotlin objects. Have a count of external
//   references. When > 0 - stable reference. When = 0 - weak reference.
class SpecialRefRegistry : private Pinned {
    using Mutex = SpinLock<MutexThreadStateHandling::kIgnore>;

    class Node : private Pinned {
    public:
        using Rc = int32_t;
        inline static constexpr Rc disposedMarker = std::numeric_limits<Rc>::min();
        static_assert(disposedMarker < 0, "disposedMarker must be an impossible Rc value");

        Node(ObjHeader* obj, Rc rc) noexcept : obj_(obj), rc_(rc) {
            RuntimeAssert(obj != nullptr, "Creating StableRef for null object");
            RuntimeAssert(rc >= 0, "Creating StableRef with negative rc %d", rc);
        }

        ~Node() {
            if (compiler::runtimeAssertsEnabled()) {
                auto rc = rc_.load(std::memory_order_relaxed);
                RuntimeAssert(rc == disposedMarker, "Deleting StableRef@%p with rc %d", this, rc);
            }
        }

        void dispose() noexcept {
            // It's possible for the rc to be > 0 here. Which can happen in Swift/ObjC
            // when retaining and autoreleasing self in deinit/dealloc. At the end,
            // releases will balance out. So, add a -lots here, and we will observe
            // exactly -lots in the destructor.
            auto rc = rc_.fetch_add(disposedMarker, std::memory_order_release);
            RuntimeAssert(rc >= 0, "Disposing StableRef@%p with rc %d", this, rc);
        }

        [[nodiscard("expensive pure function")]] ObjHeader* ref() const noexcept {
            AssertThreadState(ThreadState::kRunnable);
            // This can only be called when rc > 0 or if the object is in the roots
            // in some other way. rc = 0 is possible with virtual call Kt->ObjC->Kt.
            if (compiler::runtimeAssertsEnabled()) {
                auto rc = rc_.load(std::memory_order_relaxed);
                RuntimeAssert(rc >= 0, "Dereferencing StableRef@%p with rc %d", this, rc);
            }
            // So, GC could not have nulled out obj_.
            auto* obj = obj_.load(std::memory_order_relaxed);
            RuntimeAssert(obj != nullptr, "Dereferencing StableRef@%p with cleaned up object", this);
            return obj;
        }

        OBJ_GETTER0(tryRef) noexcept {
            AssertThreadState(ThreadState::kRunnable);
            // GC may have nulled out obj_. Use synchronized loading.
            // TODO: Weak reading barrier with CMS.
            RETURN_OBJ(obj_.load(std::memory_order_acquire));
        }

        void retainRef() noexcept {
            // promoteIntoRoots depends on rc being published before its execution.
            auto rc = rc_.fetch_add(1, std::memory_order_acq_rel);
            // Note: this can be negative if retaining after dispose. Possible with ObjC.
            if (rc == 0) {
                RuntimeAssert(
                        position_ == std_support::list<Node>::iterator{},
                        "Retaining StableRef@%p with fast deletion optimization is disallowed", this);
                // 0->1 changes require putting this node into the root set.
                SpecialRefRegistry::instance().promoteIntoRoots(*this);
            }
        }

        void releaseRef() noexcept {
            // Only need atomicity, no need to synchronize.
            rc_.fetch_sub(1, std::memory_order_relaxed);
        }

        RawSpecialRef* asRaw() noexcept { return reinterpret_cast<RawSpecialRef*>(this); }
        static Node* fromRaw(RawSpecialRef* ref) noexcept { return reinterpret_cast<Node*>(ref); }

    private:
        friend class SpecialRefRegistry;
        friend class SpecialRefRegistryTest;

        std::atomic<ObjHeader*> obj_ = nullptr;
        std::atomic<Rc> rc_ = 0; // When disposed can be disposedMarker.
        std::atomic<Node*> nextRoot_ = nullptr;
        // This and the next one only serve fast deletion optimization for shortly lived StableRefs.
        // TODO: Consider discarding this optimization completely.
        //       If we were to use custom allocator for these nodes as well they better
        //       be only deleted in the sweep anyway.
        //       Alternative: keep stable refs completely separate.
        void* owner_ = nullptr;
        std_support::list<Node>::iterator position_{};
    };

public:
    class ThreadQueue : private Pinned {
    public:
        explicit ThreadQueue(SpecialRefRegistry& registry) : owner_(registry) {}

        ~ThreadQueue() { publish(); }

        void publish() noexcept {
            for (auto& node : queue_) {
                // No need to synchronize. These two can only be updated in the runnable state.
                // TODO: If we were to remove this optimization, we could avoid scanning
                //       the whole queue here and just have the nodes inserted into the roots
                //       when they're created.
                node.owner_ = nullptr;
                node.position_ = std_support::list<Node>::iterator();
                // If the node was created with a positive refcount, we must ensure its put into
                // the roots.
                // promoteIntoRoots depends on rc being published before its execution.
                auto rc = node.rc_.load(std::memory_order_acquire);
                if (rc > 0) {
                    owner_.promoteIntoRoots(node);
                }
            }
            std::unique_lock guard(owner_.mutex_);
            RuntimeAssert(owner_.all_.get_allocator() == queue_.get_allocator(), "allocators must match");
            owner_.all_.splice(owner_.all_.end(), std::move(queue_));
        }

        void clearForTests() noexcept { queue_.clear(); }

        [[nodiscard("must be manually disposed")]] StableRef createStableRef(ObjHeader* object) noexcept;
        [[nodiscard("must be manually disposed")]] WeakRef createWeakRef(ObjHeader* object) noexcept;
        [[nodiscard("must be manually disposed")]] ObjCBackRef createObjCBackRef(ObjHeader* object) noexcept;

    private:
        friend class StableRef;
        friend class SpecialRefRegistryTest;

        [[nodiscard("must be manually disposed")]] Node& registerNode(ObjHeader* obj, Node::Rc rc, bool allowFastDeletion) noexcept {
            queue_.emplace_back(obj, rc);
            auto& node = queue_.back();
            if (allowFastDeletion) {
                node.owner_ = this;
                node.position_ = std::prev(queue_.end());
            }
            return node;
        }

        void deleteNodeIfLocal(Node& node) noexcept;

        SpecialRefRegistry& owner_;
        std_support::list<Node> queue_;
    };

    class RootsIterator {
    public:
        [[nodiscard("expensive pure function")]] ObjHeader* operator*() const noexcept {
            // Ignoring rc here. If someone nulls out rc during root
            // scanning, it's okay to be conservative and still make it a root.
            // Also, this happens on the GC thread, and only the GC thread can modify the object.
            return node_->obj_.load(std::memory_order_relaxed);
        }

        RootsIterator& operator++() noexcept {
            node_ = owner_->nextRoot(node_);
            return *this;
        }

        bool operator==(const RootsIterator& rhs) const noexcept { return node_ == rhs.node_; }

        bool operator!=(const RootsIterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        friend class SpecialRefRegistry;

        RootsIterator(SpecialRefRegistry& owner, Node* node) noexcept : owner_(&owner), node_(node) {}

        SpecialRefRegistry* owner_;
        Node* node_;
    };

    class RootsIterable : private MoveOnly {
    public:
        RootsIterator begin() const noexcept {
            // Protect against infinite iterations because of concurrent inserstions
            // right after rootsHead_.
            return RootsIterator(*owner_, owner_->nextRoot(owner_->rootsHead(), 1000));
        }

        RootsIterator end() const noexcept { return RootsIterator(*owner_, owner_->rootsTail()); }

    private:
        friend class SpecialRefRegistry;

        explicit RootsIterable(SpecialRefRegistry& owner) noexcept : owner_(&owner) {}

        raw_ptr<SpecialRefRegistry> owner_;
    };

    class Iterator {
    public:
        std::atomic<ObjHeader*>& operator*() noexcept { return iterator_->obj_; }

        Iterator& operator++() noexcept {
            iterator_ = owner_->findAliveNode(std::next(iterator_));
            return *this;
        }

        bool operator==(const Iterator& rhs) const noexcept { return iterator_ == rhs.iterator_; }

        bool operator!=(const Iterator& rhs) const noexcept { return iterator_ != rhs.iterator_; }

    private:
        friend class SpecialRefRegistry;
        friend class SpecialRefRegistryTest;

        Iterator(SpecialRefRegistry& owner, std_support::list<Node>::iterator iterator) noexcept : owner_(&owner), iterator_(iterator) {}

        SpecialRefRegistry* owner_;
        std_support::list<Node>::iterator iterator_;
    };

    class Iterable : private MoveOnly {
    public:
        Iterator begin() noexcept { return Iterator(owner_, owner_.findAliveNode(owner_.all_.begin())); }
        Iterator end() noexcept { return Iterator(owner_, owner_.all_.end()); }

    private:
        friend class SpecialRefRegistry;

        Iterable(SpecialRefRegistry& owner) noexcept : owner_(owner), guard_(owner_.mutex_) {}

        SpecialRefRegistry& owner_;
        std::unique_lock<Mutex> guard_;
    };

    SpecialRefRegistry() noexcept { rootsHead()->nextRoot_.store(rootsTail(), std::memory_order_relaxed); }

    ~SpecialRefRegistry() = default;

    static SpecialRefRegistry& instance() noexcept;

    void clearForTests() noexcept {
        rootsHead()->nextRoot_ = rootsTail();
        for (auto& node : all_) {
            // Allow the tests not to run the finalizers for weaks.
            node.rc_ = Node::disposedMarker;
        }
        all_.clear();
    }

    // Should be called on the GC thread after all threads have published.
    RootsIterable roots() noexcept { return RootsIterable(*this); }

    // Should be called on the GC thread after marking is complete.
    // Locks the registry and allows safe iteration over it.
    Iterable lockForIter() noexcept { return Iterable(*this); }

private:
    friend class ObjCBackRef;
    friend class StableRef;
    friend class WeakRef;
    friend class SpecialRefRegistryTest;

    Node* nextRoot(Node* current, int maxIterations = std::numeric_limits<int>::max()) noexcept;
    void promoteIntoRoots(Node& node) noexcept;
    std_support::list<Node>::iterator findAliveNode(std_support::list<Node>::iterator it) noexcept;

    Node* rootsHead() noexcept { return reinterpret_cast<Node*>(rootsHeadStorage_); }
    const Node* rootsHead() const noexcept { return reinterpret_cast<const Node*>(rootsHeadStorage_); }
    static Node* rootsTail() noexcept { return reinterpret_cast<Node*>(rootsTailStorage_); }

    // TODO: Iteration over `all_` will be slow, because it's `std_support::list`
    //       collected at different times from different threads, and so the nodes
    //       are all over the memory. Consider using custom allocator for that.
    std_support::list<Node> all_;
    Mutex mutex_;
    alignas(Node) char rootsHeadStorage_[sizeof(Node)] = {0};
    alignas(Node) static inline char rootsTailStorage_[sizeof(Node)] = {0};
};

} // namespace kotlin::mm
