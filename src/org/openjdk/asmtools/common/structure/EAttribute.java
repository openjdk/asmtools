/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.common.structure;

import org.openjdk.asmtools.jasm.ClassFileConst;

import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType.*;

/**
 * 4.7. Attributes
 */
public enum EAttribute {

    // Constant for JVMS
    ATT_Unrecognized(-1, "ATT_Unrecognized", "", CONSTANT_UNKNOWN),
    ATT_StackMap(1, "ATT_StackMap", "StackMap", CONSTANT_UNKNOWN),
    // Numbers correspond to VM spec (chapter 4.7.X)
    ATT_ConstantValue(2, "ATT_ConstantValue", "ConstantValue", CONSTANT_UTF8),
    ATT_Code(3, "ATT_Code", "Code", CONSTANT_UNKNOWN),
    ATT_StackMapTable(4, "ATT_StackMapTable", "StackMapTable", CONSTANT_UNKNOWN),
    ATT_Exceptions(5, "ATT_Exceptions", "Exceptions", CONSTANT_UNKNOWN),
    ATT_InnerClasses(6, "ATT_InnerClasses", "InnerClasses", CONSTANT_CLASS),
    ATT_EnclosingMethod(7, "ATT_EnclosingMethod", "EnclosingMethod", CONSTANT_CLASS),
    ATT_Synthetic(8, "ATT_Synthetic", "Synthetic", CONSTANT_UNKNOWN),
    ATT_Signature(9, "ATT_Signature", "Signature", CONSTANT_UTF8),
    ATT_SourceFile(10, "ATT_SourceFile", "SourceFile", CONSTANT_UTF8),
    ATT_SourceDebugExtension(11, "ATT_SourceDebugExtension", "SourceDebugExtension", CONSTANT_UTF8),
    ATT_LineNumberTable(12, "ATT_LineNumberTable", "LineNumberTable", CONSTANT_UNKNOWN),
    ATT_LocalVariableTable(13, "ATT_LocalVariableTable", "LocalVariableTable", CONSTANT_UNKNOWN),
    ATT_LocalVariableTypeTable(14, "ATT_LocalVariableTypeTable", "LocalVariableTypeTable", CONSTANT_UNKNOWN),
    ATT_Deprecated(15, "ATT_Deprecated", "Deprecated", CONSTANT_UNKNOWN),
    ATT_RuntimeVisibleAnnotations(16, "ATT_RuntimeVisibleAnnotations", "RuntimeVisibleAnnotations", CONSTANT_UNKNOWN),
    ATT_RuntimeInvisibleAnnotations(17, "ATT_RuntimeInvisibleAnnotations", "RuntimeInvisibleAnnotations", CONSTANT_UNKNOWN),
    ATT_RuntimeVisibleParameterAnnotations(18, "ATT_RuntimeVisibleParameterAnnotations", "RuntimeVisibleParameterAnnotations", CONSTANT_UNKNOWN),
    ATT_RuntimeInvisibleParameterAnnotations(19, "ATT_RuntimeInvisibleParameterAnnotations", "RuntimeInvisibleParameterAnnotations",CONSTANT_UNKNOWN ),
    ATT_AnnotationDefault(20, "ATT_AnnotationDefault", "AnnotationDefault", CONSTANT_UNKNOWN),
    ATT_BootstrapMethods(21, "ATT_BootstrapMethods", "BootstrapMethods", CONSTANT_METHODHANDLE),
    ATT_RuntimeVisibleTypeAnnotations(22, "ATT_RuntimeVisibleTypeAnnotations", "RuntimeVisibleTypeAnnotations", CONSTANT_UNKNOWN),
    ATT_RuntimeInvisibleTypeAnnotations(23, "ATT_RuntimeInvisibleTypeAnnotations", "RuntimeInvisibleTypeAnnotations", CONSTANT_UNKNOWN),
    ATT_MethodParameters(24, "ATT_MethodParameters", "MethodParameters", CONSTANT_UTF8),
    ATT_Module(25, "ATT_Module", "Module", CONSTANT_MODULE),
    ATT_Version(26, "ATT_Version", "Version", CONSTANT_UTF8),
    ATT_TargetPlatform(27, "ATT_TargetPlatform", "TargetPlatform", CONSTANT_UTF8),
    ATT_MainClass(28, "ATT_MainClass", "MainClass", CONSTANT_CLASS),
    ATT_ModulePackages(29, "ATT_ModulePackages", "ModulePackages", CONSTANT_PACKAGE),
    ATT_ModuleMainClass(30, "ATT_ModuleMainClass", "ModuleMainClass", CONSTANT_CLASS),
    // ATT_ModuleTarget(31, "ATT_ModuleTarget", "ModuleTarget", CONSTANT_UNKNOWN),
    // JEP 181: class file 55.0
    ATT_NestHost(32, "ATT_NestHost", "NestHost", CONSTANT_CLASS),
    ATT_NestMembers(33, "ATT_NestMembers", "NestMembers", CONSTANT_CLASS),
    //  JEP 359 Record(Preview): class file 58.65535
    //  Record_attribute {
    //    u2 attribute_name_index;
    //    u4 attribute_length;
    //    u2 components_count;
    //    component_info components[components_count];
    // }
    ATT_Record(34, "ATT_Record", "Record", CONSTANT_UTF8),
    // JEP 360 (Sealed types): class file 59.65535
    // PermittedSubclasses_attribute {
    //    u2 attribute_name_index;
    //    u4 attribute_length;
    //    u2 number_of_classes;
    //    u2 classes[number_of_classes];
    // }
    ATT_PermittedSubclasses(35, "ATT_PermittedSubclasses", "PermittedSubclasses", CONSTANT_CLASS),
    // Valhalla
    ATT_Preload(36, "ATT_Preload", "Preload", CONSTANT_CLASS);

    private final Integer value;
    private final String printVal;
    private final String parseKey;
    private final ClassFileConst.ConstType constType;
    private static HashMap<String, EAttribute> parseKeyToTags;
    private static HashMap<Integer, EAttribute> valueToTags;

    EAttribute(Integer value, String printValue, String parseKey, ClassFileConst.ConstType constType) {
        this.value = value;
        this.printVal = printValue;
        this.parseKey = parseKey;
        this.constType = constType;
    }

    public String printValue() {
        return printVal;
    }

    public String parseKey() { return parseKey; }

    public ClassFileConst.ConstType getCPTypeOfIndex() { return constType; }

    public int value() {
        return value;
    }

    public static EAttribute get(int value) {
        if (valueToTags == null) {
            valueToTags = (HashMap<Integer, EAttribute>) Stream.
                    of(EAttribute.values()).
                    collect(Collectors.toMap(EAttribute::value, Function.identity()));
        }
        EAttribute tg = valueToTags.get(value);
        if (tg == null) {
            tg = EAttribute.ATT_Unrecognized;
        }
        return tg;
    }

    public static EAttribute get(String parseKey) {
        if (parseKeyToTags == null) {
            parseKeyToTags = (HashMap<String, EAttribute>) Stream.
                    of(EAttribute.values()).
                    collect(Collectors.toMap(EAttribute::parseKey, Function.identity()));
        }
        EAttribute tg = parseKeyToTags.get(parseKey);
        if (tg == null) {
            tg = EAttribute.ATT_Unrecognized;
        }
        return tg;
    }
}
