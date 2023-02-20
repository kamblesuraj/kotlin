/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ForeignRefRegistry.hpp"

#include "GlobalData.hpp"

using namespace kotlin;

// static
mm::ForeignRefRegistry& mm::ForeignRefRegistry::instance() noexcept {
    return mm::GlobalData::Instance().foreignRefRegistry();
}

void mm::ForeignRefRegistry::promoteToRoots(Record& record) noexcept {
    // TODO: Make sure to mark the record with CMS.
    if (record.next_.load(std::memory_order_acquire)) {
        // The record is already in the roots (or someone else is trying to insert it).
        // Don't touch it.
        // nextRoot might be demoting it concurrently, though. But in that case
        // it'll recheck the rc after doing so and will re-promote the
        // record itself.
        return;
    }

    auto* next = rootsHead_.next_.load(std::memory_order_acquire);
    do {
        // Synchronized by insertion into the queue itself.
        record.next_.store(next, std::memory_order_relaxed);
    } while(!rootsHead_.next_.compare_exchange_weak(next, &record, std::memory_order_acq_rel));
}

mm::ForeignRefRegistry::Record* mm::ForeignRefRegistry::nextRoot(Record* current, int maxIterations) noexcept {
    konan::consoleErrorf("nextRoot head=%p tail=%p\n", &rootsHead_, &rootsTail_);
    for (int i = 0; i < maxIterations; ++i) {
        konan::consoleErrorf("nextRoot current=%p i=%d\n", current, i);
        RuntimeAssert(current != &rootsTail_, "Trying to increment past the end");
        auto candidate = current->next_.load(std::memory_order_acquire);
        konan::consoleErrorf("nextRoot candidadate=%p\n", candidate);
        if (candidate == &rootsTail_ || candidate->needsToBeRoot()) {
            // Perfectly good node. Stop right there.
            // It's fine if anyone inserted something between prev and current:
            // * promoteToRoots will make sure to mark that record.
            // * And on the next GC iteration it'll be properly processed, because
            //   deletion only happens at this stage.
            konan::consoleErrorf("nextRoot candidadate good\n");
            return candidate;
        }
        konan::consoleErrorf("nextRoot candidadate bad\n");
        // Okay, let's delete the candidate
        while (true) {
            RuntimeAssert(current != &rootsTail_, "Deleting candidate has moved us to the end");
            // Racy if someone concurrently inserts in the middle. Or iterates.
            // But we don't have that here. Inserts are only in the beginning.
            // Iteration also happens only here.
            auto next = candidate->next_.load(std::memory_order_acquire);
            auto actualNext = candidate;
            bool deleted = current->next_.compare_exchange_strong(actualNext, next, std::memory_order_acq_rel);
            if (deleted) {
                candidate->next_.store(nullptr, std::memory_order_release);
                break;
            }
            // Someone inserted between current and candidate.
            // Move current forward, and try deleting again.
            current = actualNext;
        }
        konan::consoleErrorf("nextRoot candidadate purged\n");
        // Candidate is deleted. But should it have been?
        if (candidate->needsToBeRoot()) {
            konan::consoleErrorf("nextRoot candidadate good actually\n");
            // Oops. Let's put it back. In the head of the list is okay.
            promoteToRoots(*candidate);
        }
        // Let's loop around and look at current's new head.
    }
    // Too many iterations. Give up. It's okay for the root to be nullptr.
    return current->next_.load(std::memory_order_acquire);
}
