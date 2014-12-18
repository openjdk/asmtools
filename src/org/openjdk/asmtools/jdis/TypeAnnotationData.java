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

import static org.openjdk.asmtools.jasm.TypeAnnotationUtils.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Type Annotation data is a specific kind of AnnotationData. As well as the normal data
 * items needed to present an annotation, Type annotations require a TargetInfo
 * descriptor. This descriptor is based on a TargetType, and it optionally may contain a
 * location descriptor (when the Type is embedded in a collection).
 *
 * The TypeAnnotationData class is based on JDis's AnnotationData class, and contains the
 * (jasm) class for representing TargetInfo.
 */
public class TypeAnnotationData extends AnnotationData {

    private TargetInfo target_info;
    private ArrayList<TypePathEntry> target_path;
    private static TTVis TT_Visitor = new TTVis();

    public TypeAnnotationData(boolean invisible, ClassData cls) {
        super(invisible, cls);
        target_info = null;
        visAnnotToken = "@T+";
        invAnnotToken = "@T-";
        dataName = "TypeAnnotationData";
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        super.read(in);

        // read everything related to the Type Annotation
        // target type tag
// KTL 1/10/13 (changed short-> byte for latest spec rev)
//        int tt = (char) in.readUnsignedShort(); // cast to introduce signedness
        int tt = (byte) in.readUnsignedByte(); // cast to introduce signedness
        Integer ttInt = new Integer(tt);
        TargetType ttype;
        ttype = targetTypeEnum(ttInt);

        if (ttype == null) {
            // Throw some kind of error for bad target type index
            throw new IOException("Bad target type: " + tt + " in TypeAnnotationData");
        }

        // read the target info
        TT_Visitor.init(in);
        TT_Visitor.visitExcept(ttype);
        target_info = TT_Visitor.getTargetInfo();

        // read the target path info
        int len = in.readUnsignedShort();
        target_path = new ArrayList<>(len);
        TraceUtils.traceln("    --------- [TypeAnnotationData.read]: Reading Location (length = " + len + ").");
        TraceUtils.trace("    --------- [TypeAnnotationData.read]: [ ");
        for (int i = 0; i < len; i++) {
            int pathType = in.readUnsignedByte();
            String pk = (getPathKind(pathType)).parsekey();
            char pathArgIndex = (char) in.readUnsignedByte();
            target_path.add(new TypePathEntry(pathType, pathArgIndex));
            TraceUtils.trace(" " + pk + "(" + pathType + "," + pathArgIndex + "), ");
        }
        TraceUtils.traceln("] ");

//            target_info.setLocation(location);
    }

    @Override
    protected void printBody(PrintWriter out, String tab) {
        // For a type annotation, print out brackets,
        // print out the (regular) annotation name/value pairs,
        // then print out the target types.
        out.print(" {");
        super.printBody(out, tab);
        target_info.print(out, tab);
        printPath(out, tab);
        out.print("}");
    }

    protected void printPath(PrintWriter out, String tab) {
        // For a type annotation, print out brackets,
        // print out the (regular) annotation name/value pairs,
        // then print out the target types.
        out.print(" {");
        boolean first = true;
        for (TypePathEntry tpe : target_path) {
            if (!first) {
                out.print(", ");
            }
            first = false;
            out.print(tpe.toString());
        }
        target_info.print(out, tab);
        printPath(out, tab);
        out.print("}");
    }

    @Override
    protected void _toString(StringBuilder sb) {
        // sub-classes override this
        sb.append(target_info.toString());
    }

    /**
     * TTVis
     *
     * Target Type visitor, used for constructing the target-info within a type
     * annotation. visitExcept() is the entry point. ti is the constructed target info.
     */
    private static class TTVis extends TypeAnnotationTargetVisitor {

        private TargetInfo ti = null;
        private IOException IOProb = null;
        private DataInputStream in;

        public TTVis() {
        }

        public void init(DataInputStream in) {
            this.in = in;
        }

        public int scanByteVal() {
            int val = 0;
            try {
                val = in.readUnsignedByte();
            } catch (IOException e) {
                IOProb = e;
            }
            return val;
        }

        public int scanShortVal() {
            int val = 0;
            try {
                val = in.readUnsignedShort();
            } catch (IOException e) {
                IOProb = e;
            }
            return val;
        }

        public int scanIntVal() {
            int val = 0;
            try {
                val = in.readInt();
            } catch (IOException e) {
                IOProb = e;
            }
            return val;
        }

