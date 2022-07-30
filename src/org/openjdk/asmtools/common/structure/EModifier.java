/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.openjdk.asmtools.common.structure.ClassFileContext.*;

/**
 * 4. The class File Format
 * <p>
 * The enum contains all JVMS (class, field, method, nested class, module and module statements) accesses and modifiers
 * taken from tables 4.1-A, 4.1-B, 4.5-A, 4.6-A, 4.7.6-A, 4.7.25 etc according to their context @see EClassFileContext
 */
public enum EModifier {

    ACC_NONE(0x0000, "", NONE),                                 // <<everywhere>>
    ACC_PUBLIC(0x0001, "public", CLASS, INNER_CLASS, FIELD, METHOD),
    ACC_PRIVATE(0x0002, "private", INNER_CLASS, FIELD, METHOD),
    ACC_PROTECTED(0x0004, "protected", INNER_CLASS, FIELD, METHOD),
    ACC_STATIC(0x0008, "static", INNER_CLASS, FIELD, METHOD),

    ACC_FINAL(0x0010, "final", CLASS, INNER_CLASS, FIELD, METHOD, METHOD_PARAMETERS),

    ACC_SUPER(0x0020, "super", CLASS),   // although this seems to be easily ignored, but not including it to the class, where it originally was,
                                                      // will cause running hotswap to fail, with
                                                      //java.lang.UnsupportedOperationException: class redefinition failed: attempted to change the class modifiers

    ACC_TRANSITIVE(0x0020, "transitive", REQUIRES),
    ACC_SYNCHRONIZED(0x0020, "synchronized", METHOD),
    ACC_OPEN(0x0020, "open", MODULE),

    ACC_VOLATILE(0x0040, "volatile", FIELD),
    ACC_BRIDGE(0x0040, "bridge", METHOD),
    ACC_STATIC_PHASE(0x0040, "static", REQUIRES),
    ACC_PERMITS_VALUE(0x0040, "permits_value", CLASS, INNER_CLASS),       // valhalla

    ACC_TRANSIENT(0x0080, "transient", FIELD),
    ACC_VARARGS(0x0080, "varargs", METHOD),

    ACC_NATIVE(0x0100, "native", METHOD),
    ACC_VALUE(0x0100, "value", CLASS, INNER_CLASS),                         // valhalla

    ACC_INTERFACE(0x0200, "interface", CLASS, INNER_CLASS),

    ACC_ABSTRACT(0x0400, "abstract", CLASS, INNER_CLASS, METHOD),

    ACC_STRICT(0x0800, "strict", METHOD),
    ACC_PRIMITIVE(0x0800, "primitive", CLASS, INNER_CLASS),                  // valhalla

    ACC_SYNTHETIC(0x1000, "synthetic", CLASS, INNER_CLASS, FIELD, METHOD, MODULE, REQUIRES, EXPORTS, OPENS, METHOD_PARAMETERS),

    ACC_ANNOTATION(0x2000, "annotation", CLASS, INNER_CLASS),

    ACC_ENUM(0x4000, "enum", CLASS, INNER_CLASS, FIELD),

    ACC_MODULE(0x8000, "module", CLASS),
    ACC_MANDATED(0x8000, "mandated", MODULE, REQUIRES, EXPORTS, OPENS, METHOD_PARAMETERS),

    SYNTHETIC_ATTRIBUTE(0x00010000, "Synthetic(Pseudo)", CLASS, INNER_CLASS, FIELD, METHOD),
    DEPRECATED_ATTRIBUTE(0x00020000, "Deprecated(Pseudo)", CLASS, INNER_CLASS, FIELD, METHOD);

