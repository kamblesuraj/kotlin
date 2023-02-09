// JDK_KIND: FULL_JDK_11

import java.lang.invoke.VarHandle

class Some {
    fun foo(handle: VarHandle) {
        handle.set(this, false)
    }
}
