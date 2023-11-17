/*
 * Copyright (c) 2009, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jcoder;

import org.openjdk.asmtools.asmutils.Pair;
import org.openjdk.asmtools.common.inputs.FileInput;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.PrintWriterOutput;
import org.openjdk.asmtools.common.outputs.StdoutOutput;
import org.openjdk.asmtools.common.outputs.ToolOutput;
import org.openjdk.asmtools.common.outputs.log.DualOutputStreamOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import static org.openjdk.asmtools.common.Environment.FAILED;
import static org.openjdk.asmtools.common.Environment.OK;
import static org.openjdk.asmtools.util.ProductInfo.FULL_VERSION;

/**
 * Jcoder is an assembler that accepts a text file based on the JCod Specification,
 * and produces a .class file for use with a Java Virtual Machine.
 * <p>
 * Main entry point of the JCoder assembler :: jcod to class
 */
public class Main extends JcoderTool {


    HashMap<String, String> macros = new HashMap<>(1);
    // tool options
    private boolean noWriteFlag = false;        // Do not write generated class files
    private boolean ignoreFlag = false;         // Ignore non-fatal error(s) that suppress writing class files

    public Main(ToolOutput toolOutput, String... argv) {
        super(toolOutput);
        parseArgs(argv);
    }

    public Main(ToolOutput toolOutput, DualStreamToolOutput log, String... argv) {
        this(toolOutput, log, null, argv);
    }

    public Main(ToolOutput toolOutput, DualStreamToolOutput log, ToolInput toolInput, String... argv) {
        super(toolOutput, log);
        if (toolInput != null) {
            fileList.add(toolInput);
        }
        parseArgs(argv);
    }

    public Main(ToolOutput toolOutput, DualStreamToolOutput log, ToolInput... toolInputs) {
        super(toolOutput, log);
        Collections.addAll(fileList, toolInputs);
    }

    public Main(ToolOutput toolOutput, DualStreamToolOutput log, ToolInput toolInput) {
        super(toolOutput, log);
        fileList.add(toolInput);
    }

    /**
     * Deprecated method to support external tools having it
     *
     * @param ref      A stream to which to write reference output
     * @param toolName the tool's name (ignored)
     */
    @Deprecated
    public Main(PrintWriter ref, String toolName) {
        super(new PrintWriterOutput(ref));
    }

    /**
     * Deprecated method to support external tools having it
     *
     * @param out      A stream to which to write reference output
     * @param toolName the tool's name (ignored)
     */
    @Deprecated
    public Main(PrintStream out, String toolName) {
        this(new PrintWriter(out), toolName);
    }

    // jcoder entry point
    public static void main(String... argv) {
        Main compiler = new Main(new StdoutOutput(), argv);
        System.exit(compiler.compile());
    }

    @Override
    public void usage() {
        environment.flush(false);
        environment.usage(List.of(
                "info.usage",
                "info.opt.d",
                "info.opt.nowrite",
                "info.opt.ignore",
                "info.opt.fixcv",
                "info.opt.fixcv.full",
                "info.opt.t",
                "info.opt.v",
                "info.opt.version"
        ));
    }

    // Run jcoder compiler with args
    public synchronized boolean compile(String... argv) {
        parseArgs(argv);
        return this.compile() == OK;
    }

    // Run jcoder compiler when args already parsed
    public synchronized int compile() {
        macros.put("VERSION", "3;45");
        // compile all input files
        int rc = OK;
        try {
            for (ToolInput inputFileName : fileList) {
                environment.setInputFile(inputFileName);
                Jcoder parser = new Jcoder(environment, macros);
                parser.parseFile();
                if (noWriteFlag || (environment.getErrorCount() > 0 && !ignoreFlag)) {
                    continue;
                }
                parser.write();
                if (environment.hasMessages()) rc += environment.flush(true);
            }
        } catch (IOException | URISyntaxException | Error exception) {
            environment.printException(exception);
        } catch (Throwable exception) {
            // all untrapped exception/errors that escaped CompilerLogger
            environment.printException(exception);
            environment.error(exception);
        }
        if (environment.hasMessages()) rc += environment.flush(true);
        return rc;
    }

