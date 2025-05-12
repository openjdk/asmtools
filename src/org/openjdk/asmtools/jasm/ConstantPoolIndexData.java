/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import static org.openjdk.asmtools.jdis.Indenter.UNDEFINED;

/**
 * Writable element of the DataVector u2 vector[elements];
 * <p>
 * u2 unset_fields[number_of_unset_fields];
 * Each entry in the unset_fields array must be a valid index into the constant_pool table.
 * The constant_pool entry at that index must be a CONSTANT_NameAndType_info structure with a field descriptor.
 */
public class ConstantPoolIndexData implements DataWriter {

    ConstCell nameAndType = null;
    ConstantPool pool = null;
    int cpIndex = UNDEFINED;

    public ConstantPoolIndexData(ConstCell nameAndType, ConstantPool pool) {
        this.nameAndType = nameAndType;
        this.pool = pool;
    }


    public ConstantPoolIndexData(int cpIndex) {
        this.cpIndex = cpIndex;
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        if (cpIndex == UNDEFINED) {
            cpIndex = nameAndType.cpIndex;
        }
        if (cpIndex == UNDEFINED) {
            throw new IOException("a file due to implementation issue:\n\t\t \"Can't retrieve CP Index for %s\"".
                    formatted(nameAndType == null ? "null" : nameAndType.toString()));
        }
        out.writeShort(cpIndex);
    }

    @Override
    public int getLength() {
        // u2
        return 2;
    }
}
