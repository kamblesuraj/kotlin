// FIR_IDENTICAL
// FULL_JDK
// FIR_DUMP
// FILE: Params.java

import java.util.Map;

public interface Params {
    Map<String, String> getEnvs();

    void setEnvs(Map<String, String> envs);
}

// FILE: AbstractParams.java

public interface AbstractParams extends Params {}

// FILE: AbstractConfiguration.java

import org.jetbrains.annotations.NotNull;
import java.util.Map;

public abstract class AbstractConfiguration {
    @NotNull
    public Map<String, String> getEnvs() {
        return null;
    }

    public void setEnvs(@NotNull final Map<String, String> envs) {
    }
}

// FILE: AbstractConfigurationWithParams.java

public class AbstractConfigurationWithParams extends AbstractConfiguration implements AbstractParams {}

// FILE: ConcreteConfiguration.java

public class ConcreteConfiguration extends AbstractConfigurationWithParams implements AbstractParams {}

// FILE: test.kt

class Factory {
    fun <T> foo(config: ConcreteConfiguration, bar: (T) -> T) {
        val envs = config.envs
        val consoleEnvs = mutableMapOf<String, String>()
        consoleEnvs.putAll(envs)
        for ((key, value) in envs) {
            println("${key.bar()} = ${value.bar()}")
        }
    }
}

fun String.bar() = ""

fun Int.bar() = ""
