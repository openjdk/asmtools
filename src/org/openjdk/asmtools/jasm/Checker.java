/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.SyntaxError;
import org.openjdk.asmtools.common.structure.ClassFileContext;
import org.openjdk.asmtools.common.structure.EModifier;


import static org.openjdk.asmtools.common.structure.EModifier.*;
import static org.openjdk.asmtools.jasm.JasmTokens.Token;

/**
 * Utility methods to verify modifiers masks for classes, interfaces, methods and fields
 */
public class Checker {

    private Checker() {}

    /*
     * Check that only one of the Access flags is set.
     */
    private static boolean validAccess(int mod) {
        return EModifier.onlyOneOfFlags(mod, ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED);
    }

    public static boolean validNestedClass(int flags) {
        return (flags & ~getFlags(MM_NESTED_CLASS)) == 0;
    }

    public static boolean validField(int flags) {
        return (flags & ~getFlags(MM_FIELD)) == 0;
    }

    /*
     * Methods of classes may have any of the flags in Table 4.6-A set. (MM_METHOD)
     * However, each method of a class may have at most one of its ACC_PUBLIC, ACC_PRIVATE, and ACC_PROTECTED flags set (JLS §8.4.3).
     */
    private static boolean validMethod(int mod) {
        return noFlagsExcept(mod, MM_METHOD);
    }

    public static boolean validAbstractMethod(int flags) {

        return (flags & ~getFlags(MM_ABSTRACT_METHOD)) == 0;
    }

    public static boolean validInitMethod(int flags) {
        return (flags & ~getFlags(MM_INIT_METHOD)) == 0;
    }

    /*
     * Fields of interfaces must have their ACC_PUBLIC, ACC_STATIC, and ACC_FINAL flags set;
     * they may have their ACC_SYNTHETIC flag set and must not have any of the other flags in Table 4.5-A set (JLS §9.3).
     */
    public static boolean validInterfaceField(int mod) {
        final int flags = isSynthetic(mod) ? mod & ~ACC_SYNTHETIC.getFlag() : mod;
        return noFlagsExcept(flags, ACC_PUBLIC, ACC_STATIC, ACC_FINAL);
    }

    /**
     * The only flags are allowed in interface: ACC_PUBLIC, ACC_INTERFACE, ACC_ABSTRACT, ACC_SYNTHETIC, ACC_ANNOTATION
     */
    public static boolean validInterface(int mod) {
        return noFlagsExcept(mod, MM_INTERFACE);
    }

    /*
     * The only flags allowed to the Class are set.
     */
    public static boolean validClass(int mod) {
        return noFlagsExcept(mod, MM_CLASS);
    }

    /**
     * Check the modifier flags for the class
     *
     * @param mod     The modifier flags being checked
     * @param scanner The file parser
     */
    public static void checkClassModifiers(int mod, Scanner scanner) {
        if( scanner.token != Token.CLASS && ! EModifier.isInterface(mod) ) {
            scanner.environment.warning(scanner.pos, "warn.one.of.two.token.expected", Token.CLASS.parseKey(), Token.INTERFACE.parseKey());
        }
        mod = EModifier.cleanFlags(mod, DEPRECATED_ATTRIBUTE, SYNTHETIC_ATTRIBUTE);
// Interface
        if (isInterface(mod)) {
            // If the ACC_INTERFACE flag is set, the ACC_ABSTRACT flag must also be set.
            if (!isAbstract(mod)) {
                scanner.environment.warning(scanner.pos, "warn.invalid.modifier.int.abs",
                        EModifier.asNames(mod, ClassFileContext.CLASS));
            }
            // If the ACC_INTERFACE flag is set, the ACC_FINAL, ACC_PRIMITIVE, ACC_ENUM, and ACC_MODULE flags must not be set.
            if (anyOf(mod, ACC_FINAL, ACC_PRIMITIVE, ACC_ENUM, ACC_MODULE)) {
                scanner.environment.warning(scanner.pos, "warn.invalid.modifier.interface.set",
                        EModifier.asNames(mod, ClassFileContext.CLASS));
            }
            if (!validInterface(mod)) {
                scanner.environment.warning(scanner.pos, "warn.invalid.modifier.int",
                        EModifier.asNames(mod & ~getFlags(MM_INTERFACE), ClassFileContext.CLASS));
            }
            if (isEnum(mod)) {
                scanner.environment.warning(scanner.pos, "warn.invalid.modifier.class.intenum",
                        EModifier.asNames(mod, ClassFileContext.CLASS));
            }
// class
        } else {
            if (!validClass(mod)) {
                scanner.environment.warning(scanner.pos, "warn.invalid.modifier.class",
                        EModifier.asNames(mod & ~getFlags(MM_CLASS), ClassFileContext.CLASS));
            }
        }
// any
        if (both(mod, ACC_ABSTRACT, ACC_FINAL)) {
            scanner.environment.warning(scanner.pos, "warn.invalid.modifier.class.finabs",
                    EModifier.asNames(mod, ClassFileContext.CLASS));
        }
    }