    // Method access and property flags (Table 4.6-A)
    public static final EModifier[] MM_METHOD = {ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED, ACC_STATIC, ACC_FINAL,
            ACC_SYNCHRONIZED, ACC_BRIDGE, ACC_VARARGS, ACC_NATIVE, ACC_ABSTRACT,
            ACC_STRICT, ACC_SYNTHETIC};
    // Class access and property modifiers (Table 4.1-A)
    public static final EModifier[] MM_CLASS = {ACC_PUBLIC, ACC_FINAL, ACC_SUPER,
            ACC_PRIMITIVE, ACC_INTERFACE, ACC_ABSTRACT, ACC_SYNTHETIC,
            ACC_ANNOTATION, ACC_ENUM, ACC_MODULE, ACC_VALUE, ACC_PERMITS_VALUE, ACC_PRIMITIVE};

    // Valid interface flags.
    public static final EModifier[] MM_INTERFACE = {ACC_PUBLIC, ACC_INTERFACE,
            ACC_ABSTRACT, ACC_SYNTHETIC, ACC_ANNOTATION};
    // Field access and property flags (Table 4.5-A)
    public static final EModifier[] MM_FIELD = {ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED,
            ACC_STATIC, ACC_FINAL, ACC_VOLATILE, ACC_TRANSIENT,
            ACC_SYNTHETIC, ACC_ENUM};
    // Abstract method
    public static final EModifier[] MM_ABSTRACT_METHOD = {ACC_PUBLIC, ACC_PROTECTED, ACC_BRIDGE, ACC_VARARGS, ACC_ABSTRACT,
            ACC_SYNTHETIC};
    // <init>, <clinit> method
    public static final EModifier[] MM_INIT_METHOD = {ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED, ACC_VARARGS, ACC_SYNTHETIC,
            ACC_STRICT, ACC_STATIC};
    //  Nested class access and property flags  (Table 4.7.6-A)
    public static final EModifier[] MM_NESTED_CLASS = {ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED, ACC_STATIC, ACC_FINAL,
            ACC_INTERFACE, ACC_ABSTRACT, ACC_SYNTHETIC, ACC_ANNOTATION, ACC_ENUM};
    // Interface method
    private static final EModifier[] MM_INTERFACE_METHOD = {ACC_PUBLIC, ACC_PRIVATE, ACC_STATIC, ACC_BRIDGE, ACC_VARARGS,
            ACC_ABSTRACT, ACC_STRICT, ACC_SYNTHETIC};
    private static final EModifier[] MM_MODULE = {ACC_OPEN, ACC_SYNTHETIC, ACC_MANDATED};
    private static final EModifier[] MM_MODULE_REQUIRES = {ACC_TRANSITIVE, ACC_STATIC_PHASE, ACC_SYNTHETIC, ACC_MANDATED};
    private static final EModifier[] MM_MODULE_EXPORTS = {ACC_SYNTHETIC, ACC_MANDATED};
    private static final EModifier[] MM_MODULE_OPENS = {ACC_SYNTHETIC, ACC_MANDATED};
    // ToString converters
    public static String NAMES_DELIMITER = ", ";
    public static String NAMES_SUFFIX = " ";
    public static String KEYWORDS_DELIMITER = " ";
    public static String KEYWORDS_SUFFIX = " ";
    private final int flag;
    private final String keyword;
    private final Set<ClassFileContext> contexts;
    private int contextMask = 0;

    EModifier(int flag, String keyword, ClassFileContext... contexts) {
        this.flag = flag;
        this.keyword = keyword;
        this.contexts = new HashSet<>(10);
        if (contexts != null) {
            for (ClassFileContext c : contexts) {
                this.contexts.add(c);
                contextMask |= c.getID();
            }
        }
    }

    // Wrappers
    public static boolean isPublic(int flags) {
        return (flags & ACC_PUBLIC.flag) != 0;
    }

    public static boolean isPrivate(int flags) {
        return (flags & ACC_PRIVATE.flag) != 0;
    }

    public static boolean isProtected(int flags) {
        return (flags & ACC_PROTECTED.flag) != 0;
    }

    public static boolean isStatic(int flags) {
        return (flags & ACC_STATIC.flag) != 0;
    }

    public static boolean isFinal(int flags) {
        return (flags & ACC_FINAL.flag) != 0;
    }

