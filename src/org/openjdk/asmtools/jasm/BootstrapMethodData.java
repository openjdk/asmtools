/*
 * Copyright (c) 1996, 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

class BootstrapMethodData extends Indexer implements DataWriter {

    ConstCell bootstrapMethodHandle;
    ArrayList<ConstCell<?>> arguments;

    public BootstrapMethodData(ConstCell bsmHandle, ArrayList<ConstCell<?>> arguments) {
        super();
        this.bootstrapMethodHandle = bsmHandle;
        this.arguments = arguments;
    }

    // methodAttrIndex - bootstrap_method_attr_index
    // The value of the bootstrap_method_attr_index item must be a valid index into the bootstrap_methods array
    // of the bootstrap method table of this class file (§4.7.23).
    public BootstrapMethodData(int methodAttrIndex) {
        super();
        this.bootstrapMethodHandle = null;
        this.arguments = null;
        super.cpIndex = methodAttrIndex;
    }

    public int getLength() {
        return 4 + arguments.size() * 2;
    }

    public boolean hasMethodAttrIndex() {
        return super.isSet();
    }

    public void setMethodAttrIndex(int methodAttrIndex) {
        super.cpIndex = methodAttrIndex;
    }

    public int getMethodAttrIndex() {
        return super.cpIndex;

    }
    public void write(CheckedDataOutputStream out) throws IOException {
        out.writeShort(bootstrapMethodHandle.cpIndex);
        out.writeShort(arguments.size());

        for (ConstCell argument : arguments) {
            out.writeShort(argument.cpIndex);
        }
    }

    @Override
    public String toString() {
        return String.format("{MethodHandle:%s Arguments:%s}",
                bootstrapMethodHandle == null || bootstrapMethodHandle.cpIndex == NotSet ? " n/a" : " #" + bootstrapMethodHandle.cpIndex,
                arguments == null || arguments.isEmpty() ? "{}" :
                        "{ " + arguments.stream().map(a -> String.format("#%d", a.cpIndex)).collect(Collectors.joining(", ")) + " }");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BootstrapMethodData)) return false;
        BootstrapMethodData that = (BootstrapMethodData) o;
        if (!Objects.equals(bootstrapMethodHandle, that.bootstrapMethodHandle))
            return false;
        return this.cpIndex == that.cpIndex & Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        int result = 31 * (bootstrapMethodHandle != null ? bootstrapMethodHandle.hashCode() : 0);
        result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
        return result;
    }

    public boolean equalsByValue(Object o) {
        if (this == o) return true;
        if (!(o instanceof BootstrapMethodData)) return false;
        BootstrapMethodData that = (BootstrapMethodData) o;
        if (!Objects.equals(bootstrapMethodHandle, that.bootstrapMethodHandle))
            return false;
        if (arguments == that.arguments) return true;
        if (arguments != null && (arguments.size() == that.arguments.size())) {
            for (int i = 0; i < arguments.size(); i++) {
                if (!arguments.get(i).equalsByValue(that.arguments.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
