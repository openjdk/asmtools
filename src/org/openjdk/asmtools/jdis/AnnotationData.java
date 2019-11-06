/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 *
 */
public class AnnotationData {
    /*-------------------------------------------------------- */
    /* AnnotData Fields */

    private boolean invisible = false;
    private int type_cpx = 0;  //an index into the constant pool indicating the annotation type for this annotation.
    private ArrayList<AnnotElem> array = new ArrayList<>();

    private ClassData cls;
    protected String visAnnotToken = "@+";
    protected String invAnnotToken = "@-";
    protected String dataName = "AnnotationData";
    /*-------------------------------------------------------- */

    public AnnotationData(boolean invisible, ClassData cls) {
        this.cls = cls;
        this.invisible = invisible;
    }

    public void read(DataInputStream in) throws IOException {
        type_cpx = in.readShort();
        TraceUtils.traceln("             " + dataName + ": name[" + type_cpx + "]=" + cls.pool.getString(type_cpx));
        int elemValueLength = in.readShort();
        TraceUtils.traceln("                 " + dataName + ": " + cls.pool.getString(type_cpx) + "num_elems: " + elemValueLength);
        for (int evc = 0; evc < elemValueLength; evc++) {
            AnnotElem elem = new AnnotElem(cls);
            TraceUtils.traceln("                    " + dataName + ": " + cls.pool.getString(type_cpx) + " reading [" + evc + "]");
            elem.read(in, invisible);
            array.add(elem);
        }
    }

    public void print(PrintWriter out, String tab) {
        printHeader(out, tab);
        printBody(out, tab);
    }

    protected void printHeader(PrintWriter out, String tab) {
        //Print annotation Header, which consists of the
        // Annotation Token ('@'), visibility ('+', '-'),
        // and the annotation name (type index, CPX).

        // Mark whether it is invisible or not.
        if (invisible) {
            out.print(tab + invAnnotToken);
        } else {
            out.print(tab + visAnnotToken);
        }
        String annoName = cls.pool.getString(type_cpx);

        // converts class type to java class name
        if (annoName.startsWith("L") && annoName.endsWith(";")) {
            annoName = annoName.substring(1, annoName.length() - 1);
        }

        out.print(annoName);
    }

    protected void printBody(PrintWriter out, String tab) {
        // For a standard annotation, print out brackets,
        // and list the name/value pairs.
        out.print(" { ");

        int i = 0;
        for (AnnotElem elem : array) {
            elem.print(out, tab);

            if (i++ < array.size() - 1) {
                out.print(", ");
            }
        }
        out.print(tab + "}");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String annoName = cls.pool.getString(type_cpx);

        // converts class type to java class name
        if (annoName.startsWith("L") && annoName.endsWith(";")) {
            annoName = annoName.substring(1, annoName.length() - 1);
        }

        //Print annotation
        // Mark whether it is invisible or not.
        if (invisible) {
            sb.append(invAnnotToken);
        } else {
            sb.append(visAnnotToken);
        }

        sb.append(annoName);
        sb.append(" { ");

        int i = 0;
        for (AnnotElem elem : array) {
            sb.append(elem.toString());

            if (i++ < array.size() - 1) {
                sb.append(", ");
            }
        }

        _toString(sb);

        sb.append("}");
        return sb.toString();
    }

    protected void _toString(StringBuilder sb) {
        // sub-classes override this
    }
} // end AnnotData