    /**
     * Check the modifier flags for the field
     *
     * @param classData  The ClassData for the current class
     * @param mod The modifier flags being checked
     * @param pos the position of the parser in the file
     */
    public static void checkFieldModifiers(ClassData classData, int mod, int pos) {
        JasmEnvironment environment = classData.getEnvironment();
        mod = EModifier.cleanFlags(mod, DEPRECATED_ATTRIBUTE, SYNTHETIC_ATTRIBUTE);
        if (classData.isInterface()) { // For interfaces fields.
            // Fields of interfaces must have their ACC_PUBLIC, ACC_STATIC, and ACC_FINAL flags set;
            // they may have their ACC_SYNTHETIC flag set and must not have any of the other flags in Table 4.5-A set (JLS §9.3).
            if (!validInterfaceField(mod)) {
                environment.warning(pos, "warn.invalid.modifier.intfield",
                        EModifier.asNames(mod, ClassFileContext.FIELD));
            }
        } else { // For non-interface fields.
            //Fields of classes may set any of the flags in Table 4.5-A.
            // However, each field of a class may have at most one of its ACC_PUBLIC, ACC_PRIVATE, and ACC_PROTECTED flags set (JLS §8.3.1),
            // and must not have both its ACC_FINAL and ACC_VOLATILE flags set (JLS §8.3.1.4).
            if (!validField(mod)) {
                environment.warning(pos, "warn.invalid.modifier.field",
                        EModifier.asNames(mod & ~getFlags(MM_FIELD), ClassFileContext.FIELD));
            }
            if (!validAccess(mod)) {
                environment.warning(pos, "warn.invalid.modifier.acc",
                        EModifier.asNames(mod, ClassFileContext.FIELD));
            }
            if (both(mod, ACC_FINAL, ACC_VOLATILE)) {
                environment.warning(pos, "warn.invalid.modifier.fiva",
                        EModifier.asNames(mod, ClassFileContext.FIELD));
            }
            // In a primitive class, each field must have at least one of its ACC_STATIC or ACC_FINAL flags set.
            if (classData.isPrimitive()) {
                if (!EModifier.anyOf(mod, ACC_STATIC, ACC_FINAL) || !EModifier.both(mod, ACC_STATIC, ACC_FINAL)) {
                    environment.warning(pos, "warn.invalid.modifier.primitive.flags",
                            EModifier.asNames(mod, ClassFileContext.FIELD));
                }
                // In an abstract class, each field must have its ACC_STATIC flag set.
                if (classData.isAbstract() && !isStatic(mod)) {
                    environment.warning(pos, "warn.invalid.modifier.primitive.abstract",
                            EModifier.asNames(mod, ClassFileContext.FIELD));
                }
            }
        }
    }

