package org.openjdk.asmtools.common.outputs.log;

import org.openjdk.asmtools.common.outputs.ToolOutput;

public class StringLog extends NamedDualStreamToolOutput {

    private final StringBuilder log = new StringBuilder();

    @Override
    public String toString() {
        return log.toString();
    }

    @Override
    public void printlns(String line) {
        log.append(line).append("\n");
    }

    @Override
    public void prints(String line) {
        log.append(line);
    }

    @Override
    public void prints(char line) {
        log.append(line);
    }

    @Override
    public void flush() {

    }

    @Override
    public void printlne(String line) {
        log.append(line).append("\n");
    }

    @Override
    public void printe(String line) {
        log.append(line);
    }

    @Override
    public void printe(char line) {
        log.append(line);
    }

    @Override
    public void stacktrace(Throwable ex) {
        log.append(ToolOutput.exToString(ex));
    }

    @Override
    public ToolOutput getSToolObject() {
        return this;
    }

    @Override
    public ToolOutput getEToolObject() {
        return this;
    }
}
