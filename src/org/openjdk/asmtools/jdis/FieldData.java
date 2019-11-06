/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.jasm.Modifiers;

import java.io.DataInputStream;
import java.io.IOException;

import static org.openjdk.asmtools.jasm.Tables.AttrTag;
import static org.openjdk.asmtools.jasm.Tables.CF_Context;

/**
 * Field data for field members in a class of the Java Disassembler
 */
public class FieldData extends MemberData {

    // CP index to the field name
    protected int name_cpx;

    // CP index to the field type
    protected int sig_cpx;

     // CP index to the field value
    protected int value_cpx = 0;

    public static final String initialTab = "";

    public FieldData(ClassData cls) {
        super(cls);
        memberType = "FieldData";
    }

    @Override
    protected boolean handleAttributes(DataInputStream in, AttrTag attrtag, int attrlen) throws IOException {
        // Read the Attributes
        boolean handled = true;
        switch (attrtag) {
            case ATT_ConstantValue:
                if (attrlen != 2) {
                    throw new ClassFormatError("invalid ConstantValue attr length");
                }
                value_cpx = in.readUnsignedShort();
                break;
            default:
                handled = false;
                break;
        }
        return handled;
    }

    /**
     * Read and resolve the field data called from ClassData.
     * Precondition: NumFields has already been read from the stream.
     */
    public void read(DataInputStream in) throws IOException {
        // read the Fields CP indexes
        access = in.readUnsignedShort();
        name_cpx = in.readUnsignedShort();
        sig_cpx = in.readUnsignedShort();
        TraceUtils.traceln("      FieldData: name[" + name_cpx + "]=" + cls.pool.getString(name_cpx)
                + " sig[" + sig_cpx + "]=" + cls.pool.getString(sig_cpx));

        // Read the attributes
        readAttributes(in);
    }

    /**
     * Prints the field data to the current output stream. called from ClassData.
     */
    public void print() throws IOException {
        // Print annotations first
        // Print the Annotations
        if (visibleAnnotations != null) {
            out.println();
            for (AnnotationData visad : visibleAnnotations) {
                visad.print(out, initialTab);
            }
        }
        if (invisibleAnnotations != null) {
            out.println();
            for (AnnotationData invisad : invisibleAnnotations) {
                invisad.print(out, initialTab);
            }
        }

        if (visibleTypeAnnotations != null) {
            out.println();
            for (TypeAnnotationData visad : visibleTypeAnnotations) {
                visad.print(out, initialTab);
                out.println();
            }
        }
        if (invisibleTypeAnnotations != null) {
            out.println();
            for (TypeAnnotationData invisad : invisibleTypeAnnotations) {
                invisad.print(out, initialTab);
                out.println();
            }
        }

        boolean pr_cpx = options.contains(Options.PR.CPX);
        out.print(Modifiers.accessString(access, CF_Context.CTX_FIELD));
        if (isSynthetic) {
            out.print("synthetic ");
        }
        if (isDeprecated) {
            out.print("deprecated ");
        }
        out.print("Field ");
        if (pr_cpx) {
            out.print("#" + name_cpx + ":#" + sig_cpx);
        } else {
            out.print(cls.pool.getName(name_cpx) + ":" + cls.pool.getName(sig_cpx));
        }
        if (value_cpx != 0) {
            out.print("\t= ");
            cls.pool.PrintConstant(cls.out, value_cpx);
        }
        if (pr_cpx) {
            out.println(";\t // " + cls.pool.getName(name_cpx) + ":" + cls.pool.getName(sig_cpx));
        } else {
            out.println(";");
        }
    }
} // end FieldData

