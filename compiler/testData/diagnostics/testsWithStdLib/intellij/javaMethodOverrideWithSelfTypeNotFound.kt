// FILE: JavaBreakpointType.java

import org.jetbrains.annotations.NotNull;

public interface JavaBreakpointType<P> {
    @NotNull
    Breakpoint<P> createJavaBreakpoint(Breakpoint<P> breakpoint);
}

// FILE: Breakpoint.java

public abstract class Breakpoint<P> {}

// FILE: JavaMethodBreakpointProperties.java

public class JavaMethodBreakpointProperties {}

// FILE: JavaLineBreakpointTypeBase.java

public abstract class JavaLineBreakpointTypeBase<P> implements JavaBreakpointType<P> {}

// FILE: JavaMethodBreakpointType.java

import org.jetbrains.annotations.NotNull;

public class JavaMethodBreakpointType extends JavaLineBreakpointTypeBase<JavaMethodBreakpointProperties> {
    @NotNull
    @Override
    public Breakpoint<JavaMethodBreakpointProperties> createJavaBreakpoint(Breakpoint breakpoint) {
        return null;
    }
}

// FILE: MethodBreakpoint.java

public class MethodBreakpoint extends Breakpoint<JavaMethodBreakpointProperties> {}

// FILE: KotlinFunctionBreakpointType.kt

class KotlinFunctionBreakpointType : JavaMethodBreakpointType() {
    override fun createJavaBreakpoint(breakpoint: Breakpoint<JavaMethodBreakpointProperties>) = MethodBreakpoint()
}