    @Override
    protected void parseArgs(String... argv) {
        try {
            // Parse arguments
            for (int i = 0; i < argv.length; i++) {
                String arg = argv[i];
                switch (arg) {
                    // public options
                    case "-v" -> setVerboseFlag(true);
                    case "-t" -> {
                        setVerboseFlag(true);
                        setTraceFlag(true);
                    }
                    case org.openjdk.asmtools.Main.DIR_SWITCH -> setDestDir(++i, argv);
                    case org.openjdk.asmtools.Main.DUAL_LOG_SWITCH ->
                            environment.setOutputs(new DualOutputStreamOutput());
                    case "-m" -> {
                        if ((i + 1) >= argv.length) {
                            environment.error("err.m_requires_macro");
                            usage();
                            throw new IllegalArgumentException();
                        }
                        try {
                            String[] macroPair = argv[++i].split("[.:]+", 2);
                            if (macroPair.length == 2) {
                                macros.put(macroPair[0], macroPair[1]);
                            } else {
                                throw new NumberFormatException();
                            }
                        } catch (PatternSyntaxException | NumberFormatException exception) {
                            environment.error("err.invalid_macro");
                            usage();
                            throw new IllegalArgumentException();
                        }
                    }
                    case "-nowrite" -> noWriteFlag = true;
                    case "-ignore" -> ignoreFlag = true;
                    case org.openjdk.asmtools.Main.VERSION_SWITCH -> {
                        environment.println(FULL_VERSION);
                        System.exit(OK);
                    }
                    case "-h", "-help", "-?" -> {
                        usage();
                        System.exit((OK));
                    }
                    case org.openjdk.asmtools.Main.STDIN_SWITCH -> {
                        addStdIn();
                    }
                    // overrides cf version even if it's defined in the source file.
                    case "-fixcv" -> {
                        if ((i + 1) >= argv.length) {
                            environment.error("err.fix_cv_requires_arg");
                            usage();
                            throw new IllegalArgumentException();
                        }
                        String cfvArg = argv[++i];
                        boolean withThreshold = cfvArg.contains("-");
                        try {
                            if (withThreshold) {
                                String[] versions = cfvArg.split("-", 2);
                                String[] versionsThreshold = versions[0].split("[.:]+", 2);
                                String[] versionsUpdate = versions[1].split("[.:]+", 2);
                                if (versionsThreshold.length != 2 || versionsUpdate.length != 2) {
                                    throw new NumberFormatException();
                                }
                                Pair<Integer, Integer> versionsPair = new Pair<>(Integer.parseInt(versionsThreshold[0]),
                                        Integer.parseInt(versionsThreshold[1]));
                                if( versionsPair.second > 0xFFFF || versionsPair.first > 0xFFFF ) {
                                    throw new NumberFormatException();
                                }
                                environment.cfv.setThreshold(versionsPair.first, versionsPair.second);
                                versionsPair = new Pair<>(Integer.parseInt(versionsUpdate[0]), Integer.parseInt(versionsUpdate[1]));
                                if( versionsPair.second > 0xFFFF || versionsPair.first > 0xFFFF ) {
                                    throw new NumberFormatException();
                                }
                                environment.cfv.setVersion(versionsPair.first, versionsPair.second).setByParameter(true).setFrozen(true);
                            } else {
                                String[] versions = cfvArg.split("[.:]+", 2);
                                if (versions.length == 2) {
                                    Pair<Integer, Integer> versionsPair = new Pair<>(Integer.parseInt(versions[0]), Integer.parseInt(versions[1]));
                                    if( versionsPair.second > 0xFFFF || versionsPair.first > 0xFFFF ) {
                                        throw new NumberFormatException();
                                    }
                                    environment.cfv.setVersion(Integer.parseInt(versions[0]), Integer.parseInt(versions[1])).
                                            setByParameter(true).setFrozen(true);
                                } else {
                                    throw new NumberFormatException();
                                }
                            }
                        } catch (PatternSyntaxException | NumberFormatException exception) {
                                environment.error("err.invalid_threshold_major_minor_param");
                            usage();
                            throw new IllegalArgumentException();
                        }
                    }
                    default -> {
                        if (arg.startsWith("-")) {
                            environment.error("err.invalid_option", arg);
                            usage();
                            throw new IllegalArgumentException();
                        } else {
                            fileList.add(new FileInput(argv[i]));
                        }
                    }
                }
            }
            if (fileList.isEmpty()) {
                usage();
                System.exit(FAILED);
            }
        } catch (IllegalArgumentException iae) {
            if (environment.hasMessages()) {
                environment.flush(false);
            }
            System.exit(FAILED);
        }
    }
}
