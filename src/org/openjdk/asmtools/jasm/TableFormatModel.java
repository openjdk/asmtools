/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.asmtools.jdis.*;
import org.openjdk.asmtools.jdis.BootstrapMethodData;
import org.openjdk.asmtools.jdis.ConstantPool;
import org.openjdk.asmtools.jdis.ExceptionData;
import org.openjdk.asmtools.jdis.FieldData;
import org.openjdk.asmtools.jdis.InnerClassData;
import org.openjdk.asmtools.jdis.LineNumberData;
import org.openjdk.asmtools.jdis.LocalVariableData;
import org.openjdk.asmtools.jdis.MethodData;
import org.openjdk.asmtools.jdis.RecordData;
import org.openjdk.asmtools.jdis.StackMapData;

/**
 * The TableFormatModel class stores and manages attributes that support a tabular (javap-like) format,
 * which can be switched on using the --table option.
 */
public class TableFormatModel {
    public enum Token {
        NOT_SUPPORTED("NotSupported", null, null, null),
        SOURCE_FILE("SourceFile", EAttribute.ATT_SourceFile, JasmTokens.Token.SOURCEFILE, SourceFileData.class),
        ENCLOSING_METHOD("EnclosingMethod", EAttribute.ATT_EnclosingMethod, JasmTokens.Token.ENCLOSINGMETHOD, EnclosingMethodData.class),
        NEST_MEMBERS("NestMembers", EAttribute.ATT_NestMembers, JasmTokens.Token.NESTMEMBERS, NestMembersData.class),
        NEST_HOST("NestHost", EAttribute.ATT_NestHost, JasmTokens.Token.NESTHOST, NestHostData.class),
        INNER_CLASSES("InnerClasses", EAttribute.ATT_InnerClasses, JasmTokens.Token.INNERCLASS, InnerClassData.class),
        SIGNATURE("Signature", EAttribute.ATT_Signature, JasmTokens.Token.SIGNATURE, SignatureData.class),
        CONSTANT_POOL("Constant pool", null, null, ConstantPool.class),
        METHOD_DATA("Method_info", null, null, MethodData.class),
        FIELD_DATA("Field_info", null, null, FieldData.class),
        RECORD_DATA("record_component_info", null, null, RecordData.class),
        LINE_NUMBERS("LineNumberTable", EAttribute.ATT_LineNumberTable, JasmTokens.Token.LINETABLE_HEADER, LineNumberData.class),
        LOCAL_VARIABLES("LocalVariableTable", EAttribute.ATT_LocalVariableTable, JasmTokens.Token.LOCALVARIABLES_HEADER, LocalVariableData.class),
        LOCAL_VARIABLE_TYPES("LocalVariableTypeTable", EAttribute.ATT_LocalVariableTypeTable, JasmTokens.Token.LOCALVARIABLETYPES_HEADER, LocalVariableTypeData.class),
        EXCEPTIONS("throws", null, JasmTokens.Token.THROWS, ExceptionData.class),
        STACK_MAP("StackMap", EAttribute.ATT_StackMap, JasmTokens.Token.STACKMAP, StackMapData.class),
        STACK_MAP_TABLE("StackMapTable", EAttribute.ATT_StackMapTable, JasmTokens.Token.STACKMAPTABLE_HEADER, StackMapData.class),
        BOOTSTRAP_METHOD("BootstrapMethods", EAttribute.ATT_BootstrapMethods, JasmTokens.Token.BOOTSTRAPMETHOD, BootstrapMethodData.class);

        final private String parseKey;
        final private EAttribute attribute;

        final private JasmTokens.Token jasmToken;
        final private String printKey;
        final private Class<? extends Indenter> owner;

        Token(String parseKey, EAttribute attribute, JasmTokens.Token jasmToken, Class<? extends Indenter> owner) {
            this.parseKey = parseKey;
            this.attribute = attribute;
            this.jasmToken = jasmToken;
            this.printKey = "%s".formatted(parseKey);
            this.owner = owner;
        }

        public String parseKey() {
            return parseKey;
        }

        public String printKey() {
            return printKey;
        }

        public boolean isExtendedPrintingSupported() {
            return owner != null;
        }

        public EAttribute getAttribute() {
            return attribute;
        }

        public JasmTokens.Token getJasmToken() {
            return jasmToken;
        }

        public static Token getBy(Class<? extends Indenter> cls) {
            for (Token item : Token.values()) {
                if (cls.equals(item.owner)) {
                    return item;
                }
            }
            return NOT_SUPPORTED;
        }
    }
}