    /**
     * Check the modifier flags for the method
     *
     * @param classData  The ClassData for the current class
     * @param mod The modifier flags being checked
     * @param pos the position of the parser in the file
     * @param isInit is the method constructor
     * @param isClinit is the method static initializer
     */
    public static void checkMethodModifiers(ClassData classData, int mod, int pos, boolean isInit, boolean isClinit) {
        final JasmEnvironment environment = classData.getEnvironment();
        final int cfvMajorVersion = classData.cfv.major_version();
        mod = EModifier.cleanFlags(mod, DEPRECATED_ATTRIBUTE, SYNTHETIC_ATTRIBUTE);
        int wrongFlags;
        if (!validMethod(mod)) {
            wrongFlags = mod & ~EModifier.getFlags(MM_METHOD);
            environment.warning(pos, "warn.invalid.modifier.mth",
                    EModifier.asNames(wrongFlags, ClassFileContext.METHOD));
        }
        if (!validAccess(mod)) {
            environment.warning(pos, "warn.invalid.modifier.mth.acc",
                    EModifier.asNames(mod, ClassFileContext.METHOD));
        }
        if (isClinit) {    // <clinit>
            // In a class file whose version number is 51.0 or above, a method whose name is <clinit> must have its ACC_STATIC flag set.
            if (classData.cfv.major_version() > 51 && !isStatic(mod)) {
                environment.warning(pos, "warn.clinit.static",
                        EModifier.asNames(mod, ClassFileContext.METHOD));
            }
        } else {            // any method
            // interface methods.
            if (classData.isInterface()) {
                if (isInit) {
                    environment.warning(pos, "warn.init.in_int");
                } else {
                    validateInterfaceMethod(mod, classData, pos);
                }
            // class methods
            } else {
                if (isInit && !validInitMethod(mod)) {
                    wrongFlags = mod & ~EModifier.getFlags(MM_INIT_METHOD);
                    environment.warning(pos, "warn.invalid.modifier.init",
                            EModifier.asNames(wrongFlags, ClassFileContext.METHOD));
                } else if (isAbstract(mod)) {
                    if ( !validAbstractMethod(mod) ) {
                        wrongFlags = mod & ~EModifier.getFlags(MM_ABSTRACT_METHOD);
                        environment.warning(pos, "warn.invalid.modifier.abst",
                                EModifier.asNames(wrongFlags, ClassFileContext.METHOD));
                    } else if (isStrict(mod) &&  (cfvMajorVersion >= 46 && cfvMajorVersion <= 60) ) {
                        environment.warning(pos, "warn.invalid.modifier.strict");
                    }
                }
            }
        }
    }

    /**
     * Check the modifier flags for the inner-class
     *
     * @param classData  The ClassData for the current class
     * @param mod The modifier flags being checked
     * @param pos the position of the parser in the file
     */
    public static void checkInnerClassModifiers(ClassData classData, int mod, int pos) {
        JasmEnvironment environment = classData.getEnvironment();
        mod = EModifier.cleanFlags(mod, DEPRECATED_ATTRIBUTE, SYNTHETIC_ATTRIBUTE);
        if (!validNestedClass(mod)) {
            int wrongFlags = mod & ~EModifier.getFlags(MM_NESTED_CLASS);
            environment.warning(pos, "warn.invalid.modifier.innerclass",
                    EModifier.asNames(wrongFlags, ClassFileContext.INNER_CLASS));
        }
    }

    // Methods of interfaces may have any of the flags in Table 4.6-A set except ACC_PROTECTED, ACC_FINAL, ACC_SYNCHRONIZED,
    // and ACC_NATIVE (JLS §9.4). In a class file whose version number is less than 52.0, each method of an interface
    // must have its ACC_PUBLIC and ACC_ABSTRACT flags set; in a class file whose version number is 52.0 or above,
    // each method of an interface must have exactly one of its ACC_PUBLIC and ACC_PRIVATE flags set.
    public static void validateInterfaceMethod(int mod, ClassData cd, int pos) {
        final int cfvMajorVersion = cd.cfv.major_version();
        final JasmEnvironment environment = cd.getEnvironment();
        if (EModifier.anyOf(mod, ACC_PROTECTED, ACC_FINAL, ACC_SYNCHRONIZED, ACC_NATIVE)) {
            environment.warning(pos, "warn.invalid.modifier.method",
                    EModifier.asNames(mod, ClassFileContext.METHOD));
        }
        if (cfvMajorVersion < 52) {
            if (!both(mod, ACC_PUBLIC, ACC_ABSTRACT)) {
                environment.warning(pos, "warn.invalid.modifier.intmth.less.52",
                        EModifier.asNames(mod, ClassFileContext.METHOD));
            } else { // cfvMajorVersion >= 52
                if (!EModifier.onlyOneOfFlags(mod, EModifier.ACC_PUBLIC, ACC_PRIVATE)) {
                    environment.warning(pos, "warn.invalid.modifier.intmth.is.52",
                            EModifier.asNames(mod, ClassFileContext.METHOD));
                }
            }
        }
    }
}