        //This is the entry point for a visitor that tunnels exceptions
        public void visitExcept(TargetType tt) throws IOException {
            IOProb = null;
            ti = null;

            TraceUtils.traceln("                      Target Type: " + tt.parseKey());
            visit(tt);

            if (IOProb != null) {
                throw IOProb;
            }
        }

        public TargetInfo getTargetInfo() {
            return ti;
        }

        private boolean error() {
            return IOProb != null;
        }

        @Override
        public void visit_type_param_target(TargetType tt) {
            TraceUtils.trace("                      Type Param Target: ");
            int byteval = scanByteVal(); // param index
            TraceUtils.traceln("{ param_index: " + byteval + "}");
            if (!error()) {
                ti = new typeparam_target(tt, byteval);
            }
        }

        @Override
        public void visit_supertype_target(TargetType tt) {
            TraceUtils.trace("                      SuperType Target: ");
            int shortval = scanShortVal(); // type index
            TraceUtils.traceln("{ type_index: " + shortval + "}");
            if (!error()) {
                ti = new supertype_target(tt, shortval);
            }
        }

        @Override
        public void visit_typeparam_bound_target(TargetType tt) {
            TraceUtils.trace("                      TypeParam Bound Target: ");
            int byteval1 = scanByteVal(); // param index
            if (error()) {
                return;
            }
            int byteval2 = scanByteVal(); // bound index
            if (error()) {
                return;
            }
            TraceUtils.traceln("{ param_index: " + byteval1 + " bound_index: " + byteval2 + "}");
            ti = new typeparam_bound_target(tt, byteval1, byteval2);
        }

        @Override
        public void visit_empty_target(TargetType tt) {
            TraceUtils.traceln("                      Empty Target: ");
            if (!error()) {
                ti = new empty_target(tt);
            }
        }

        @Override
        public void visit_methodformalparam_target(TargetType tt) {
            TraceUtils.trace("                      MethodFormalParam Target: ");
            int byteval = scanByteVal(); // param index
            TraceUtils.traceln("{ param_index: " + byteval + "}");
            if (!error()) {
                ti = new methodformalparam_target(tt, byteval);
            }
        }

        @Override
        public void visit_throws_target(TargetType tt) {
            TraceUtils.trace("                      Throws Target: ");
            int shortval = scanShortVal(); // exception index
            TraceUtils.traceln("{ exception_index: " + shortval + "}");
            if (!error()) {
                ti = new throws_target(tt, shortval);
            }
        }

        @Override
        public void visit_localvar_target(TargetType tt) {
            TraceUtils.traceln("                      LocalVar Target: ");
            int tblsize = scanShortVal(); // table length (short)
            if (error()) {
                return;
            }
            localvar_target locvartab = new localvar_target(tt, tblsize);
            ti = locvartab;

            for (int i = 0; i < tblsize; i++) {
                int shortval1 = scanShortVal(); // startPC
                if (error()) {
                    return;
                }
                int shortval2 = scanShortVal(); // length
                if (error()) {
                    return;
                }
                int shortval3 = scanShortVal(); // CPX
                TraceUtils.trace("                         LocalVar[" + i + "]: ");
                TraceUtils.traceln("{ startPC: " + shortval1 + ", length: " + shortval2 + ", CPX: " + shortval3 + "}");
                locvartab.addEntry(shortval1, shortval2, shortval3);
            }
        }

        @Override
        public void visit_catch_target(TargetType tt) {
            TraceUtils.trace("                      Catch Target: ");
            int shortval = scanShortVal(); // catch index
            TraceUtils.traceln("{ catch_index: " + shortval + "}");
            if (!error()) {
                ti = new catch_target(tt, shortval);
            }
        }

        @Override
        public void visit_offset_target(TargetType tt) {
            TraceUtils.trace("                      Offset Target: ");
            int shortval = scanShortVal(); // offset index
            TraceUtils.traceln("{ offset_index: " + shortval + "}");
            if (!error()) {
                ti = new offset_target(tt, shortval);
            }
        }

        @Override
        public void visit_typearg_target(TargetType tt) {
            TraceUtils.trace("                      TypeArg Target: ");
            int shortval = scanShortVal(); // offset
            if (error()) {
                return;
            }
            int byteval = scanByteVal(); // type index
            if (error()) {
                return;
            }
            TraceUtils.traceln("{ offset: " + shortval + " type_index: " + byteval + "}");
            ti = new typearg_target(tt, shortval, byteval);
        }

    }

}
