/*
 * Copyright (c) 2023, 2025, Oracle, Red Hat  and/or theirs affiliates. All rights reserved.
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
package org.openjdk.asmtools.common.outputs;

import org.openjdk.asmtools.common.Environment;

import java.io.IOException;
import java.util.Optional;

/**
 * Historically, the output loggers for compilers had two stderrs, one to stdout and second to stderr.
 * That should be removed, in favour of just dual stream tool output, printing output to stdout and log into stderr
 */
public abstract class NamedToolOutput implements ToolOutput {

    protected String fullyQualifiedName = "";
    /**
     * If the output is a file, then the destinationFileName is used to form the filename of the output.
     * 1. File FILENAME or class file CLASSNAME takes the highest priority. This filename cannot be overridden.
     * 2. Public class CLASSNAME { }– class name is CLASSNAME, and this CLASSNAME will be used to generate the filename (i.e., CLASSNAME.class).
     * 3. this_class – The filename will be CLASSNAME.class, but the class name will be this_class.
     * <p>
     * Also, if the -f option is used and the number of processed class files is more than 1,
     * then destinationFileName will only apply to the first input file only.
     * For example, if you run jdis -w . -f FILE input.class input2.class, then in the output directory,
     * the files FILE.jasm and input2.jasm will be generated.
     */
    protected String destinationFileName = null;
    private Optional<String> suffix;
    private Environment environment;

    @Override
    public String getCurrentClassName() {
        return fullyQualifiedName;
    }

    @Override
    public void startClass(String fullyQualifiedName, Optional<String> suffix, Environment logger) throws IOException {
        this.fullyQualifiedName = fullyQualifiedName;
        this.suffix = suffix;
        this.environment = logger;
    }

    @Override
    public void finishClass(String fullyQualifiedName) throws IOException {
        this.destinationFileName = null;
        this.fullyQualifiedName = null;
    }

    public String getDestinationFileName() {
        return destinationFileName;
    }

    public NamedToolOutput setDestinationFileName(String destinationFileName) {
        this.destinationFileName = destinationFileName;
        return this;
    }

    public abstract String getName();

}
