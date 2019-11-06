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

import org.openjdk.asmtools.jasm.Tables;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Base class for ClassData, MethodData, FieldData and RecordData(JEP 360)
 */
public class MemberData {

    /**
     * access flags (modifiers)
     */
    protected int access;

    // flags
    protected boolean isSynthetic = false;
    protected boolean isDeprecated = false;

    /**
     * The visible annotations for this class, member( field or method) or record component
     */
    protected ArrayList<AnnotationData> visibleAnnotations;

    /**
     * The invisible annotations for this class, member( field or method) or record component
     */
    protected ArrayList<AnnotationData> invisibleAnnotations;

    /**
     * The visible annotations for this class, member( field or method) or record component
     */
    protected ArrayList<TypeAnnotationData> visibleTypeAnnotations;

    /**
     * The invisible annotations for this class, member( field or method) or record component
     */
    protected ArrayList<TypeAnnotationData> invisibleTypeAnnotations;

    /**
     * The remaining attributes of this class, member( field or method) or record component
     */
    protected ArrayList<AttrData> attrs;

    // internal references
    protected Options options = Options.OptionObject();
    protected ClassData cls;
    protected PrintWriter out;
    protected String memberType = "";

    public MemberData(ClassData cls) {
        init(cls);
    }

    public MemberData() {
    }

    public void init(ClassData cls) {
        this.out = cls.out;
        this.cls = cls;
        this.options = cls.options;
    }
    protected boolean handleAttributes(DataInputStream in, Tables.AttrTag attrtag, int attrlen) throws IOException {
        // sub-classes override
        return false;
    }

    protected void readAttributes(DataInputStream in) throws IOException {
        // Read the Attributes
        int natt = in.readUnsignedShort();
        TraceUtils.traceln("natt=" + natt);
        attrs = new ArrayList<>(natt);
        TraceUtils.traceln(memberType + " - Attributes: " + natt);
        AttrData attr;
        for (int k = 0; k < natt; k++) {
            int name_cpx = in.readUnsignedShort();
            attr = new AttrData(cls);
            attrs.add(attr);
            String attr_name = cls.pool.getString(name_cpx);
            TraceUtils.traceln("   " + memberType + ": #" + k + " name[" + name_cpx + "]=" + attr_name);
            Tables.AttrTag tag = Tables.attrtag(attr_name);
            int attrlen = in.readInt();
            switch (tag) {
                case ATT_Synthetic:
                    // Read Synthetic Attr
                    if (attrlen != 0) {
                        throw new ClassFormatError("invalid Synthetic attr length");
                    }
                    isSynthetic = true;
                    break;
                case ATT_Deprecated:
                    // Read Deprecated Attr
                    if (attrlen != 0) {
                        throw new ClassFormatError("invalid Deprecated attr length");
                    }
                    isDeprecated = true;
                    break;
                case ATT_RuntimeVisibleAnnotations:
                case ATT_RuntimeInvisibleAnnotations:
                    // Read Annotations Attr
                    int cnt = in.readShort();
                    ArrayList<AnnotationData> annots = new ArrayList<>(cnt);
                    boolean invisible = (tag == Tables.AttrTag.ATT_RuntimeInvisibleAnnotations);
                    for (int i = 0; i < cnt; i++) {
                        TraceUtils.traceln("      AnnotationData: #" + i);
                        AnnotationData annot = new AnnotationData(invisible, cls);
                        annot.read(in);
                        annots.add(annot);
                    }

                    if (invisible) {
                        invisibleAnnotations = annots;
                    } else {
                        visibleAnnotations = annots;
                    }
                    break;
                case ATT_RuntimeVisibleTypeAnnotations:
                case ATT_RuntimeInvisibleTypeAnnotations:
                    // Read Type Annotations Attr
                    int tcnt = in.readShort();
                    ArrayList<TypeAnnotationData> tannots = new ArrayList<>(tcnt);
                    boolean tinvisible = (tag == Tables.AttrTag.ATT_RuntimeInvisibleTypeAnnotations);
                    for (int tindex = 0; tindex < tcnt; tindex++) {
                        TraceUtils.traceln("      TypeAnnotationData: #" + tindex);
                        TypeAnnotationData tannot = new TypeAnnotationData(tinvisible, cls);
                        tannot.read(in);
                        tannots.add(tannot);
                    }

                    if (tinvisible) {
                        invisibleTypeAnnotations = tannots;
                    } else {
                        visibleTypeAnnotations = tannots;
                    }
                    break;
                default:
                    boolean handled = handleAttributes(in, tag, attrlen);
                    if (!handled) {
                        attr.read(name_cpx, attrlen, in);
                    }
                    break;

            }
        }
    }

}
