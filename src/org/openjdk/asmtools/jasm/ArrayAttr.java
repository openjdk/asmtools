/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.structure.EAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class of the "classes/descriptors[]" data of attributes
 * <p>
 * JEP 181 (Nest-based Access Control): class file 55.0
 * NestMembers_attribute {
 *      u2 attribute_name_index;
 *      u4 attribute_length;
 *      u2 number_of_classes;
 *      u2 classes[number_of_classes];
 * }
 * <p>
 * JEP 360 (Sealed types): class file 59.65535
 * PermittedSubclasses_attribute {
 *      u2 attribute_name_index;
 *      u4 attribute_length;
 *      u2 number_of_classes;
 *      u2 classes[number_of_classes];
 * }
 * <p>
 * Valhalla:
 * LoadableDescriptors_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u2 number_of_descriptors;
 *     u2 descriptors[number_of_descriptors];
 * }
 */
public class ArrayAttr extends AttrData {

    List<ConstCell> cells = new ArrayList<>();

    public ArrayAttr(ConstantPool pool, EAttribute attribute, List<ConstCell> constCellList) {
        super(pool, attribute);
        for (ConstCell<?> cell : constCellList) {
            this.cells.add(classifyConstCell(pool, cell));
        }
    }

    public int size() {
        return cells.size();
    }

    @Override
    public int attrLength() {
        return 2 + cells.size() * 2;
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        super.write(out);
        out.writeShort(cells.size());
        for (ConstCell c : cells) {
            out.writeShort(c.cpIndex);
        }
    }
}