    public static boolean isTransitive(int flags) {
        return isFinal(flags);
    }

    public static boolean isSuper(int flags) {
        return (flags & ACC_SUPER.flag) != 0;
    }

    public static boolean isSynchronized(int flags) {
        return isSuper(flags);
    }

    public static boolean isVolatile(int flags) {
        return (flags & ACC_VOLATILE.flag) != 0;
    }

    public static boolean isBridge(int flags) {
        return isVolatile(flags);
    }

    public static boolean isStaticPhase(int flags) {
        return isVolatile(flags);
    }

    public static boolean isTransient(int flags) {
        return (flags & ACC_TRANSIENT.flag) != 0;
    }

    public static boolean isVarArgs(int flags) {
        return isTransient(flags);
    }

    public static boolean isNative(int flags) {
        return (flags & ACC_NATIVE.flag) != 0;
    }

    public static boolean isInterface(int flags) {
        return (flags & ACC_INTERFACE.flag) != 0;
    }

    public static boolean isAbstract(int flags) {
        return (flags & ACC_ABSTRACT.flag) != 0;
    }

    public static boolean isStrict(int flags) {
        return (flags & ACC_STRICT.flag) != 0;
    }

    public static boolean isSynthetic(int flags) {
        return (flags & ACC_SYNTHETIC.flag) != 0;
    }

    public static boolean isAnnotation(int flags) {
        return (flags & ACC_ANNOTATION.flag) != 0;
    }

    public static boolean isEnum(int flags) {
        return (flags & ACC_ENUM.flag) != 0;
    }

    public static boolean isModule(int flags) {
        return (flags & ACC_MODULE.flag) != 0;
    }

    public static boolean isMandated(int flags) {
        return isModule(flags);
    }

    public static boolean isSyntheticPseudoMod(int flags) {
        return (flags & SYNTHETIC_ATTRIBUTE.flag) != 0;
    }

    public static boolean isDeprecatedPseudoMod(int flags) {
        return (flags & DEPRECATED_ATTRIBUTE.flag) != 0;
    }

    public static boolean isValue(int flags) {
        return (flags & ACC_VALUE.flag) != 0;
    }

    public static boolean isPermitsValue(int flags) {
        return (flags & ACC_PERMITS_VALUE.flag) != 0;
    }

    public static boolean isPrimitive(int flags) {
        return (flags & ACC_PRIMITIVE.flag) != 0;
    }

    public static boolean hasPseudoMod(int flags) {
        return isSyntheticPseudoMod(flags) || isDeprecatedPseudoMod(flags);
    }

    /*
     * Check that only one flag is set
     */
    public static boolean onlyOneOfFlags(int flag, EModifier... modifiers) {
        if (modifiers.length >= 2) {
            final int mask = flag & getFlags(modifiers);
            return Arrays.stream(modifiers).mapToInt(EModifier::getFlag).anyMatch(f -> (f | mask) == f);
        }
        return modifiers.length != 0 && flag == modifiers[0].getFlag();
    }

    /*
     * Check that at least one flag is set
     */
    public static boolean anyOf(int flag, EModifier... modifiers) {
        if (modifiers.length > 0) {
            return (flag & getFlags(modifiers)) != 0;
        }
        return false;
    }

    public static int cleanFlags(int flag, EModifier... modifiers) {
        if (modifiers.length > 0) {
            for (EModifier m : modifiers) {
                flag &= ~m.flag;
            }
        }
        return flag;
    }


    /*
     * Are both flags set?
     */
    public static boolean both(int flags, EModifier modifierA, EModifier modifierB) {
        final int bothFlags = modifierA.getFlag() | modifierB.getFlag();
        return (flags & bothFlags) == bothFlags;
    }

    /*
     * Check that there are no other modifiers in flags except given.
     */
    public static boolean noFlagsExcept(int flags, EModifier... modifiers) {
        return (flags & getFlags(modifiers)) == flags;
    }

