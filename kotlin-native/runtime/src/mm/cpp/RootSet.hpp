/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_ROOT_SET_H
#define RUNTIME_MM_ROOT_SET_H

#include "ForeignRefRegistry.hpp"
#include "GlobalsRegistry.hpp"
#include "ShadowStack.hpp"
#include "StableRefRegistry.hpp"
#include "ThreadLocalStorage.hpp"

struct ObjHeader;

namespace kotlin {
namespace mm {

class ThreadData;

class ThreadRootSet {
public:
    enum class Source {
        kStack,
        kTLS,
    };

    struct Value {
        ObjHeader*& object;
        Source source;

        bool operator==(const Value& rhs) const noexcept { return object == rhs.object && source == rhs.source; }
    };

    class Iterator {
    public:
        struct begin_t {};
        static constexpr inline begin_t begin = begin_t{};

        struct end_t {};
        static constexpr inline end_t end = end_t{};

        Iterator(begin_t, ThreadRootSet& owner) noexcept;
        Iterator(end_t, ThreadRootSet& owner) noexcept;

        Value operator*() noexcept;

        Iterator& operator++() noexcept;

        bool operator==(const Iterator& rhs) const noexcept;
        bool operator!=(const Iterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        enum class Phase {
            kStack,
            kTLS,
            kDone,
        };

        void Init() noexcept;

        ThreadRootSet& owner_;
        Phase phase_;
        union {
            ShadowStack::Iterator stackIterator_;
            ThreadLocalStorage::Iterator tlsIterator_;
        };
    };

    ThreadRootSet(ShadowStack& stack, ThreadLocalStorage& tls) noexcept : stack_(stack), tls_(tls) {}
    explicit ThreadRootSet(ThreadData& threadData) noexcept;

    Iterator begin() noexcept { return Iterator(Iterator::begin, *this); }
    Iterator end() noexcept { return Iterator(Iterator::end, *this); }

private:
    ShadowStack& stack_;
    ThreadLocalStorage& tls_;
};

class GlobalRootSet {
public:
    enum class Source {
        kGlobal,
        kStableRef,
        kForeignRef,
    };

    struct Value {
        ObjHeader*& object;
        Source source;

        bool operator==(const Value& rhs) const noexcept { return object == rhs.object && source == rhs.source; }
    };

    class Iterator {
    public:
        struct begin_t {};
        static constexpr inline begin_t begin = begin_t{};

        struct end_t {};
        static constexpr inline end_t end = end_t{};

        Iterator(begin_t, GlobalRootSet& owner) noexcept;
        Iterator(end_t, GlobalRootSet& owner) noexcept;

        Value operator*() noexcept;

        Iterator& operator++() noexcept;

        bool operator==(const Iterator& rhs) const noexcept;
        bool operator!=(const Iterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        enum class Phase {
            kGlobals,
            kStableRefs,
            kForeignRefs,
            kDone,
        };

        void Init() noexcept;

        GlobalRootSet& owner_;
        Phase phase_;
        union {
            GlobalsRegistry::Iterator globalsIterator_;
            StableRefRegistry::Iterator stableRefsIterator_;
            ForeignRefRegistry::RootsIterator foreignRefsIterator_;
        };
    };

    GlobalRootSet(GlobalsRegistry& globalsRegistry, StableRefRegistry& stableRefRegistry, ForeignRefRegistry& foreignRefRegistry) noexcept :
        globalsIterable_(globalsRegistry.LockForIter()), stableRefsIterable_(stableRefRegistry.LockForIter()), foreignRefsIterable_(foreignRefRegistry.iterateOverRoots()) {}
    GlobalRootSet() noexcept;

    Iterator begin() noexcept { return Iterator(Iterator::begin, *this); }
    Iterator end() noexcept { return Iterator(Iterator::end, *this); }

private:
    // TODO: These use separate locks, which is inefficient, and slightly dangerous. In practice it's
    //       fine, because this is the only place where these two locks are taken simultaneously.
    GlobalsRegistry::Iterable globalsIterable_;
    StableRefRegistry::Iterable stableRefsIterable_;
    ForeignRefRegistry::RootsIterable foreignRefsIterable_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_ROOT_SET_H
