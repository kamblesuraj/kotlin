/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SpecialRefRegistry.hpp"

#include "GlobalData.hpp"
#include "MemoryPrivate.hpp"
#include "ObjCBackRef.hpp"
#include "StableRef.hpp"
#include "ThreadData.hpp"
#include "ThreadState.hpp"
#include "WeakRef.hpp"

using namespace kotlin;

mm::StableRef mm::SpecialRefRegistry::ThreadQueue::createStableRef(ObjHeader* object) noexcept {
    return mm::StableRef(registerNode(object, 1, true).asRaw());
}

mm::WeakRef mm::SpecialRefRegistry::ThreadQueue::createWeakRef(ObjHeader* object) noexcept {
    return mm::WeakRef(registerNode(object, 0, false).asRaw());
}

mm::ObjCBackRef mm::SpecialRefRegistry::ThreadQueue::createObjCBackRef(ObjHeader* object) noexcept {
    return mm::ObjCBackRef(registerNode(object, 1, false).asRaw());
}

void mm::SpecialRefRegistry::ThreadQueue::deleteNodeIfLocal(Node& node) noexcept {
    // This is a very weird optimization.
    // * We're saving some time during root scanning and some memory by
    //   deleting some short-lived nodes without ever publishing them.
    // * But in order to do that we have to be in a runnable state, so
    //   we potentially force a native state thread to go wait for the GC.
    if (node.owner_ == this) {
        queue_.erase(node.position_);
    }
}

// static
mm::SpecialRefRegistry& mm::SpecialRefRegistry::instance() noexcept {
    return GlobalData::Instance().specialRefRegistry();
}

mm::SpecialRefRegistry::Node* mm::SpecialRefRegistry::nextRoot(Node* current, int maxIterations) noexcept {
    for (int i = 0; i < maxIterations; ++i) {
        RuntimeAssert(current != nullptr, "current cannot be null");
        RuntimeAssert(current != rootsTail(), "current cannot be tail");
        auto candidate = current->nextRoot_.load(std::memory_order_acquire);
        RuntimeAssert(candidate != nullptr, "candidate cannot be null");
        if (candidate == rootsTail() || candidate->rc_.load(std::memory_order_acquire) > 0) {
            // Perfectly good node. Stop right there.
            // If someone concurrently inserts something into the head, it's fine:
            // * promoteIntoRoots will make sure to mark that node.
            // * On the next GC iteration we will definitely go through it.
            return candidate;
        }
        // Bad node. Let's remove it from the roots.
        while (true) {
            RuntimeAssert(current != nullptr, "current cannot be null");
            RuntimeAssert(current != rootsTail(), "current cannot be tail");
            RuntimeAssert(candidate != nullptr, "candidate cannot be null");
            RuntimeAssert(candidate != rootsTail(), "candidate cannot be tail");
            // Racy if someone concurrently inserts in the middle. Or iterates.
            // But we don't have that here. Inserts are only in the beginning.
            // Iteration also happens only here.
            auto next = candidate->nextRoot_.load(std::memory_order_acquire);
            RuntimeAssert(next != nullptr, "candidate's next cannot be null");
            auto actualNext = candidate;
            bool deleted = current->nextRoot_.compare_exchange_strong(actualNext, next, std::memory_order_acq_rel);
            RuntimeAssert(actualNext != nullptr, "current's next cannot be null");
            if (deleted) {
                candidate->nextRoot_.store(nullptr, std::memory_order_release);
                break;
            }
            // Someone inserted between current and candidate.
            // Move current forward, and try deleting again.
            // Anything inserted into the head is fine:
            // * promoteIntoRoots will make sure to mark that node.
            // * On the next GC iteration we will definitely go through it.
            // Additionally, by moving current forward we make sure that this
            // is never an infinite loop because all concurrent insertions
            // happen into the head.
            current = actualNext;
        }
        // We removed candidate. But should we have?
        if (candidate->rc_.load(std::memory_order_acquire) > 0) {
            // Ooops. Let's put it back. Okay to put into the head.
            promoteIntoRoots(*candidate);
        }
        // Okay, properly deleted. Let's look at the next node.
    }
    // Too many iterations. Conservatively making next of current the root.
    // It's okay: either null, or we just keep some object alive longer than
    // absolutely needed.
    return current->nextRoot_.load(std::memory_order_acquire);
}

void mm::SpecialRefRegistry::promoteIntoRoots(Node& node) noexcept {
    auto* obj = node.obj_.load(std::memory_order_relaxed);
    if (!obj) {
        // If the object is cleared, do nothing as an optimization.
        return;
    }
    // TODO: With CMS barrier for marking `obj` should be here.
    // This must be read only after rc is published, acquire is required.
    if (node.nextRoot_.load(std::memory_order_acquire) != nullptr) {
        // The node is already a root (or someone is making it one).
        // Do not touch it.
        // nextRoot might be demoting it concurrently, though. But it'll
        // recheck the rc afterwards and will re-promote the node if needed.
        return;
    }

    Node* next = rootsHead()->nextRoot_.load(std::memory_order_acquire);
    do {
        RuntimeAssert(next != nullptr, "head's next cannot be null");
        node.nextRoot_.store(next, std::memory_order_release);
    } while (!rootsHead()->nextRoot_.compare_exchange_weak(next, &node, std::memory_order_acq_rel));
}

std_support::list<mm::SpecialRefRegistry::Node>::iterator mm::SpecialRefRegistry::findAliveNode(
        std_support::list<Node>::iterator it) noexcept {
    while (it != all_.end() && it->rc_.load(std::memory_order_acquire) == Node::disposedMarker) {
        // Removing disposed nodes.
        if (it->nextRoot_.load(std::memory_order_relaxed) != nullptr) {
            // Wait, it's in the roots list. Lets wait until the next GC
            // for it to get cleaned up from there.
            ++it;
            continue;
        }
        // Actually deleting.
        it = all_.erase(it);
    }
    return it;
}
