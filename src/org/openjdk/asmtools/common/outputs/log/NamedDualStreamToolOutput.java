package org.openjdk.asmtools.common.outputs.log;

import org.openjdk.asmtools.common.Environment;

import java.io.IOException;
import java.util.Optional;

public abstract class NamedDualStreamToolOutput implements DualStreamToolOutput {
    private String fqn;
    private Optional<String> suffix;
    private Environment environment;

    @Override
    public String getCurrentClassName() {
        return fqn;
    }

    @Override
    public void startClass(String fqn, Optional<String> suffix, Environment logger) throws IOException {
        this.fqn = fqn;
        this.suffix = suffix;
        this.environment = logger;
    }

    @Override
    public void finishClass(String fqn) throws IOException {
        this.fqn = null;
    }
}
