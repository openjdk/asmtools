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
import java.util.stream.Stream;

/**
 * Container for attributes having inline tables:
 * InnerClasses, BootstrapMethods, LineNumberTable, Runtime(In)Visible(Type|Parameter)Annotations,
 * LocalVariableTable, StackMapTable
 */
class DataVectorAttr<T extends DataWriter> extends AttrData implements Collection<T> {

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

    @Override
    public boolean add(T element) {
        return elements.add(element);
    }

    @Override
    public boolean remove(Object o) {
        return elements.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return elements.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return elements.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return elements.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return elements.retainAll(c);
    }

    @Override
    public void clear() {
        elements.clear();
    }

    public DataVectorAttr<T> addAll(Stream<T> s) {
        s.filter(e->e != null).forEach(elements::add);
        return this;
    }

    public T set(int i, T element) {
        return elements.set(i, element);
    }

    public int size() { return elements.size(); }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    public void replaceAll(Collection<T> collection) {
        elements.clear();
        elements.addAll(collection);
    }

    public ArrayList<T> getElements() {
        return elements;
    }

    @Override
    public Iterator<T> iterator() {
        return elements.iterator();
    }

    @Override
    public Object[] toArray() {
        return elements.toArray();
    }

    @Override
    public <V> V[] toArray(V[] a) {
        return elements.toArray(a);
    }

    public Stream<T> stream() { return elements.stream(); };

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
