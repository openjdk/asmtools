/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.util.I18NResourceBundle;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

/**
 * TODO: Replacement for Environment that will replace it.
 */
public abstract class Environment<T extends ToolLogger> implements ILogger {

    // Results
    public final static int OK     = 0;
    public final static int FAILED = 1;

    T toolLogger;

    // processed input file
    private String inputFileName;
    // checks output verbosity
    private boolean verboseFlag;

    private boolean traceFlag;
    private boolean ignoreWarnings;         // do not print / ignore warnings
    private boolean strictWarnings;         // consider warnings as errors


    protected Environment(Builder builder, I18NResourceBundle i18n) {
        ToolLogger.setResources(builder.programName, i18n);
        this.toolLogger = (T) builder.toolLogger;
    }

    public void setInputFile(String inputFileName) throws IOException, URISyntaxException {
        this.inputFileName = inputFileName;
        toolLogger.setInputFileName(inputFileName);
    }

    public void setTraceFlag(boolean traceFlag) {
        this.traceFlag = traceFlag;
    }

    public void setIgnoreWarnings(boolean ignoreWarnings) {
        this.ignoreWarnings = ignoreWarnings;
    }

    public void setStrictWarnings(boolean strictWarnings) {
        this.strictWarnings = strictWarnings;
    }

    public String getSimpleInputFileName() { return toolLogger.getSimpleInputFileName(); }

    public String getInputFileName() { return inputFileName; }

    /**
     * @return DataInputStream or null if the method can't read a file
     */
    protected DataInputStream getDataInputStream() throws URISyntaxException, IOException {
        Objects.requireNonNull(this.inputFileName, "Input file name should be defined.");
        try {
            return new DataInputStream(new FileInputStream(this.inputFileName));
        } catch (IOException ex) {
            if (this.inputFileName.matches("^[A-Za-z]+:.*")) {
                try {
                    final URI uri = new URI(this.inputFileName);
                    final URL url = uri.toURL();
                    final URLConnection conn = url.openConnection();
                    conn.setUseCaches(false);
                    return new DataInputStream(conn.getInputStream());
                } catch (URISyntaxException | IOException exception) {
                    error("err.cannot.read", this.inputFileName);
                    throw exception;
                }
            } else {
                throw ex;
            }
        }
    }

    @Override
    public void traceln(String id, Object... args) {
        if( traceFlag )
            ILogger.super.traceln(id,args);
    }

    @Override
    public void trace(String id, Object... args) {
        if( traceFlag )
            ILogger.super.trace(id,args);
    }

    @Override
    public void error(String id, Object... args) {
        toolLogger.error(id, args);
    }

    @Override
    public void info(String id, Object... args) {
        toolLogger.info(id, args);
    }

    @Override
    public void printException(Throwable throwable) {
        if (verboseFlag)
            toolLogger.printException(throwable);
    }

    @Override
    public PrintWriter getErrLog() {
        return toolLogger.getErrLog();
    }

    @Override
    public PrintWriter getOutLog() {
        return toolLogger.getOutLog();
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
        public PrintWriter toolOutput;
        protected String programName;

        public Builder(String programName, PrintWriter toolOutput, T toolLogger) {
            this.programName = programName;
            this.toolOutput = toolOutput;
            this.toolLogger = toolLogger;
        }

        public  Builder(String programName, T toolLogger) {
            this.programName = programName;
            this.toolLogger = toolLogger;
        }

        /**
         * @return new environment
         */
        abstract public E build();
    }
} // end Environment