    public static int getFlags(EModifier... modifiers) {
        int flag = 0;
        if (modifiers.length > 0) {
            for (EModifier m : modifiers) {
                flag |= m.flag;
            }
        }
        return flag;
    }

    public static int getFlags(ClassFileContext context, EModifier... modifiers) {
        int flag = 0;
        if (modifiers.length == 0) {
            modifiers = EModifier.values();
        }
        for (EModifier m : modifiers) {
            if( m.contexts.contains(context) ) {
                flag |= m.flag;
            }
        }
        return flag;
    }

    private static int addTo(ArrayList<String> list, int flags, boolean isName, EModifier modifier) {
        list.add(isName ? modifier.getFlagName() : modifier.getJavaFlagModifier());
        return clearIfSet(flags, modifier);
    }

    public static String asKeywords(int modifiers, ClassFileContext context) {
        return flagsToString(modifiers, false, context, KEYWORDS_DELIMITER, KEYWORDS_SUFFIX);
    }

    public static String asNames(int modifiers, ClassFileContext context) {
        return flagsToString(modifiers, true, context, NAMES_DELIMITER, NAMES_SUFFIX);
    }

    private static String flagsToString(int modifiers, boolean isName, ClassFileContext context, String delimiter, String suffix) {
        String s = String.join(delimiter, flagsToList(modifiers, isName, context));
        return s.isBlank() ? "" : s + suffix;
    }

