/*
 * Copyright (c) 1996, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jasm;

import org.openjdk.asmtools.asmutils.Pair;
import org.openjdk.asmtools.common.inputs.FileInput;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.PrintWriterOutput;
import org.openjdk.asmtools.common.outputs.StdoutOutput;
import org.openjdk.asmtools.common.outputs.ToolOutput;
import org.openjdk.asmtools.common.outputs.log.DualOutputStreamOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.outputs.log.StderrLog;
import org.openjdk.asmtools.common.structure.CFVersion;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;

import static org.openjdk.asmtools.common.Environment.FAILED;
import static org.openjdk.asmtools.common.Environment.OK;
import static org.openjdk.asmtools.util.ProductInfo.FULL_VERSION;

/**
 * Jasm is an assembler that accepts a text file based on the JASM Specification,
 * and produces a .class file for use with a Java Virtual Machine.
 * <p>
 * Main entry point of the JASM assembler :: jasm to class
 */
public class Main extends JasmTool {

    private final CFVersion cfv = new CFVersion();

    // tool options
    private boolean noWriteFlag = false;        // Do not write generated class files

    // hidden options
    private int byteLimit = 0;

    // hidden options: Parser debug flags
    private boolean debugScanner = false;
    private boolean debugMembers = false;
    private boolean debugCP = false;
    private boolean debugAnnot = false;
    private boolean debugInstr = false;

    public Main(ToolOutput toolOutput, String... argv) {
        super(toolOutput);
        parseArgs(argv);
    }

    public Main(ToolOutput toolOutput, DualStreamToolOutput log, String... argv) {
        super(toolOutput, log);
        parseArgs(argv);
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

    // jasm entry point
    public static void main(String... argv) {
        Main compiler = new Main(new StdoutOutput(), new StderrLog(), argv);
        System.exit(compiler.compile());
    }

    // Run jasm compiler with args
    public synchronized boolean compile(String... argv) {
        parseArgs(argv);
        return this.compile() == OK;
    }

    // Run jasm compiler when args already parsed
    public synchronized int compile() {
        // compile all input files
        int rc = OK;
        try {
            for (ToolInput inputFileName : fileList) {
                environment.setInputFile(inputFileName);
                Parser parser = new Parser(environment, cfv);
                // Set hidden options: Parser debug flags
                parser.setDebugFlags(debugScanner, debugMembers, debugCP, debugAnnot, debugInstr);
                parser.parseFile();
                if (environment.getErrorCount() > 0) break;
                if (noWriteFlag) continue;
                ClassData[] clsData = parser.getClassesData();
                for (ClassData cd : clsData) {
                    String fqn = cd.myClassName;
                    environment.getToolOutput().startClass(fqn, Optional.of(cd.fileExtension), environment);
                    if (byteLimit > 0) {
                        cd.setByteLimit(byteLimit);
                    }
                    cd.write(environment.getToolOutput());
                    environment.getToolOutput().finishClass(fqn);
                }
                if (environment.hasMessages()) rc += environment.flush(true);
            }
        } catch (IOException | URISyntaxException | Error exception) {
            environment.printException(exception);
            rc++;
        } catch (Throwable exception) {
            // all untrapped exception/errors that escaped CompilerLogger
            environment.printException(exception);
            environment.error(exception);
        }
        if (environment.hasMessages()) rc += environment.flush(true);
        return rc;
    }

    @Override
    public void usage() {
        environment.flush(false);
        environment.usage(List.of(
                "info.usage",
                "info.opt.d",
                "info.opt.nowrite",
                "info.opt.nowarn",
                "info.opt.strict",
                "info.opt.cv",
                "info.opt.fixcv",
                "info.opt.fixcv.full",
                "info.opt.t",
                "info.opt.v",
                "info.opt.version"
        ));
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
                    case "-strict" -> environment.setStrictWarningsOn();
                    case "-nowarn" -> environment.setIgnoreWarningsOn();
                    case "-nowrite" -> noWriteFlag = true;
                    case org.openjdk.asmtools.Main.VERSION_SWITCH -> {
                        environment.println(FULL_VERSION);
                        System.exit(OK);
                    }
                    case org.openjdk.asmtools.Main.DIR_SWITCH -> setDestDir(++i, argv);
                    case org.openjdk.asmtools.Main.DUAL_LOG_SWITCH ->
                            this.environment.setOutputs(new DualOutputStreamOutput());
                    case "-h", "-help", "-?" -> {
                        usage();
                        System.exit(OK);
                    }
                    // overrides cf version even if it's defined in the source file.
                    case "-fixcv", "-cv" -> {
                        boolean frozenCFV = (arg.startsWith("-fix"));
                        if ((i + 1) >= argv.length) {
                            if (frozenCFV) {
                                environment.error("err.fix_cv_requires_arg");
                            } else {
                                environment.error("err.cv_requires_arg");
                            }
                            usage();
                            throw new IllegalArgumentException();
                        }
                        try {
                            String cfvArg = argv[++i];
                            if (cfvArg.contains("-")) {
                                if (!frozenCFV) {
                                    throw new NumberFormatException();
                                }
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
                                cfv.setThreshold(versionsPair.first, versionsPair.second);
                                versionsPair = new Pair<>(Integer.parseInt(versionsUpdate[0]), Integer.parseInt(versionsUpdate[1]));
                                if( versionsPair.second > 0xFFFF || versionsPair.first > 0xFFFF ) {
                                    throw new NumberFormatException();
                                }
                                cfv.setVersion(versionsPair.first, versionsPair.second).setByParameter(true).setFrozen(true);
                            } else {
                                String[] versions = cfvArg.split("[.:]+", 2);
                                if (versions.length == 2) {
                                    Pair<Integer, Integer> versionsPair = new Pair<>(Integer.parseInt(versions[0]), Integer.parseInt(versions[1]));
                                    if( versionsPair.second > 0xFFFF || versionsPair.first > 0xFFFF ) {
                                        throw new NumberFormatException();
                                    }
                                    cfv.setVersion(Integer.parseInt(versions[0]), Integer.parseInt(versions[1])).
                                            setByParameter(true).setFrozen(frozenCFV);
                                } else {
                                    throw new NumberFormatException();
                                }
                            }
                        } catch (PatternSyntaxException | NumberFormatException exception) {
                            if (frozenCFV) {
                                environment.error("err.invalid_threshold_major_minor_param");
                            } else {
                                environment.error("err.invalid_major_minor_param");
                            }
                            usage();
                            throw new IllegalArgumentException();
                        }
                    }
                    // non-public options
                    case "-XdScanner" -> debugScanner = true;
                    case "-XdMember" -> debugMembers = true;
                    case "-XdCP" -> debugCP = true;
                    case "-XdInstr" -> debugInstr = true;
                    case "-XdAnnot" -> debugAnnot = true;
                    case "-XdAll" -> {
                        debugScanner = true;
                        debugMembers = true;
                        debugCP = true;
                        debugInstr = true;
                        debugAnnot = true;
                    }
                    case "-Xdlimit" -> {
                        // parses file until the specified byte number
                        if (i + 1 > argv.length) {
                            environment.error("err.byte.limit");
                            throw new IllegalArgumentException();
                        } else {
                            try {
                                byteLimit = Integer.parseInt(argv[++i]);
                            } catch (NumberFormatException e) {
                                environment.error("err.byte.limit");
                                throw new IllegalArgumentException();
                            }
                        }
                    }
                    case org.openjdk.asmtools.Main.STDIN_SWITCH -> addStdIn();
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
