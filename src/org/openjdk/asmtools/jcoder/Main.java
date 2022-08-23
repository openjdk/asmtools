/*
 * Copyright (c) 2009, 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.structure.ToolInput;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
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

    private final ArrayList<ToolInput> fileList = new ArrayList<>(1);
    HashMap<String, String> macros = new HashMap<>(1);
    private File destDir;
    // tool options
    private boolean noWriteFlag = false;        // Do not write generated class files
    private boolean ignoreFlag = false;         // Ignore non-fatal error(s) that suppress writing class files

    public Main(String... argv) {
        super();
        parseArgs(argv);
    }

    public Main(PrintWriter errorLogger, PrintWriter outputLogger, String... argv) {
        super(errorLogger, outputLogger);
        parseArgs(argv);
    }

    // jcoder entry point
    public static void main(String... argv) {
        Main compiler = new Main(argv);
        System.exit(compiler.compile());
    }

    @Override
    public void usage() {
        environment.flush(false);
        environment.info("info.usage");
        environment.info("info.opt.nowrite");
        environment.info("info.opt.ignore");
        environment.info("info.opt.d");
        environment.info("info.opt.v");
        environment.info("info.opt.g");
        environment.info("info.opt.version");
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
                parser.write(destDir);
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
                    case "-d" -> destDir = setDestDir(++i, argv);
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
                    case "-version" -> environment.println(FULL_VERSION);
                    case "-h", "-help" -> {
                        usage();
                        System.exit((OK));
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
            if (fileList.size() == 0) {
                fileList.add(new ToolInput.StdinInput());
            }
        } catch (IllegalArgumentException iae) {
            if (environment.hasMessages()) {
                environment.flush(false);
            }
            System.exit(FAILED);
        }
    }
}
