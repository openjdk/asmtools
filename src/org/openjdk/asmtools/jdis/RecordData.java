/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.jasm.JasmTokens;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.openjdk.asmtools.jasm.JasmTokens.Token.*;

/**
 * The Record attribute data
 * <p>
 * since class file 58.65535 (JEP 359)
 */
public class RecordData extends  MemberData<ClassData> {

    private List<Component> components;

    public RecordData(ClassData classData) {
        super(classData);
    }

    public RecordData read(DataInputStream in) throws IOException {
        int count = in.readUnsignedShort();
        environment.traceln("components=" + count);
        components = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            components.add(new Component(data).read(in));
        }
        return this;
    }

    /**
     * Prints the record data to the current output stream. called from ClassData.
     */
    public void print() throws IOException {
        int count = components.size();
        if (count > 0) {
            printIndentLn(RECORD.parseKey() + " {");
            for (int i = 0; i < count; i++) {
                Component cn = components.get(i);
                cn.setCommentOffset(getCommentOffset() + getIndentSize()).incIndent();
                if (i != 0 && cn.getAnnotationsCount() > 0)
                    cn.toolOutput.printlns("");
                cn.print();
            }
            printIndentLn("}");
        }
    }

    /**
     * record_component_info {
     *     u2             name_index;
     *     u2             descriptor_index;
     *     u2             attributes_count;
     *     attribute_info attributes[attributes_count];
     * }
     */
    private static class Component extends MemberData<ClassData> {
        // CP index to a CONSTANT_Utf8_info structure representing a valid unqualified name denoting the record component
        private int name_cpx;
        // CP index to a CONSTANT_Utf8_info structure representing a field descriptor which encodes the type of the record component (§4.3.2).
        private int descriptor_cpx;

        public Component(ClassData classData) {
            super(classData);
            memberType = "RecordData";
        }

        @Override
        protected boolean handleAttributes(DataInputStream in, EAttribute attributeTag, int attributeLength) throws IOException {
            // Read the Attributes
            boolean handled = true;
            if (attributeTag == EAttribute.ATT_Signature) {
                if (signature != null) {
                    environment.warning("warn.one.attribute.required", "Signature", "record_component_info");
                }
                signature = new SignatureData(data).read(in, attributeLength);
            } else {
                handled = false;
            }
            return handled;
        }

        /**
         * Read and resolve the component data called from ClassData.
         */
        public Component read(DataInputStream in) throws IOException {
            // read the Component CP indexes
            name_cpx = in.readUnsignedShort();
            descriptor_cpx = in.readUnsignedShort();
            environment.traceln("RecordComponent: name[" + name_cpx + "]=" + data.pool.getString(name_cpx, index->"?")
                    + " descriptor[" + descriptor_cpx + "]=" + data.pool.getString(descriptor_cpx, index->"?"));
            // Read the attributes
            readAttributes(in);
            return this;
        }

        /**
         * Prints the component data to the current output stream. called from RecordData.
         */
        public void print() throws IOException {
            // print component's attributes
                super.printAnnotations();
            // print component
            StringBuilder prefix = new StringBuilder(getIndentString());
            // add synthetic, deprecated if necessary
            prefix.append(getPseudoFlagsAsString());
            // component
            prefix.append(JasmTokens.Token.COMPONENT.parseKey()).append(' ');
            printVar(prefix, null, name_cpx, descriptor_cpx, 0);
        }
    }
}
