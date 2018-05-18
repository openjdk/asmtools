/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.util.I18NResourceBundle;
import org.openjdk.asmtools.util.ProductInfo;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Main program of the Java Disassembler
 */
public class Main {

    private Options options = Options.OptionObject();
    /**
     * Name of the program.
     */
    public static String programName;
    /**
     * The stream where error message are printed.
     */
    PrintWriter out;
    /* debugging value, output stream will only allow this many
     * bytes to be written before throwing an error.
     */
    private int bytelimit = 0;

    public static final I18NResourceBundle i18n
            = I18NResourceBundle.getBundleForClass(Main.class);

    /**
     * Constructor.
     */
    public Main(PrintWriter out, String program) {
        this.out = out;
        Main.programName = program;
    }

    /**
     * Top level error message
     */
    public void error(String msg) {
        System.err.println(programName + ": " + msg);
    }

    /**
     * Usage
     */
    public void usage() {
        error(i18n.getString("jdis.usage"));
        error(i18n.getString("jdis.opt.g"));
        error(i18n.getString("jdis.opt.sl"));
        error(i18n.getString("jdis.opt.hx"));
        error(i18n.getString("jdis.opt.v"));
        error(i18n.getString("jdis.opt.version"));
    }

    /**
     * Run the disassembler
     */
    public synchronized boolean disasm(String argv[]) {
        ArrayList<String> vj = new ArrayList<>();

        // Parse arguments
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            switch (arg) {
                case "-g":
                    options.set(Options.PR.DEBUG);
                    break;
                case "-v":
                    options.setCodeOptions();
                    break;
                case "-sl":
                    options.set(Options.PR.SRC);
                    break;
                case "-hx":
                    options.set(Options.PR.HEX);
                    break;
                case "-version":
                    out.println(ProductInfo.FULL_VERSION);
                    break;
                default:
                    if (arg.startsWith("-")) {
                        error(i18n.getString("jdis.error.invalid_option", arg));
                        usage();
                        return false;
                    } else {
                        vj.add(arg);
                    }
                    break;
            }
        }

        if (vj.isEmpty()) {
            usage();
            return false;
        }

        for (String inpname : vj) {
            if (inpname == null) {
                continue;
            } // cross out by CompilerChoice.compile
            try {
                ClassData cc = new ClassData(out);
                cc.read(new DataInputStream(new FileInputStream(inpname)));
                cc.print();
                continue;
            } catch (FileNotFoundException ee) {
                error(i18n.getString("jdis.error.cannot_read", inpname));
            } catch (Error ee) {
                if (options.contains(Options.PR.DEBUG))
                    ee.printStackTrace();
                error(i18n.getString("jdis.error.fatal_error", inpname));
            } catch (Exception ee) {
                if (options.contains(Options.PR.DEBUG))
                    ee.printStackTrace();
                error(i18n.getString("jdis.error.fatal_exception", inpname));
            }
            return false;
        }
        return true;
    }

    /**
     * Main program
     */
    public static void main(String argv[]) {
        Main disassembler = new Main(new PrintWriter(new uEscWriter(System.out)), "jdis");
        boolean result = disassembler.disasm(argv);
        System.exit(result ? 0 : 1);
    }
}
