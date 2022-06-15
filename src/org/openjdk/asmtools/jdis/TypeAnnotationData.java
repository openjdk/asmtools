/*
 * Copyright (c) 1996, 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.jasm.TypeAnnotationTargetInfoData;
import org.openjdk.asmtools.jasm.TypeAnnotationTypePathData;

import java.io.DataInputStream;
import java.io.IOException;

import static org.openjdk.asmtools.jasm.TypeAnnotationTargetInfoData.*;
import static org.openjdk.asmtools.jasm.TypeAnnotationTypes.*;

/**
 * Type Annotation data is a specific kind of AnnotationData. As well as the normal data
 * items needed to present an annotation, Type annotations require a TargetInfo
 * descriptor. This descriptor is based on a TargetType, and it optionally may contain a
 * location descriptor (when the Type is embedded in a collection).
 * <p>
 * The TypeAnnotationData class is based on JDis's AnnotationData class, and contains the
 * (jasm) class for representing TargetInfo.
 */
public class TypeAnnotationData<T extends MemberData> extends AnnotationData {

    private final TargetTypeVisitor targetTypeVisitor = new TargetTypeVisitor();
    private TypeAnnotationTargetInfoData targetInfo;
    private final TypeAnnotationTypePathData typePath;

    public TypeAnnotationData(T data, boolean invisible) {
        super(data, invisible);
        targetInfo = null;
        typePath = new TypeAnnotationTypePathData();
        visibleAnnotationToken = "@T+";
        invisibleAnnotationToken = "@T-";
        dataName = "TypeAnnotationData";
    }

    @Override
    public void read(DataInputStream in) throws IOException {

        int targetTypeID = in.readUnsignedByte();
        ETargetType targetType = ETargetType.getTargetType(targetTypeID);

        if (targetType == null) {
            // Throw some kind of error for bad target type index
            throw new IOException("Bad target type: " + targetTypeID + " in TypeAnnotationData");
        }

        // read the target info
        targetTypeVisitor.init(in);
        targetTypeVisitor.visitExcept(targetType);
        targetInfo = targetTypeVisitor.getTargetInfo();

        // read the target path info
        int len = in.readUnsignedByte();
        environment.traceln("[TypeAnnotationData.read]: Reading Location (length = " + len + ").");
        environment.trace("[TypeAnnotationData.read]: [ ");
        for (int i = 0; i < len; i++) {
            int pathType = in.readUnsignedByte();
            String pk = (getPathKind(pathType)).parseKey();
            char pathArgIndex = (char) in.readUnsignedByte();
            typePath.addTypePathEntry(new TypePathEntry(pathType, pathArgIndex));
            environment.trace(" " + pk + "(" + pathType + "," + pathArgIndex + "), ");
        }
        environment.traceln("] ");
        super.read(in);
    }

    @Override
    protected void printBody() throws IOException {
        // For a type annotation, print out brackets,
        // print out the (regular) annotation name/value pairs,
        // then print out the target types.
        /* Previous version
        --------------------------------------------------------------------------------------------
        out.print(" {");
        super.printBody(out, "");
        targetInfo.print(out, tab);
        typePath.print(out, tab);
        out.print(tab + "}");
        --------------------------------------------------------------------------------------------
         */
        if (isEmpty()) {
            // Marker annotation
            print(" { { }");
        } else {
            println(" {");
            setElementState(AnnotationElementState.INLINED_ELEMENT);
            super.printBody();
        }
        print(targetInfo.toPrintString());
        String location = typePath.toPrintString();
        if (!location.isBlank()) {
            print(location + " ");
        }
        print("}");
    }

    /**
     * Target Type visitor, used for constructing the target-info within a type
     * annotation. visitExcept() is the entry point. ti is the constructed target info.
     */
    private  class TargetTypeVisitor extends TypeAnnotationTargetVisitor {

        private TypeAnnotationTargetInfoData targetInfo = null;
        private IOException IOProb = null;
        private DataInputStream in;

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

        //This is the entry point for a visitor that tunnels exceptions
        public void visitExcept(ETargetType targetType) throws IOException {
            IOProb = null;
            targetInfo = null;
            environment.traceln("Target Type: " + targetType.parseKey());
            visit(targetType);

            if (IOProb != null) {
                throw IOProb;
            }
        }

