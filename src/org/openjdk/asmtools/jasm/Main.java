/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static org.openjdk.asmtools.jasm.CFVersion.DEFAULT_MAJOR_VERSION;
import static org.openjdk.asmtools.jasm.CFVersion.DEFAULT_MINOR_VERSION;
import org.openjdk.asmtools.util.I18NResourceBundle;
import org.openjdk.asmtools.util.ProductInfo;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 *
 *
 */
public class Main {

    /**
     * Name of the program.
     */
    String program;

    /**
     * The stream where error message are printed.
     */
    PrintStream out;
    int nerrors = 0;

    public static final I18NResourceBundle i18n
            = I18NResourceBundle.getBundleForClass(Main.class);

    private File destDir = null;
    private boolean traceFlag = false;
    private boolean debugInfoFlag = false;
    private long tm = System.currentTimeMillis();
    private ArrayList<String> v = new ArrayList<>();
    private boolean nowrite = false;
    private boolean nowarn = false;
    private boolean strict = false;
    private String props = null;
    private int nwarnings = 0;
    private CFVersion cfv = new CFVersion();
    private int bytelimit = 0;
    private boolean debugScanner = false;
    private boolean debugMembers = false;
    private boolean debugCP = false;
    private boolean debugAnnot = false;
    private boolean debugInstr = false;


    /**
     * Constructor.
     */
    public Main(PrintStream out, String program) {
        this.out = out;
        this.program = program;
    }

    /**
     * Top level error message
     */
    public void error(String msg) {
        nerrors++;
        out.println(program + ": " + msg);
    }

    /**
     * Usage
     */
    public void usage() {
        out.println(i18n.getString("jasm.usage"));
        out.println(i18n.getString("jasm.opt.d"));
        out.println(i18n.getString("jasm.opt.g"));
        out.println(i18n.getString("jasm.opt.v"));
        out.println(i18n.getString("jasm.opt.nowrite"));
        out.println(i18n.getString("jasm.opt.nowarn"));
        out.println(i18n.getString("jasm.opt.strict"));
        out.println(i18n.getString("jasm.opt.cv", DEFAULT_MAJOR_VERSION, DEFAULT_MINOR_VERSION));
        out.println(i18n.getString("jasm.opt.version"));
    }

