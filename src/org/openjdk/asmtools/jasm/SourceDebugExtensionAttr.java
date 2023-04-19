/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.openjdk.asmtools.asmutils.HexUtils.toByteArray;

/**
 * SourceDebugExtension_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u1 debug_extension[attribute_length];
 * The debug_extension array holds extended debugging information which has no semantic effect on the JVM.
 * The information is represented using a modified UTF-8 string (§4.4.7) with no terminating zero byte.
 * }
 */
public class SourceDebugExtensionAttr extends AttrData {

    StringBuilder utf8DebugExtension = new StringBuilder();
    List<Byte> byteDebugExtension = new ArrayList<>();

    // Defines type of the attribute either it is presented as UTF8 string or Byte array.
    public enum Type {
        NONE, UTF8, BYTE;
    }

    // by default the type isn't defined. First append operation will define the type.
    Type type = Type.NONE;

    SourceDebugExtensionAttr(ConstantPool pool) {
        super(pool, EAttribute.ATT_SourceDebugExtension);
    }

    public boolean isEmpty() {
        return switch (type) {
            case UTF8 -> utf8DebugExtension.isEmpty();
            case BYTE -> byteDebugExtension.isEmpty();
            default -> throw new RuntimeException("SourceDebugExtension_attribute is not initialized");
        };
    }

    public void append(String str) throws IllegalArgumentException {
        if (type == Type.BYTE)
            throw new IllegalArgumentException("The valid modified UTF-8 string is expected.");
        utf8DebugExtension.append(str);
        type = Type.UTF8;
    }

    public void append(int value) {
        if (type == Type.UTF8)
            throw new IllegalArgumentException("The byte representation of the string is expected.");
        byteDebugExtension.add((byte) value);
        type = Type.BYTE;
    }

    public int attrLength() {
        // 4.4.7. The CONSTANT_Utf8_info Structure
        // u2 length; + (u1 bytes[length]).length();
        return switch (type) {
            case UTF8 -> 2 + utf8DebugExtension.toString().getBytes(UTF_8).length;
            case BYTE -> byteDebugExtension.size();
            default -> throw new RuntimeException("SourceDebugExtension_attribute is not initialized");
        };
    }

    public void write(CheckedDataOutputStream out) throws IOException {
        super.write(out);  // attr name, attr length
        switch (type) {
            case UTF8 -> out.writeUTF(utf8DebugExtension.toString());
            case BYTE -> out.write(toByteArray(byteDebugExtension));
            default -> throw new RuntimeException("SourceDebugExtension_attribute is not initialized");
        }
        ;
    }
}
