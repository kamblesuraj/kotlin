/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Memory.h"
#include "Utils.hpp"

namespace kotlin::mm {

OBJ_GETTER(createWeakReferenceCounter, ObjHeader* object) noexcept;
void disposeWeakReferenceCounter(ObjHeader* counter) noexcept;

OBJ_GETTER(derefWeakReferenceCounter, ObjHeader* counter) noexcept;
ObjHeader* weakReferenceCounterBaseObjectUnsafe(ObjHeader* counter) noexcept;

} // namespace kotlin::mm
