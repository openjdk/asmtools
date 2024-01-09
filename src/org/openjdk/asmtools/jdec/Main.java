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
package org.openjdk.asmtools.jdec;

import org.openjdk.asmtools.common.inputs.FileInput;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.EscapedPrintStreamOutput;
import org.openjdk.asmtools.common.outputs.StdoutOutput;
import org.openjdk.asmtools.common.outputs.ToolOutput;
import org.openjdk.asmtools.common.outputs.log.DualOutputStreamOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.outputs.log.StderrLog;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.openjdk.asmtools.common.Environment.FAILED;
import static org.openjdk.asmtools.common.Environment.OK;
import static org.openjdk.asmtools.util.ProductInfo.FULL_VERSION;

/**
 * jdec is a disassembler that accepts a .class file, and prints the plain-text translation of jcod source file
 * to the standard output.
 * <p>
 * Main program of the Java DECoder :: class to jcod
 */
public class Main extends JdecTool {

    public Main(ToolOutput toolOutput, DualStreamToolOutput log, ToolInput... toolInputs) {
        super(toolOutput, log);
        Collections.addAll(fileList, toolInputs);
    }

    public Main(ToolOutput toolOutput, DualStreamToolOutput log, ToolInput toolInput, String... argv) {
        super(toolOutput, log);
        if (toolInput != null) {
            fileList.add(toolInput);
        }
        parseArgs(argv);
    }

    public Main(ToolOutput toolOutput, DualStreamToolOutput log, String... argv) {
        this(toolOutput, log, null, argv);
    }

    public Main(EscapedPrintStreamOutput toolOutput, String[] argv) {
        this(toolOutput, new StderrLog(), argv);
    }

    public Main(ToolOutput toolOutput, DualStreamToolOutput log, ToolInput toolInput) {
        super(toolOutput, log);
        fileList.add(toolInput);
    }

    // jdec entry point
    public static void main(String... argv) {
        Main decoder = new Main(new StdoutOutput(), argv);
        System.exit(decoder.decode());
    }

    @Override
    public void usage() {
        environment.usage(List.of(
                "info.usage",
                "info.opt.d",
                "info.opt.g",
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
                    setPrintDetails(true);
                    break;
                case "-v":
                    environment.setVerboseFlag(true);
                    break;
                case "-t":
                    environment.setVerboseFlag(true);
                    environment.setTraceFlag(true);
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
                case "-h", "-help":
                    usage();
                    System.exit(OK);
                case org.openjdk.asmtools.Main.STDIN_SWITCH:
                    addStdIn();
                    break;
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

    public void setPrintDetails(boolean value) {
        environment.setPrintDetailsFlag(value);
    }

    // Runs the decoder with args
    public synchronized boolean decode(String... argv) {
        parseArgs(argv);
        return this.decode() == OK;
    }

    /**
     * Runs the decoder
     */
    public synchronized int decode() {
        for (ToolInput inputFileName : fileList) {
            try {
                environment.setInputFile(inputFileName);
                ClassData classData = new ClassData(environment);
                classData.decodeClass();
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
}
