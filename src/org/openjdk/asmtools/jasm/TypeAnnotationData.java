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
package org.openjdk.asmtools.jasm;

import java.io.IOException;
import java.util.ArrayList;
import org.openjdk.asmtools.jasm.TypeAnnotationUtils.*;

/**
 *
 */
public class TypeAnnotationData extends AnnotationData {

    protected TargetType targetType;
    protected TargetInfo targetInfo;
    protected ArrayList<TypePathEntry> targetPath;


    /*-------------------------------------------------------- */

    /*-------------------------------------------------------- */
    /* TypeAnnotationData Methods */
    public TypeAnnotationData(Argument typeCPX, boolean invisible) {
        super(typeCPX, invisible);
    }

    @Override
    public int getLength() {
        return super.getLength() + 2 + targetInfo.getLength();
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        super.write(out);
// KTL:  (1/10/13) Spec changed: char -> byte
//        out.writeShort(targetType.value);
        out.writeByte(targetType.value);
        targetInfo.write(out);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(int tabLevel) {
        StringBuilder sb = new StringBuilder();
        String tabStr = tabString(tabLevel);

        sb.append(tabStr + "Target Type: ");
        sb.append(targetType.toString());
        sb.append('\n');

        sb.append(tabStr + "Target Info: ");
        sb.append(targetInfo.toString(tabLevel));
        sb.append('\n');

        sb.append(tabStr + "Target Path: [");
        boolean first = true;
        for (TypePathEntry tpe : targetPath) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(tpe);

        }
        sb.append("]");
        sb.append('\n');

        return sb.toString();
    }

    protected static String tabString(int tabLevel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tabLevel; i++) {
            sb.append('\t');
        }

        return sb.toString();
    }

}
