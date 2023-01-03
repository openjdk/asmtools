package org.openjdk.asmtools.common.outputs;

import org.openjdk.asmtools.common.Environment;

import java.io.IOException;
import java.util.Optional;

/**
 * Historically, the output loggers for compilers had two stderrs, one to sdout and secon to stderr.
 * That should be removed, in favour of just dualstream tool output, printing output to stdout and log into stderr
 */
public abstract class NamedToolOutput implements ToolOutput {
    protected String fqn;
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
