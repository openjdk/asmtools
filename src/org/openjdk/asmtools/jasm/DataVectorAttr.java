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

import org.openjdk.asmtools.common.structure.EAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Container for attributes having inline tables:
 * InnerClasses, BootstrapMethods, LineNumberTable, Runtime(In)Visible(Type|Parameter)Annotations,
 * LocalVariableTable, StackMapTable
 */
class DataVectorAttr<T extends DataWriter> extends AttrData implements Iterable<T> {

    private ArrayList<T> elements;
    private boolean     byteIndex;

    /**
     *
     * @param pool Constant pool
     * @param eAttribute the attribute name @see org.openjdk.asmtools.common.content.EAttribute
     * @param byteIndex indicates 1 or two bytes is used to keep number of table elements:
     *                  u2 StackMapTable_attribute.number_of_entries
     *                  u1 RuntimeVisibleParameterAnnotations_attribute.num_parameters
     * @param initialData initial elements of table
     */
    private DataVectorAttr(ConstantPool pool, EAttribute eAttribute, boolean byteIndex, ArrayList<T> initialData) {
        super(pool, eAttribute);
        this.elements = initialData;
        this.byteIndex = byteIndex;
    }

    DataVectorAttr(ConstantPool pool, EAttribute attribute, ArrayList<T> initialData) {
        this(pool, attribute, false, initialData);
    }

    DataVectorAttr(ConstantPool pool, EAttribute attribute) {
        this(pool, attribute, false, new ArrayList<>());

    }

    DataVectorAttr(ConstantPool pool, EAttribute attribute, boolean byteIndex) {
        this(pool, attribute, byteIndex, new ArrayList<>());
    }

    public T get(int index) {
        return elements.get(index);
    }

    public void add(T element) {
        elements.add(element);
    }

    public void put(int i, T element) {
        elements.set(i, element);
    }

    public int size() { return elements.size(); }

    public void replaceAll(Collection<T> collection) {
        elements.clear();
        elements.addAll(collection);
    }

    @Override
    public Iterator<T> iterator() {
        return elements.iterator();
    }

    @Override
    public int attrLength() {
        // calculate overall size
        int length = elements.stream().mapToInt(e->e.getLength()).sum();
        // add the length of number of elements
        length += (byteIndex) ? 1 : 2;
        return length;
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        super.write(out);  // attr name, attr length
        if (byteIndex) {
            out.writeByte(elements.size());
        } else {
            out.writeShort(elements.size());
        } // number of elements
        for (T elem : elements) {
            elem.write(out);
        }
    }

}
