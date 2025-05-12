/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.asmtools.common;

import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.ToolOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Environment<T extends ToolLogger> implements ILogger {

    // Results
    public final static int OK = 0;
    public final static int FAILED = 1;

    T toolLogger;

    //-w <directory> Specify where to place generated class files, without considering the classpath.
    // If not specified, output will be directed to <stdout>.
    private boolean ignorePackage = false;

    // processed input file or stdin
    private ToolInput toolInput;
    private ToolOutput toolOutput;
    // checks output verbosity
    private boolean verboseFlag;

    public boolean isTraceFlag() {
        return traceFlag;
    }

    private boolean traceFlag;

    /**
     * @param builder the environment builder
     */
    protected Environment(Builder builder) {
        this.toolOutput = builder.toolOutput;
        this.toolLogger = (T) builder.toolLogger;
    }

    public void setToolInput(ToolInput toolInput) throws IOException, URISyntaxException {
        this.toolInput = toolInput;
        toolLogger.setInputFileName(toolInput);
    }

    public void setTraceFlag(boolean flag) {
        this.traceFlag = flag;
    }

    public void setIgnoreWarningsOn() {
        toolLogger.ignoreWarnings = true;
    }

    public void setStrictWarningsOn() {
        toolLogger.strictWarnings = true;
    }


    public boolean isIgnorePackage() {
        return ignorePackage;
    }

    public Environment<T> setIgnorePackage(boolean ignorePackage) {
        this.ignorePackage = ignorePackage;
        return this;
    }

    public String getSimpleInputFileName() {
        return toolLogger.getSimpleInputFileName();
    }

    /**
     * Returns the name of the source file that is used by the tool to assemble the SourceFile attribute
     *
     * @return the name of the source file
     */
    public String getSourceName() {
        String sourceFileName = getSimpleInputFileName();
        String sourceName = sourceFileName.contains(".") ?
                sourceFileName.substring(0, sourceFileName.indexOf('.')) :
                sourceFileName;
        return sourceName;
    }

    public ToolInput getToolInput() {
        return toolInput;
    }

    /**
     * @return DataInputStream or null if the method can't read a file
     */
    protected DataInputStream getDataInputStream() throws URISyntaxException, IOException {
        Objects.requireNonNull(this.toolInput, "Input must be defined.");
        return toolInput.getDataInputStream(Optional.of(this));
    }

    @Override
    public void traceln(String id, Object... args) {
        if (traceFlag)
            ILogger.super.traceln(id, args);
    }

    public void traceln(Supplier<String> supplier) {
        if (traceFlag) {
            ILogger.super.traceln(supplier.get());
        }
    }

    @Override
    public void trace(String id, Object... args) {
        if (traceFlag)
            ILogger.super.trace(id, args);
    }

    @Override
    public void error(String id, Object... args) {
        toolLogger.error(id, args);
    }

    @Override
    public void error(Throwable exception) {
        toolLogger.error(exception);
    }

    public void trace(Supplier<String> supplier) {
        if (traceFlag) {
            ILogger.super.trace(supplier.get());
        }
    }

    @Override
    public void info(String id, Object... args) {
        toolLogger.info(id, args);
    }

    public String getInfo(String id, Object... args) {
        return toolLogger.getInfo(id, args);
    }

    public void usage(List<String> ids) {
        toolLogger.usage(ids);
    }

    public void usage(List<String> ids, Function<String, String> func) {
        toolLogger.usage(ids, func);
    }

    @Override
    public void printException(Throwable throwable) {
        if (verboseFlag)
            toolLogger.printException(throwable);
    }

    @Override
    public DualStreamToolOutput getOutputs() {
        return getLogger().getOutputs();
    }

    @Override
    public void setOutputs(DualStreamToolOutput nw) {
        getLogger().setOutputs(nw);
    }

    @Override
    public ToolOutput getToolOutput() {
        return toolOutput;
    }

    @Override
    public void setToolOutput(ToolOutput toolOutput) {
        this.toolOutput = toolOutput;
    }

    public boolean getVerboseFlag() {
        return verboseFlag;
    }

    public Environment setVerboseFlag(boolean value) {
        this.verboseFlag = value;
        return this;
    }

    public T getLogger() {
        return (T) toolLogger;
    }

    /**
     * The Environment builder.
     */
    public abstract static class Builder<E extends Environment, T extends ToolLogger> {
        T toolLogger;
        public ToolOutput toolOutput;

        public Builder(ToolOutput toolOutput, T toolLogger) {
            this.toolOutput = toolOutput;
            this.toolLogger = toolLogger;
        }

        /**
         * @return new environment
         */
        abstract public E build();
    }
} // end Environment
