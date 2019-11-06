/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.jasm.JasmTokens;
import org.openjdk.asmtools.jasm.Tables;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.openjdk.asmtools.jdis.TraceUtils.traceln;

/**
 * The Record attribute data
 * <p>
 * since class file 58.65535 (JEP 359)
 */
public class RecordData {

    private final ClassData cls;
    private final boolean pr_cpx = Options.OptionObject().contains(Options.PR.CPX);
    private String initialTab = " ";
    private List<Component> components;

    public RecordData(ClassData cls) {
        this.cls = cls;
    }

    public RecordData read(DataInputStream in) throws IOException {
        int count = in.readUnsignedShort();
        traceln("components=" + count);
        components = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            components.add(new Component(cls).read(in));
        }
        return this;
    }

    /**
     * Prints the record data to the current output stream. called from ClassData.
     */
    public void print() throws IOException {
        int bound = components.size()-1;
        cls.out.println(JasmTokens.Token.RECORD.parsekey());
        for(int cn = 0; cn <= bound; cn++) {
            components.get(cn).print( cn == bound ? ";" : ",");
        }
        cls.out.println();
    }

    public boolean isEmpty() {
        return components.isEmpty();
    }


    private class Component extends MemberData {
        // CP index to the name
        private int name_cpx;
        // CP index to the descriptor
        private int desc_cpx;
        // Signature can be located in component_info
        private SignatureData signature;
        //
        private int countAttr = 0;

        public Component(ClassData cls) {
            super(cls);
            memberType = "RecordData";
        }

        @Override
        protected boolean handleAttributes(DataInputStream in, Tables.AttrTag attrtag, int attrlen) throws IOException {
            // Read the Attributes
            boolean handled = true;
            switch (attrtag) {
                case ATT_Signature:
                    if( signature != null ) {
                        traceln("Record attribute:  more than one attribute Signature are in component.attribute_info_attributes[attribute_count]");
                        traceln("Last one will be used.");
                    }
                    signature = new SignatureData(cls).read(in, attrlen);
                    break;
                default:
                    handled = false;
                    break;
            }
            return handled;
        }

        /**
         * Read and resolve the component data called from ClassData.
         */
        public Component read(DataInputStream in) throws IOException {
            // read the Component CP indexes
            name_cpx = in.readUnsignedShort();
            desc_cpx = in.readUnsignedShort();
            traceln("      RecordComponent: name[" + name_cpx + "]=" + cls.pool.getString(name_cpx)
                    + " descriptor[" + desc_cpx + "]=" + cls.pool.getString(desc_cpx));
            // Read the attributes
            readAttributes(in);
            // Calculate amount of attributes
            countAttr = (visibleAnnotations == null ? 0 : visibleAnnotations.size()) +
                    (invisibleAnnotations == null ? 0 : invisibleAnnotations.size()) +
                    (visibleTypeAnnotations == null ? 0 : visibleTypeAnnotations.size()) +
                    (invisibleTypeAnnotations == null ? 0 : invisibleTypeAnnotations.size()) +
                    (signature != null ? 1 : 0);
            return this;
        }

        public boolean isEmpty() {
            return countAttr == 0;
        }

        private void printAnnotations(String endOfComponent) {
            if( signature != null ) {
                signature.print(initialTab);
                countAttr--;
                if(countAttr != 0)
                    out.println();
            }
            if (visibleAnnotations != null) {
                for (AnnotationData visad : visibleAnnotations) {
                    // out.print(initialTab);
                    visad.print(out, initialTab);
                    countAttr--;
                    if(countAttr != 0)
                        out.println();
                }
            }
            if (invisibleAnnotations != null) {
                for (AnnotationData invisad : invisibleAnnotations) {
                    invisad.print(out, initialTab);
                    countAttr--;
                    if(countAttr != 0)
                        out.println();
                }
            }

            if (visibleTypeAnnotations != null) {
                for (TypeAnnotationData visad : visibleTypeAnnotations) {
                    visad.print(out, initialTab);
                    countAttr--;
                    if(countAttr != 0)
                        out.println();
                }
            }
            if (invisibleTypeAnnotations != null) {
                for (TypeAnnotationData invisad : invisibleTypeAnnotations) {
                    invisad.print(out, initialTab);
                    countAttr--;
                    if(countAttr != 0)
                        out.println();
                }
            }
            out.println(endOfComponent);
        }

        /**
         * Prints the component data to the current output stream. called from RecordData.
         */
        public void print(String endOfComponent) throws IOException {

            out.print(initialTab);
            if (isSynthetic) {
                out.print("synthetic ");
            }
            if (isDeprecated) {
                out.print("deprecated ");
            }

            if (pr_cpx) {
                out.print("#" + name_cpx + ":#" + desc_cpx + (isEmpty() ? endOfComponent : ""));
                out.println("\t // " + cls.pool.getName(name_cpx) + ":" + cls.pool.getName(desc_cpx));

            } else {
                out.println(cls.pool.getName(name_cpx) + ":" + cls.pool.getName(desc_cpx) + (isEmpty() ? endOfComponent : ""));
            }
            // print component's attributes
            if (!isEmpty()) {
                printAnnotations(endOfComponent);
            }
        }
    }
}