    /**
     * Get either a keyword or a name of a flags according to the context.
     *
     * @param flags   the flags to choose a corresponding names or keywords.
     * @param isName  either the JVMS modifier names ot JLS keywords are selected
     * @param context the entity for which the names/keywords are selected
     * @return the List of the names or the keywords according to the parameter isName
     */
    private static ArrayList<String> flagsToList(int flags, boolean isName, ClassFileContext context) {
        ArrayList<String> list = new ArrayList<>();
        // run through all access flags
        if (isPublic(flags) && context.belongToContextOf(ACC_PUBLIC)) {
            flags = addTo(list, flags, isName, ACC_PUBLIC);
        } else if (isPrivate(flags) && context.belongToContextOf(ACC_PRIVATE)) {
            flags = addTo(list, flags, isName, ACC_PRIVATE);
        } else if (isProtected(flags) && context.belongToContextOf(ACC_PROTECTED)) {
            flags = addTo(list, flags, isName, ACC_PROTECTED);
        }
        // ACC_STATIC
        if (isStatic(flags) && context.belongToContextOf(ACC_STATIC)) {
            // Static CLASS doesn't exist. Only INNER_CLASS, FIELD, METHOD can have this modifier.
            flags = addTo(list, flags, isName, ACC_STATIC);
        }
        // ACC_FINAL
        if (isFinal(flags) && context.isOneOf(CLASS, INNER_CLASS, FIELD, METHOD, METHOD_PARAMETERS)) {
            flags = addTo(list, flags, isName, ACC_FINAL);
        }
        // ACC_TRANSITIVE ACC_SUPER ACC_SYNCHRONIZED ACC_OPEN
        if (isSuper(flags)) {                          //  == isTransitive(flags) == isSynchronized(flags) == isOpen(flags)
            switch (context) {
                case CLASS -> {
                    // In Java SE 8, the ACC_SUPER semantics became mandatory,
                    // regardless of the setting of ACC_SUPER or the class file version number,
                    // and the flags no longer had any effect.
                    // still we have to keep it in here (if it was here), as if the new class is used for hotswap, it s absence would casue
                    // java.lang.UnsupportedOperationException: class redefinition failed: attempted to change the class modifiers
                    flags = addTo(list, flags, isName, ACC_SUPER);
                }
                case REQUIRES -> flags = addTo(list, flags, isName, ACC_TRANSITIVE);
                case METHOD -> flags = addTo(list, flags, isName, ACC_SYNCHRONIZED);
                case MODULE -> flags = addTo(list, flags, isName, ACC_OPEN);
            }
        }
        // ACC_VOLATILE ACC_BRIDGE ACC_STATIC_PHASE ACC_PERMITS_VALUE
        if (isVolatile(flags)) {                         // == isBridge(flags) ==isStaticPhase(flags) == isPermitsValue(flags)
            switch (context) {
                case FIELD -> flags = addTo(list, flags, isName, ACC_VOLATILE);
                case METHOD -> flags = addTo(list, flags, isName, ACC_BRIDGE);
                case REQUIRES -> flags = addTo(list, flags, isName, ACC_STATIC_PHASE);
                case CLASS, INNER_CLASS -> flags = addTo(list, flags, isName, ACC_PERMITS_VALUE);
            }
        }
        // ACC_TRANSIENT ACC_VARARGS
        if (isTransient(flags)) {                        // == isVarArgs(flags)
            switch (context) {
                case FIELD -> flags = addTo(list, flags, isName, ACC_TRANSIENT);
                case METHOD -> flags = addTo(list, flags, isName, ACC_VARARGS);
            }
        }
        // ACC_NATIVE    ACC_VALUE
        if (isNative(flags)) {                           // == isValue(flags)
            switch (context) {
                case METHOD -> flags = addTo(list, flags, isName, ACC_NATIVE);
                case CLASS, INNER_CLASS -> flags = addTo(list, flags, isName, ACC_VALUE);
            }
        }
        // ACC_INTERFACE
        if (isInterface(flags) && context.isOneOf(CLASS, INNER_CLASS)) {
            if (isName) {
                flags = addTo(list, flags, true, ACC_INTERFACE);
            } else {
                flags = clearIfSet(flags, ACC_INTERFACE);
            }
        }
        // ACC_ABSTRACT
        if (isAbstract(flags) && context.isOneOf(CLASS, INNER_CLASS, METHOD)) {
            flags = addTo(list, flags, isName, ACC_ABSTRACT);
        }

        // ACC_STRICT ACC_PRIMITIVE
        if (isStrict(flags)) {                          // == isPrimitive(flags)
            switch (context) {
                case METHOD -> flags = addTo(list, flags, isName, ACC_STRICT);
                case CLASS, INNER_CLASS -> flags = addTo(list, flags, isName, ACC_PRIMITIVE);
            }
        }
        // ACC_SYNTHETIC
        if (isSynthetic(flags) && context.belongToContextOf(ACC_SYNTHETIC)) {
            flags = addTo(list, flags, isName, ACC_SYNTHETIC);
        }
        // ACC_ANNOTATION
        if (isAnnotation(flags) && context.isOneOf(CLASS, INNER_CLASS)) {
            flags = addTo(list, flags, isName, ACC_ANNOTATION);
        }
        // ACC_ENUM
        if (isEnum(flags) && context.isOneOf(CLASS, INNER_CLASS, FIELD)) {
            flags = addTo(list, flags, isName, ACC_ENUM);
        }
        // ACC_MODULE ACC_MANDATED
        if (isModule(flags)) {                          // == isMandated(flags)
            if (context == CLASS) {
                if (isName) {
                    flags = addTo(list, flags, true, ACC_MODULE);
                } else {
                    flags = clearIfSet(flags, ACC_MODULE);
                }
            } else if (context.isOneOf(MODULE, REQUIRES, EXPORTS, OPENS, METHOD_PARAMETERS)) {
                flags = addTo(list, flags, isName, ACC_MANDATED);
            }
        }
        if (flags != 0) {
            list.add(String.format("0x%04X", flags));
        }
        return list;
    }

    private static int clearIfSet(int flag, EModifier... modifiers) {
        for (EModifier m : modifiers) {
            if ((flag & m.flag) != 0) {
                flag &= ~m.flag;
            }
        }
        return flag;
    }

    public Set<ClassFileContext> getClassFileContext() {
        return contexts;
    }

    public int getAllovedContextMask() {
        return contextMask;
    }

    public int getFlag() {
        return flag;
    }

    public String getJavaFlagModifier() {
        return keyword;
    }

    public String getFlagName() {
        return this.toString();
    }
}
