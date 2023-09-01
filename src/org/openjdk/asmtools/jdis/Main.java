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
package org.openjdk.asmtools.jdis;

import org.openjdk.asmtools.common.inputs.FileInput;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.StdoutOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.outputs.log.DualOutputStreamOutput;
import org.openjdk.asmtools.common.outputs.ToolOutput;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
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

    public Main(ToolOutput toolOutput, DualStreamToolOutput log, ToolInput... toolInputs) {
        super(toolOutput, log);
        Collections.addAll(fileList, toolInputs);
    }

    public Main(ToolOutput toolOutput, DualStreamToolOutput log, ToolInput toolInput) {
        super(toolOutput, log);
        fileList.add(toolInput);
    }

    public Main(ToolOutput toolOutput, DualStreamToolOutput log, ToolInput toolInput, String... argv) {
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

    public Main(ToolOutput toolOutput, DualStreamToolOutput logger, String... argv) {
        super(toolOutput, logger);
        parseArgs(argv);
    }

    // jdis entry point
    public static void main(String... argv) {
        Main disassembler = new Main(new StdoutOutput(), argv);
        System.exit(disassembler.disasm());
    }

    // Runs disassembler with args
    public synchronized boolean disasm(String... argv) {
        parseArgs(argv);
        return this.disasm() == OK;
    }

    // Runs disassembler when args already parsed
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
        environment.usage(List.of(
        "info.usage",
        "info.opt.d",
        "info.opt.g",
        "info.opt.nc",
        "info.opt.lt",
        "info.opt.lv",
        "info.opt.instr.offset",
        "info.opt.hx",
        "info.opt.sl",
// TODO "info.opt.table",
        "info.opt.t",
        "info.opt.v",
        "info.opt.version"));
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
                case "-pc":
                    Options.set(Options.PR.PC);
                    break;
                case "-sl":
                    Options.set(Options.PR.SRC);
                    break;
                case "-lt":
                    Options.set(Options.PR.LNT);
                    break;
                case "-nc":
                    Options.set(Options.PR.NC);
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
                    this.environment.setOutputs(new DualOutputStreamOutput());
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
                        fileList.add(new FileInput(arg));
                    }
            }
        }
        if (fileList.isEmpty()) {
            usage();
            System.exit(FAILED);
        }
    }
}
