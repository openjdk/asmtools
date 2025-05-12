/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package org.openjdk.asmtools.jdis.notations;

import org.openjdk.asmtools.common.ToolLogger;
import org.openjdk.asmtools.jdis.ConstantPool;
import org.openjdk.asmtools.jdis.notations.Type.*;

import java.util.ArrayList;
import java.util.List;

/**
 * See JVMS 4.4.4.
 */
public class Signature<T extends ToolLogger> extends Descriptor<T> {

    /**
     * Constructor for the signature placed in the constant_pool
     *
     * @param logger to log any exceptions, if they occur.
     * @param index a valid reference into the constant_pool table
     */
    public Signature(T logger, int index) {
        super(logger, index);
    }

    /**
     * Constructor for the descriptor presented as String
     *
     * @param logger to log any exceptions if they occur.
     * @param signature the string presentation of the descriptor
     */
    public Signature(T logger, String signature) {
        super(logger, signature);
    }

    public Type getType(ConstantPool pool) {
        if (type == null)
            type = parse(getValue(pool));
        return type;
    }

    @Override
    public int getParameterCount(ConstantPool pool) {
        MethodType m = (MethodType) getType(pool);
        return m.paramTypes.size();
    }

    @Override
    public String getParameterTypes(ConstantPool pool) {
        MethodType m = (MethodType) getType(pool);
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        String sep = "";
        for (Type paramType : m.paramTypes) {
            sb.append(sep);
            sb.append(paramType);
            sep = ", ";
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String getReturnType(ConstantPool pool) {
        MethodType m = (MethodType) getType(pool);
        return m.returnType.toString();
    }

    @Override
    public String getFieldType(ConstantPool pool) {
        return getType(pool).toString();
    }

    private Type parse(String sig) {
        this.sig = sig;
        sigp = 0;

        List<TypeParamType> typeParamTypes = null;
        if (sig.charAt(sigp) == '<')
            typeParamTypes = parseTypeParamTypes();

        if (sig.charAt(sigp) == '(') {
            List<Type> paramTypes = parseTypeSignatures(')');
            Type returnType = parseTypeSignature();
            List<Type> throwsTypes = null;
            while (sigp < sig.length() && sig.charAt(sigp) == '^') {
                sigp++;
                if (throwsTypes == null)
                    throwsTypes = new ArrayList<>();
                throwsTypes.add(parseTypeSignature());
            }
            return new MethodType(typeParamTypes, paramTypes, returnType, throwsTypes);
        } else {
            Type t = parseTypeSignature();
            if (typeParamTypes == null && sigp == sig.length())
                return t;
            Type superclass = t;
            List<Type> superinterfaces = null;
            while (sigp < sig.length()) {
                if (superinterfaces == null)
                    superinterfaces = new ArrayList<>();
                superinterfaces.add(parseTypeSignature());
            }
            return new ClassSigType(typeParamTypes, superclass, superinterfaces);
        }
    }

    private Type parseTypeSignature() {
        switch (sig.charAt(sigp)) {
            case 'B':
                sigp++;
                return new SimpleType("byte");

            case 'C':
                sigp++;
                return new SimpleType("char");

            case 'D':
                sigp++;
                return new SimpleType("double");

            case 'F':
                sigp++;
                return new SimpleType("float");

            case 'I':
                sigp++;
                return new SimpleType("int");

            case 'J':
                sigp++;
                return new SimpleType("long");

            case 'L':
                return parseClassTypeSignature();

            case 'S':
                sigp++;
                return new SimpleType("short");

            case 'T':
                return parseTypeVariableSignature();

            case 'V':
                sigp++;
                return new SimpleType("void");

            case 'Z':
                sigp++;
                return new SimpleType("boolean");

            case '[':
                sigp++;
                return new ArrayType(parseTypeSignature());

            case '*':
                sigp++;
                return new WildcardType();

            case '+':
                sigp++;
                return new WildcardType(WildcardType.Kind.EXTENDS, parseTypeSignature());

            case '-':
                sigp++;
                return new WildcardType(WildcardType.Kind.SUPER, parseTypeSignature());

            default:
                throw new IllegalStateException(debugInfo());
        }
    }

    private List<Type> parseTypeSignatures(char term) {
        sigp++;
        List<Type> types = new ArrayList<>();
        while (sig.charAt(sigp) != term)
            types.add(parseTypeSignature());
        sigp++;
        return types;
    }

    private Type parseClassTypeSignature() {
        assert sig.charAt(sigp) == 'L';
        sigp++;
        return parseClassTypeSignatureRest();
    }

    private Type parseClassTypeSignatureRest() {
        StringBuilder sb = new StringBuilder();
        List<Type> argTypes = null;
        ClassType t = null;
        char sigch;

        do {
            switch (sigch = sig.charAt(sigp)) {
                case '<':
                    argTypes = parseTypeSignatures('>');
                    break;

                case '.':
                case ';':
                    sigp++;
                    t = new ClassType(t, sb.toString(), argTypes);
                    sb.setLength(0);
                    argTypes = null;
                    break;

                default:
                    sigp++;
                    sb.append(sigch);
                    break;
            }
        } while (sigch != ';');

        return t;
    }

    private List<TypeParamType> parseTypeParamTypes() {
        assert sig.charAt(sigp) == '<';
        sigp++;
        List<TypeParamType> types = new ArrayList<>();
        while (sig.charAt(sigp) != '>')
            types.add(parseTypeParamType());
        sigp++;
        return types;
    }

    private TypeParamType parseTypeParamType() {
        int sep = sig.indexOf(":", sigp);
        String name = sig.substring(sigp, sep);
        Type classBound = null;
        List<Type> interfaceBounds = null;
        sigp = sep + 1;
        if (sig.charAt(sigp) != ':')
            classBound = parseTypeSignature();
        while (sig.charAt(sigp) == ':') {
            sigp++;
            if (interfaceBounds == null)
                interfaceBounds = new ArrayList<>();
            interfaceBounds.add(parseTypeSignature());
        }
        return new TypeParamType(name, classBound, interfaceBounds);
    }

    private Type parseTypeVariableSignature() {
        sigp++;
        int sep = sig.indexOf(';', sigp);
        Type t = new SimpleType(sig.substring(sigp, sep));
        sigp = sep + 1;
        return t;
    }

    private String debugInfo() {
        return sig.substring(0, sigp) + "!" + sig.charAt(sigp) + "!" + sig.substring(sigp + 1);
    }

    private String sig;
    private int sigp;

    private Type type;
}
