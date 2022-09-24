/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.ToolInput;
import org.openjdk.asmtools.common.ToolOutput;

import java.io.*;

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


    public Main(ToolOutput toolOutput, ToolOutput.DualStreamToolOutput log, String... argv) {
        super(toolOutput, log);
        parseArgs(argv);
    }

    public Main(ToolOutput.EscapedPrintStreamOutput toolOutput, String[] argv) {
        this(toolOutput, new ToolOutput.SingleDualOutputStreamOutput(), argv);
    }

    // jdec entry point
    public static void main(String... argv) {
        Main decoder = new Main(new ToolOutput.EscapedPrintStreamOutput(System.out), argv);
        System.exit(decoder.decode());
    }

//    public static final I18NResourceBundle i18n
//            = I18NResourceBundle.getBundleForClass(Main.class);
//    int printFlags = 0;
//
//    public Main(PrintWriter out, PrintWriter err, String programName) {
//        super(out, err, programName);
//        printCannotReadMsg = (fname) ->
//                error(i18n.getString("jdec.error.cannot_read", fname));
//    }
//
//    public Main(PrintStream out, String program) {
//        this(new PrintWriter(out), new PrintWriter(System.err), program);
//    }
//
//    /**
//     * Main program
//     */
//    public static void main(String... argv) {
//        Main decoder = new Main(new PrintWriter(new uEscWriter(System.out)), new PrintWriter(System.err), "jdec");
//        System.exit(decoder.decode(argv) ? 0 : 1);
//    }

    @Override
    public void usage() {
        environment.info("info.usage");
        environment.info("info.opt.g");
        environment.info("info.opt.d");
        environment.info("info.opt.v");
        environment.info("info.opt.version");
    }

    @Override
    protected void parseArgs(String... argv) {
        // Parse arguments
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            switch (arg) {
                case "-g":
                    environment.setPrintDetailsFlag(true);
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
                    this.environment.setOutputs(new ToolOutput.DualOutputStreamOutput());
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
                        fileList.add(new ToolInput.FileInput(arg));
                    }
            }
        }
        if (fileList.isEmpty()) {
            usage();
            System.exit(FAILED);
        }
    }

    /**
     * Run the decoder
     */
    public synchronized int decode() {
        for (ToolInput inputFileName : fileList) {
            try {
//                DataInputStream dataInputStream = getDataInputStream(inpname);
//                if (dataInputStream == null)
//                    return false;
//                ClassData cc = new ClassData(dataInputStream, printFlags, toolOutput);
//                cc.DebugFlag = VerboseFlag.getAsBoolean();
//                cc.decodeClass(inpname);
//                toolOutput.flush();
//                continue;
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

//        long tm = System.currentTimeMillis();
//        ArrayList<String> vargs = new ArrayList<>();
//        ArrayList<String> vj = new ArrayList<>();
//        boolean nowrite = false;
//        int addOptions = 0;
//
//
//
//        String[] names = new String[0];
//        names = vj.toArray(names);
//        for (String inpname : names) {
//            try {
//                DataInputStream dataInputStream = getDataInputStream(inpname);
//                if (dataInputStream == null)
//                    return false;
//                ClassData cc = new ClassData(dataInputStream, printFlags, toolOutput);
//                cc.DebugFlag = VerboseFlag.getAsBoolean();
//                cc.decodeClass(inpname);
//                toolOutput.flush();
//                continue;
//            } catch (Error ee) {
//                if (VerboseFlag.getAsBoolean())
//                    ee.printStackTrace();
//                error(i18n.getString("jdec.error.fatal_error"));
//            } catch (Exception ee) {
//                if (VerboseFlag.getAsBoolean())
//                    ee.printStackTrace();
//                error(i18n.getString("jdec.error.fatal_exception"));
//            }
//            return false;
//        }
//        return true;
    }
}
