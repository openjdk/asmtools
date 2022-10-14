/*
 * Copyright (c) 1996, 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.ToolOutput;
import org.openjdk.asmtools.common.structure.CFVersion;
import org.openjdk.asmtools.common.ToolInput;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;

import static org.openjdk.asmtools.common.Environment.FAILED;
import static org.openjdk.asmtools.common.Environment.OK;
import static org.openjdk.asmtools.common.structure.CFVersion.DEFAULT_MAJOR_VERSION;
import static org.openjdk.asmtools.common.structure.CFVersion.DEFAULT_MINOR_VERSION;
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

    public Main(ToolOutput toolOutput, ToolOutput.DualStreamToolOutput log, String... argv) {
        super(toolOutput, log);
        parseArgs(argv);
    }

    public Main(ToolOutput toolOutput, ToolOutput.DualStreamToolOutput log, ToolInput toolInput, String... argv) {
        super(toolOutput, log);
        if (toolInput!=null){
            fileList.add(toolInput);
        }
        parseArgs(argv);
    }

    public Main(ToolOutput toolOutput, ToolOutput.DualStreamToolOutput log, ToolInput... toolInputs) {
        super(toolOutput, log);
        for(ToolInput toolInput: toolInputs){
            fileList.add(toolInput);
        }
        parseArgs(new String[0]);
    }

    // jasm entry point
    public static void main(String... argv) {
        Main compiler = new Main(new ToolOutput.EscapedPrintStreamOutput(System.out), new ToolOutput.SingleDualOutputStreamOutput(), argv);
        System.exit(compiler.compile());
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
                String fqn = parser.getClassesData()[0].myClassName;
                environment.getToolOutput().startClass(fqn, Optional.of(parser.getClassesData()[0].fileExtension), environment);
                ClassData[] clsData = parser.getClassesData();
                for (ClassData cd : clsData) {
                    if (byteLimit > 0) {
                        cd.setByteLimit(byteLimit);
                    }
                    cd.write(environment.getToolOutput());
                }
                environment.getToolOutput().finishClass(fqn);
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
        environment.info("info.usage");
        environment.info("info.opt.d");
        environment.info("info.opt.t");
        environment.info("info.opt.v");
        environment.info("info.opt.nowrite");
        environment.info("info.opt.nowarn");
        environment.info("info.opt.strict");
        environment.info("info.opt.cv", DEFAULT_MAJOR_VERSION, DEFAULT_MINOR_VERSION);
        environment.info("info.opt.fixcv");
        environment.info("info.opt.version");
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
                    case "-strict" -> environment.setStrictWarnings(true);
                    case "-nowarn" -> environment.setIgnoreWarnings(true);
                    case "-nowrite" -> noWriteFlag = true;
                    case org.openjdk.asmtools.Main.VERSION_SWITCH -> {
                        environment.println(FULL_VERSION);
                        System.exit(OK);
                    }
                    case org.openjdk.asmtools.Main.DIR_SWITCH -> setDestDir(++i, argv);
                    case org.openjdk.asmtools.Main.DUAL_LOG_SWITCH -> {
                        this.environment.setOutputs(new ToolOutput.DualOutputStreamOutput());
                    }
                    case "-h", "-help" -> {
                        usage();
                        System.exit(OK);
                    }
                    // overrides cf version even if it's defined in the source file.
                    case "-fixcv", "-cv" -> {
                        boolean frozenCFV = (arg.startsWith("-fix"));
                        if ((i + 1) >= argv.length) {
                            environment.error("err.cv_requires_arg");
                            usage();
                            throw new IllegalArgumentException();
                        }
                        try {
                            String[] versions = argv[++i].split("[.:]+", 2);
                            if (versions.length == 2) {
                                cfv.setMajorVersion(Short.parseShort(versions[0])).
                                        setMinorVersion(Short.parseShort(versions[1])).
                                        setByParameter(true).setFrozen(frozenCFV);
                            } else {
                                throw new NumberFormatException();
                            }
                        } catch (PatternSyntaxException | NumberFormatException exception) {
                            environment.error("err.invalid_major_minor_param");
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
                    case org.openjdk.asmtools.Main.STDIN_SWITCH -> {
                        addStdIn();
                        break;
                    }
                    default -> {
                        if (arg.startsWith("-")) {
                            environment.error("err.invalid_option", arg);
                            usage();
                            throw new IllegalArgumentException();
                        } else {
                            fileList.add(new ToolInput.FileInput(argv[i]));
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
