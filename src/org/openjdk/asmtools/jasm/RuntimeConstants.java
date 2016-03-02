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

/**
 *
 */
public interface RuntimeConstants {

    /* Signature Characters */
    public static final char   SIGC_VOID                  = 'V';
    public static final String SIG_VOID                   = "V";
    public static final char   SIGC_BOOLEAN               = 'Z';
    public static final String SIG_BOOLEAN                = "Z";
    public static final char   SIGC_BYTE                  = 'B';
    public static final String SIG_BYTE                   = "B";
    public static final char   SIGC_CHAR                  = 'C';
    public static final String SIG_CHAR                   = "C";
    public static final char   SIGC_SHORT                 = 'S';
    public static final String SIG_SHORT                  = "S";
    public static final char   SIGC_INT                   = 'I';
    public static final String SIG_INT                    = "I";
    public static final char   SIGC_LONG                  = 'J';
    public static final String SIG_LONG                   = "J";
    public static final char   SIGC_FLOAT                 = 'F';
    public static final String SIG_FLOAT                  = "F";
    public static final char   SIGC_DOUBLE                = 'D';
    public static final String SIG_DOUBLE                 = "D";
    public static final char   SIGC_ARRAY                 = '[';
    public static final String SIG_ARRAY                  = "[";
    public static final char   SIGC_CLASS                 = 'L';
    public static final String SIG_CLASS                  = "L";
    public static final char   SIGC_METHOD                = '(';
    public static final String SIG_METHOD                 = "(";
    public static final char   SIGC_ENDCLASS              = ';';
    public static final String SIG_ENDCLASS               = ";";
    public static final char   SIGC_ENDMETHOD             = ')';
    public static final String SIG_ENDMETHOD              = ")";
    public static final char   SIGC_PACKAGE               = '/';
    public static final String SIG_PACKAGE                = "/";

    /* Class File Constants */
//    public static final int JAVA_MAGIC                   = 0xcafebabe;
    public static final int JAVA_VERSION                 = 45;
    public static final int JAVA_MINOR_VERSION           = 3;
    /* Access Flags */

    public static final int ACC_NONE          = 0x0000; // <<everywhere>>
    public static final int ACC_PUBLIC        = 0x0001; // class, inner, field, method
    public static final int ACC_PRIVATE       = 0x0002; //        inner, field, method
    public static final int ACC_PROTECTED     = 0x0004; //        inner, field, method
    public static final int ACC_STATIC        = 0x0008; //        inner, field, method
    public static final int ACC_FINAL         = 0x0010; // class, inner, field, method
    public static final int ACC_SUPER         = 0x0020; // class
    public static final int ACC_REEXPORT      = 0x0020; //                             requires (ACC_PUBLIC)
    public static final int ACC_SYNCHRONIZED  = 0x0020; //                      method
    public static final int ACC_VOLATILE      = 0x0040; //               field
    public static final int ACC_BRIDGE        = 0x0040; //                      method
    public static final int ACC_TRANSIENT     = 0x0080; //               field
    public static final int ACC_VARARGS       = 0x0080; //                      method
    public static final int ACC_NATIVE        = 0x0100; //                      method
    public static final int ACC_INTERFACE     = 0x0200; // class, inner
    public static final int ACC_ABSTRACT      = 0x0400; // class, inner,        method
    public static final int ACC_STRICT        = 0x0800; //                      method
    public static final int ACC_SYNTHETIC     = 0x1000; // class, inner, field, method requires
    public static final int ACC_ANNOTATION    = 0x2000; // class, inner
    public static final int ACC_ENUM          = 0x4000; // class, inner, field
    public static final int ACC_MODULE        = 0x8000; // class
    public static final int ACC_MANDATED      = 0x8000; //                      method requires

    /* Attribute codes */
    public static final int SYNTHETIC_ATTRIBUTE          = 0x00010000; // actually, this is an attribute
    public static final int DEPRECATED_ATTRIBUTE         = 0x00020000; // actually, this is an attribute
    /* The version of a class file since which the compact format of stack map
     * is necessary */
    public final int SPLIT_VERIFIER_CFV = 50;

}
