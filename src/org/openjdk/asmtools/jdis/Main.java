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
package org.openjdk.asmtools.jdis;

import org.openjdk.asmtools.common.ToolInput;
import org.openjdk.asmtools.common.ToolOutput;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

import static org.openjdk.asmtools.common.Environment.FAILED;
import static org.openjdk.asmtools.common.Environment.OK;
import static org.openjdk.asmtools.util.ProductInfo.FULL_VERSION;

/**
 * jdis is a disassembler that accepts a .class file, and prints the plain-text translation of jasm source file
 * to the standard output.
 * <p>
 * Main program of the Java Disassembler :: class to jasm
 */
public class Main extends JdisTool {

    public Main(ToolOutput toolOutput, ToolOutput.DualStreamToolOutput log, ToolInput... toolInputs) {
        super(toolOutput, log);
        for(ToolInput toolInput: toolInputs){
            fileList.add(toolInput);
        }
        parseArgs(new String[0]);
    }

    public Main(ToolOutput toolOutput, ToolOutput.DualStreamToolOutput log, ToolInput toolInput, String... argv) {
        super(toolOutput, log);
        if (toolInput != null) {
            fileList.add(toolInput);
        }
        parseArgs(argv);
    }

    public Main(ToolOutput toolOutput, String... argv) {
        super(toolOutput);
        parseArgs(argv);
    }

    public Main(ToolOutput toolOutput, ToolOutput.DualStreamToolOutput logger, String... argv) {
        super(toolOutput, logger);
        parseArgs(argv);
    }

    // jdis entry point
    public static void main(String... argv) {
        Main disassembler = new Main(new ToolOutput.EscapedPrintStreamOutput(System.out), argv);
        System.exit(disassembler.disasm());
    }

    // Run disassembler when args already parsed
    public synchronized int disasm() {
        for (ToolInput inputFileName : fileList) {
            try {
                environment.setInputFile(inputFileName);
                ClassData classData = new ClassData(environment);
                try(DataInputStream dis=inputFileName.getDataInputStream(Optional.of(environment))) {
                    classData.read(dis, Paths.get(inputFileName.getFileName()));
                }
                environment.getToolOutput().startClass(classData.className, Optional.of(".jasm"), environment);
                classData.print();
                environment.getToolOutput().finishClass(classData.className);
                environment.getOutputs().flush();
                continue;
            } catch (FileNotFoundException fnf) {
                environment.printException(fnf);
                environment.error("err.not_found", inputFileName);
            } catch (IOException | ClassFormatError ioe) {
                environment.printException(ioe);
                if (!environment.getVerboseFlag())
                    environment.printErrorLn(ioe.getMessage());
                environment.error("err.fatal_error", inputFileName);
            } catch (Error error) {
                environment.printException(error);
                environment.error("err.fatal_error", inputFileName);
            } catch (Exception ex) {
                environment.printException(ex);
                environment.error("err.fatal_exception", inputFileName);
            }
            return FAILED;
        }
        return OK;
    }

    @Override
    public void usage() {
        environment.info("info.usage");
        environment.info("info.opt.g");
        environment.info("info.opt.sl");
        environment.info("info.opt.lt");
        environment.info("info.opt.lv");
        environment.info("info.opt.hx");
        environment.info("info.opt.d");
        environment.info("info.opt.v");
        environment.info("info.opt.t");
        environment.info("info.opt.version");
    }

    @Override
    protected void parseArgs(String... argv) {
        // Parse arguments
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            switch (arg) {
                case "-g":
                    Options.setDetailedOutputOptions();
                    break;
                case "-v":
                    Options.set(Options.PR.VERBOSE);
                    environment.setVerboseFlag(true);
                    break;
                case "-t":
                    Options.set(Options.PR.VERBOSE);
                    Options.set(Options.PR.TRACE);
                    environment.setVerboseFlag(true);
                    environment.setTraceFlag(true);
                    break;
                case "-sl":
                    Options.set(Options.PR.SRC);
                    break;
                case "-lt":
                    Options.set(Options.PR.LNT);
                    break;
                case "-lv":
                    Options.set(Options.PR.VAR);
                    break;
                case "-hx":
                    Options.set(Options.PR.HEX);
                    break;
                case org.openjdk.asmtools.Main.DIR_SWITCH:
                    setDestDir(++i, argv);
                    break;
                case org.openjdk.asmtools.Main.DUAL_LOG_SWITCH:
                    this.environment.setOutputs(new ToolOutput.DualOutputStreamOutput());
                    break;
                case org.openjdk.asmtools.Main.VERSION_SWITCH:
                    environment.println(FULL_VERSION);
                    System.exit(OK);
                case org.openjdk.asmtools.Main.STDIN_SWITCH:
                    addStdIn();
                    break;
                case "-h", "-help":
                    usage();
                    System.exit(OK);
                default:
                    if (arg.startsWith("-")) {
                        environment.error("err.invalid_option", arg);
                        usage();
                        System.exit(FAILED);
                    } else {
                        fileList.add(new ToolInput.FileInput(arg));
                    }
            }
        }
        if (fileList.isEmpty()) {
            usage();
            System.exit(FAILED);
        }
    }
}
