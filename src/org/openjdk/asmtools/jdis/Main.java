/*
 * Copyright (c) 1996, 2025, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.asmtools.common.outputs.ToolOutput;
import org.openjdk.asmtools.common.outputs.log.DualOutputStreamOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.openjdk.asmtools.Main.WRITE_SWITCH;
import static org.openjdk.asmtools.common.Environment.FAILED;
import static org.openjdk.asmtools.common.Environment.OK;
import static org.openjdk.asmtools.common.outputs.FSOutput.FSDestination.DIR;
import static org.openjdk.asmtools.common.outputs.FSOutput.FSDestination.FILE;
import static org.openjdk.asmtools.jdis.Options.PrintOption;
import static org.openjdk.asmtools.jdis.Options.PrintOption.*;
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

    @Override
    public synchronized int decode() {
        return this.disasm();
    }

    public synchronized boolean decode(String... argv) {
        return this.disasm(argv);
    }

    // Runs disassembler when args already parsed
    public synchronized int disasm() {
        int rc = OK;
        for (ToolInput toolInput : fileList) {
            ClassData classData = null;
            try {
                environment.setToolInput(toolInput);
                classData = new ClassData(environment);
                toolInput.setDetailedInput(classData.isDetailedOutput());
                try (DataInputStream dis = toolInput.getDataInputStream(Optional.of(environment))) {
                    classData.read(dis, Paths.get(toolInput.getName()));
                }
                environment.traceln(() -> "Options:\n%s\n".formatted(Options.getPrintOptions()));
                environment.getToolOutput().startClass(classData.className, Optional.of(".jasm"), environment);
                classData.print();
                environment.getToolOutput().finishClass(classData.className);
                environment.getOutputs().flush();
                rc += environment.getLogger().registerTotalIssues(rc, toolInput);
                environment.getLogger().flush();
                continue;
            } catch (FileNotFoundException fnf) {
                environment.printException(fnf);
                environment.error("err.not_found", toolInput);
                rc = FAILED;
            } catch (IOException | ClassFormatError ioe) {
                classData.postPrint();
                environment.error(ioe);
                environment.printException(ioe);
                rc += environment.getLogger().registerTotalIssues(rc, toolInput);
            } catch (Error error) {
                classData.postPrint();
                environment.error(error);
                environment.printException(error);
                rc += environment.getLogger().registerTotalIssues(rc, toolInput);
            } catch (Exception ex) {
                classData.postPrint();
                environment.error(ex);
                environment.printException(ex);
                rc += environment.getLogger().registerTotalIssues(rc, toolInput);
            }
            environment.getLogger().flush();
            break;
        }
        return rc;
    }

    @Override
    public void usage() {
        environment.usage(List.of(
                "info.usage",
                "info.opt.d",
                "info.opt.w",
                "info.opt.g",
                "info.opt.gg",
                "info.opt.nc",
                "info.opt.table",
                "info.opt.hx",
                "info.opt.instr.offset",
                "info.opt.sysinfo",
                "info.opt.lnt",
                "info.opt.lvt",
                "info.opt.drop",
                "info.opt.b",
                "info.opt.version",
                "info.opt.t",
                "info.opt.v"
        ));
    }

    @Override
    protected void parseArgs(String... argv) {
        Options.setDefaultOutputOptions();
        // Parse arguments
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            switch (arg) {
                case "-g":
                    Options.setDetailedOutputOptions();
                    break;
                case "-v":
                    Options.set(VERBOSE);
                    environment.setVerboseFlag(true);
                    break;
                case "-sysinfo":
                    Options.set(SYSINFO);
                    break;
                case "-t":
                    Options.set(VERBOSE);
                    Options.set(TRACE);
                    environment.setVerboseFlag(true);
                    environment.setTraceFlag(true);
                    break;
                case "-pc":
                    Options.set(PRINT_BCI);
                    break;
                case "-nc":
                    Options.set(NO_COMMENTS);
                    break;
                case "-hx":
                    Options.set(HEX);
                    break;
                case "-f":                                          // -f <file>
                    setFSDestination(FILE, ++i, argv);
                    break;
                case org.openjdk.asmtools.Main.DIR_SWITCH:          // -d <directory>
                    setFSDestination(DIR, ++i, argv);
                    break;
                case WRITE_SWITCH:                                  // -w
                    environment.setIgnorePackage(true);
                    setFSDestination(DIR, ++i, argv);
                    break;
                case org.openjdk.asmtools.Main.DUAL_LOG_SWITCH:     // -dls
                    this.environment.setOutputs(new DualOutputStreamOutput());
                    break;
                case org.openjdk.asmtools.Main.VERSION_SWITCH:      // -version
                    environment.println(FULL_VERSION);
                    System.exit(OK);
                case org.openjdk.asmtools.Main.STDIN_SWITCH:        // -
                    addStdIn();
                    break;
                case "-h", "-help":
                    usage();
                    System.exit(OK);
                case "-best-effort":
                    Options.set(BEST_EFFORT);
                    break;
                case "-gg":
                    Options.setDetailedOutputOptions();
                    Options.set(EXTRA_DETAILED_Output);
                    break;
                case "-table":
                    Options.set(PrintOption.TABLE);
                    break;
                default:
                    if (arg.startsWith("-")) {
                        if (arg.startsWith("-drop")) {
                            if (!parseParameters("-drop", arg, "DROP", DROP_All)) {
                                usage();
                                System.exit(FAILED);
                            }
                        } else if (arg.startsWith("-lnt")) {
                            if (!parseParameters("-lnt", arg, "LINE_NUMBER_TABLE", LINE_NUMBER_TABLE_All)) {
                                usage();
                                System.exit(FAILED);
                            }
                        } else if (arg.startsWith("-lvt")) {
                            if (!parseParameters("-lvt", arg, "LOCAL_VARIABLE", LOCAL_VARIABLE_All)) {
                                usage();
                                System.exit(FAILED);
                            }
                        } else {
                            environment.error("err.invalid_option", arg);
                            usage();
                            System.exit(FAILED);
                        }
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

    /**
     * Parse parameters group -option:parameters [parameters=option1,option2]
     *
     * @param option      one of the options [-lvt, -drop, -lnt]
     * @param parameters  one of combinations corresponded to the option:
     *                    <all,numbers,lines,table>, <all,vars,types> or
     *                    <all|debug|SourceFile,LocalVariable,LocalVariableType,CharacterRange>
     * @param optPrefix   prefix of the PR: LINE_NUMBER_TABLE, LOCAL_VARIABLE or DROP
     * @param blankOption option that is used if there are no parameters option1,option2... attached to the -option
     * @return true if parameters group parsed successfully
     */
    private boolean parseParameters(String option, String parameters, String optPrefix, PrintOption blankOption) {
        parameters = parameters.substring(option.length());
        if (parameters.isBlank()) {
            if (!blankOption.isActive()) {
                environment.error("err.option.unsupported", option + ":all");
                return false;
            }
            blankOption.apply();
        } else if (parameters.matches("^[:=-]+.*")) {
            parameters = parameters.substring(1);
            String[] prmArray = parameters.split(",");
            for (int i = 0; i < prmArray.length; i++) {
                PrintOption printOption = getStringFlag(optPrefix, prmArray[i]);
                if (printOption == null || !printOption.name().startsWith(optPrefix)) {
                    environment.error("err.invalid_parameter_of_option", prmArray[i], option);
                    return false;
                } else if (!printOption.isActive()) {
                    environment.error("err.option.unsupported", "%s:%s".formatted(option, prmArray[i]));
                    return false;
                }
                printOption.apply();
                if (printOption.equals(blankOption)) { // blank options is equal to
                    return true;
                }
            }
        } else {
            environment.error("err.option.unsupported", option + parameters);
            return false;
        }
        return true;
    }
}