        public TypeAnnotationTargetInfoData getTargetInfo() {
            return targetInfo;
        }

        private boolean error() {
            return IOProb != null;
        }

        @Override
        public void visit_type_param_target(ETargetType targetType) {
            environment.trace("Type Param Target: ");
            int paramIndex = scanByteVal();
            environment.traceln("{ param_index: " + paramIndex + "}");
            if (!error()) {
                targetInfo = new type_parameter_target(targetType, paramIndex);
            }
        }

        @Override
        public void visit_supertype_target(ETargetType targetType) {
            environment.trace("SuperType Target: ");
            int typeIndex = scanShortVal();
            environment.traceln("{ type_index: " + typeIndex + "}");
            if (!error()) {
                targetInfo = new supertype_target(targetType, typeIndex);
            }
        }

        @Override
        public void visit_typeparam_bound_target(ETargetType targetType) {
            environment.trace("TypeParam Bound Target: ");
            int paramIndex = scanByteVal();
            if (error()) {
                return;
            }
            int boundIndex = scanByteVal();
            if (error()) {
                return;
            }
            environment.traceln("{ param_index: " + paramIndex + " bound_index: " + boundIndex + "}");
            targetInfo = new type_parameter_bound_target(targetType, paramIndex, boundIndex);
        }

        @Override
        public void visit_empty_target(ETargetType targetType) {
            environment.traceln("Empty Target: ");
            if (!error()) {
                targetInfo = new empty_target(targetType);
            }
        }

        @Override
        public void visit_methodformalparam_target(ETargetType targetType) {
            environment.trace("MethodFormalParam Target: ");
            int paramIndex = scanByteVal();
            environment.traceln("{ param_index: " + paramIndex + "}");
            if (!error()) {
                targetInfo = new formal_parameter_target(targetType, paramIndex);
            }
        }

        @Override
        public void visit_throws_target(ETargetType targetType) {
            environment.trace("Throws Target: ");
            int exceptionIndex = scanShortVal();
            environment.traceln("{ exception_index: " + exceptionIndex + "}");
            if (!error()) {
                targetInfo = new throws_target(targetType, exceptionIndex);
            }
        }

        @Override
        public void visit_localvar_target(ETargetType targetType) {
            environment.traceln("LocalVar Target: ");
            int tableLength = scanShortVal(); // table length (short)
            if (error()) {
                return;
            }
            localvar_target locvartab = new localvar_target(targetType, tableLength);
            targetInfo = locvartab;

            for (int i = 0; i < tableLength; i++) {
                int startPC = scanShortVal();
                if (error()) {
                    return;
                }
                int length = scanShortVal();
                if (error()) {
                    return;
                }
                int CPX = scanShortVal();
                environment.trace("LocalVar[" + i + "]: ");
                environment.traceln("{ startPC: " + startPC + ", length: " + length + ", CPX: " + CPX + "}");
                locvartab.addEntry(startPC, length, CPX);
            }
        }

        @Override
        public void visit_catch_target(ETargetType targetType) {
            environment.trace("Catch Target: ");
            int catchIndex = scanShortVal();
            environment.traceln("{ catch_index: " + catchIndex + "}");
            if (!error()) {
                targetInfo = new catch_target(targetType, catchIndex);
            }
        }

        @Override
        public void visit_offset_target(ETargetType targetType) {
            environment.trace("Offset Target: ");
            int offsetIndex = scanShortVal();
            environment.traceln("{ offset_index: " + offsetIndex + "}");
            if (!error()) {
                targetInfo = new offset_target(targetType, offsetIndex);
            }
        }

        @Override
        public void visit_typearg_target(ETargetType targetType) {
            environment.trace("TypeArg Target: ");
            int offset = scanShortVal();
            if (error()) {
                return;
            }
            int typeIndex = scanByteVal();
            if (error()) {
                return;
            }
            environment.traceln("{ offset: " + offset + " type_index: " + typeIndex + "}");
            targetInfo = new type_argument_target(targetType, offset, typeIndex);
        }
    }
}