    /**
     * Run the compiler
     */
    private synchronized boolean parseArgs(String argv[]) {
        // Parse arguments
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            switch (arg) {
                case "-v":
                    traceFlag = true;
                    break;
                case "-g":
                    debugInfoFlag = true;
                    break;
                case "-nowrite":
                    nowrite = true;
                    break;
                case "-strict":
                    strict = true;
                    break;
                case "-nowarn":
                    nowarn = true;
                    break;
                case "-version":
                    out.println(ProductInfo.FULL_VERSION);
                    break;
                case "-d":
                    if ((i + 1) >= argv.length) {
                        error(i18n.getString("jasm.error.d_requires_argument"));
                        usage();
                        return false;
                    }
                    destDir = new File(argv[++i]);
                    if (!destDir.exists()) {
                        error(i18n.getString("jasm.error.does_not_exist", destDir.getPath()));
                        return false;
                    }
                    break;
                // non-public options
                case "-XdScanner":
                    debugScanner = true;
                    break;
                case "-XdMember":
                    debugMembers = true;
                    break;
                case "-XdCP":
                    debugCP = true;
                    break;
                case "-XdInstr":
                    debugInstr = true;
                    break;
                case "-XdAnnot":
                    debugAnnot = true;
                    break;
                case "-XdAll":
                    debugScanner = true;
                    debugMembers = true;
                    debugCP = true;
                    debugInstr = true;
                    debugAnnot = true;
                    break;
                case "-Xdlimit":
                    // parses file until the specified byte number
                    if (i + 1 > argv.length) {
                        out.println(" Error: Unspecified byte-limit");
                        return false;
                    } else {
                        i++;
                        String bytelimstr = argv[i];
                        bytelimit = 0;
                        try {
                            bytelimit = Integer.parseInt(bytelimstr);
                        } catch (NumberFormatException e) {
                            out.println(" Error: Unspecified byte-limit");
                            return false;
                        }
                    }
                    break;
                case "-cv":
                    if ((i + 1) >= argv.length) {
                        error(i18n.getString("jasm.error.cv_requires_arg"));
                        usage();
                        return false;
                    }
                    String[] versions = {"", ""};                      // workaround for String.split()
                    int index = argv[++i].indexOf(".");                //
                    if (index != -1) {                                 //
                        versions[0] = argv[i].substring(0, index);     //
                        versions[1] = argv[i].substring(index + 1);    //
                    }                                                  //
                    if (versions.length != 2) {
                        error(i18n.getString("jasm.error.invalid_major_minor_param"));
                        usage();
                        return false;
                    }
                    try {
                        cfv = new CFVersion(Short.parseShort(versions[0]), Short.parseShort(versions[1]) );
                    } catch (NumberFormatException e) {
                        error(i18n.getString("jasm.error.invalid_major_minor_param"));
                        usage();
                        return false;
                    }
                    break;
                default:
                    if (arg.startsWith("-")) {
                        error(i18n.getString("jasm.error.invalid_option", arg));
                        usage();
                        return false;
                    } else {
                        v.add(argv[i]);
                    }
                    break;
            }
        }
        if (v.size() == 0) {
            usage();
            return false;
        }
        if (strict) {
            nowarn = false;
        }
        return true;
    }

    private void reset() {
        destDir = null;
        traceFlag = false;
        debugInfoFlag = false;
        System.currentTimeMillis();
        v = new ArrayList<>();
        nowrite = false;
        nowarn = false;
        strict = false;
        props = null;
        nwarnings = 0;
        bytelimit = 0;
    }

    /**
     * Run the compiler
     */
    public synchronized boolean compile(String argv[]) {
        // Reset the state of all objs
        reset();

        boolean validArgs = parseArgs(argv);
        if (!validArgs) {
            return false;
        }
        // compile all input files
        Environment sf = null;
        try {
            for (String inpname : v) {
                Parser p;
                try {
                    sf = new Environment(new File(inpname), out, nowarn);
                    sf.traceFlag = traceFlag;
                    sf.debugInfoFlag = debugInfoFlag;
                    p = new Parser(sf, cfv.clone() );
                    p.setDebugFlags(debugScanner, debugMembers, debugCP, debugAnnot, debugInstr);
                    p.parseFile();
                } catch (FileNotFoundException ex) {
                    error(i18n.getString("jasm.error.cannot_read", inpname));
                    continue;
                }
                nerrors += sf.nerrors;
                nwarnings += sf.nwarnings;
                if (nowrite || (nerrors > 0)) {
                    sf.flushErrors();
                    continue;
                }
                try {
                    ClassData[] clsData = p.getClassesData();
                    for (int i = 0; i < clsData.length; i++) {
                        ClassData cd = clsData[i];
                        if (bytelimit > 0) {
                            cd.setByteLimit(bytelimit);
                        }
                        cd.write(destDir);
                    }
                } catch (IOException ex) {
                    if (bytelimit > 0) {
                        // IO Error thrown from user-specified byte ount
                        ex.printStackTrace();
                        error("UserSpecified byte-limit at byte[" + bytelimit + "]: " + ex.getMessage() + "\n" + sf.getErrorFile() + ": [" + sf.lineNumber() + ", " + sf.lineOffset() + "]");
                    } else {
                        String er = i18n.getString("jasm.error.cannot_write", ex.getMessage());
                        error(er + "\n" + sf.getErrorFile() + ": [" + sf.lineNumber() + ", " + sf.lineOffset() + "]");
                    }
                }
                sf.flushErrors(); // possible errors from write()
            }
        } catch (Error ee) {
            if (debugInfoFlag) {
                ee.printStackTrace();
            }
            String er = ee.getMessage() + "\n" + i18n.getString("jasm.error.fatal_error");
            error(er + "\n" + sf.getErrorFile() + ": [" + sf.lineNumber() + ", " + sf.lineOffset() + "]");
        } catch (Exception ee) {
            if (debugInfoFlag) {
                ee.printStackTrace();
            }
            String er = ee.getMessage() + "\n" + ee.getMessage() + "\n" + i18n.getString("jasm.error.fatal_exception");
            error(er + "\n" + sf.getErrorFile() + ": [" + sf.lineNumber() + ", " + sf.lineOffset() + "]");
        }

        boolean errs = nerrors > 0;
        boolean warns = (nwarnings > 0) && (!nowarn);
        boolean errsOrWarns = errs || warns;
        if (!errsOrWarns) {
            return true;
        }
        if (errs) {
            out.print(nerrors > 1 ? (nerrors + " errors") : "1 error");
        }
        if (errs && warns) {
            out.print(", ");
        }
        if (warns) {
            out.print(nwarnings > 1 ? (nwarnings + " warnings") : "1 warning");
        }
        out.println();
        if (strict) {
            return !errsOrWarns;
        } else {
            return !errs;
        }
    }

    /**
     * main program
     */
    public static void main(String argv[]) {
        Main compiler = new Main(System.out, "jasm");
        System.exit(compiler.compile(argv) ? 0 : 1);
    }
}
