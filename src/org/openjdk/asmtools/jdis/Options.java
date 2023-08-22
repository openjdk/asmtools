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

import java.util.EnumSet;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * The singleton class to share global options among jdis classes.
 */
public class Options {

     public enum PR {
        CP("Constant Pool"),
        LNT("Line Number table"),
        PC("Program Counter - for all instructions"),
        LABS("Labels (as identifiers)"),
        CPX("CP index along with arguments"),
        SRC("Source Line as comment"),
        HEX("Numbers as hexadecimals"),
        VAR("Local variables declarations"),
        TRACE("Print internal traces, debug information"),
        NC("No comments, suppress printing comments"),
        VERBOSE("Verbose information");
        final String descriptor;

         PR(String descriptor) {
             this.descriptor=descriptor;
         }
     };

    static private final EnumSet<PR> JASM = EnumSet.of(PR.LABS);    // <no options>: default option(s)
    static private final EnumSet<PR> DETAILED_OUTPUT = EnumSet.of(  // -g:           detailed output format
            PR.CP,
            PR.PC,
            PR.CPX,
            PR.VAR
    );

    static private EnumSet<PR> printOptions = JASM;
    /* -------------------------------------------------------- */

    public static  void set(PR val) {
        printOptions.add(val);
    }

    public static  void setDetailedOutputOptions() {
        printOptions.addAll(DETAILED_OUTPUT);
        printOptions.remove(PR.LABS);
    }

    public static  void unsetDetailedOutputOptions() {
        printOptions.removeAll(DETAILED_OUTPUT);
        printOptions.add(PR.LABS);
    }

    public static boolean contains(PR val) { return printOptions.contains(val); }

    public static boolean traceEnabled() { return printOptions.contains(PR.TRACE); }

    public static String asShortString() {
        return  format("[ %s ]",
                printOptions.stream().map(item -> item.name()).collect(Collectors.joining(", ")));
    }
    public static String asLongString() {
        return format("Prints: [ %s ]",
                printOptions.stream().map(item -> item.descriptor).collect(Collectors.joining(", ")));
    }
}
