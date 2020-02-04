/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.IOException;

/**
 * The NestMembers attribute data
 * <p>
 * JEP 360 (Sealed types): class file 59.65535
 * PermittedSubtypes_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 permitted_subtypes_count;
 * u2 classes[permitted_subtypes_count];
 * }
 */
public class PermittedSubtypesData extends ClassArrayData {
    public PermittedSubtypesData(ClassData cls) {
        super(cls, JasmTokens.Token.PERMITTEDSUBTYPES.parsekey());
    }

    public PermittedSubtypesData read(DataInputStream in, int attribute_length) throws IOException, ClassFormatError {
        return (PermittedSubtypesData) super.read(in, attribute_length);
    }
}
