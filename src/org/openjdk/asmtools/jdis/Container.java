/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jdis;

import org.openjdk.asmtools.jasm.TableFormatModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class Container<T extends Indenter, M extends MemberData<?>> extends Indenter implements Iterable<T>, Measurable {

    protected final ArrayList<T> elements;
    protected M owner;

    public Container(M owner, Class<T> elementClass, int initialCapacity) {
        this.owner = owner;
        this.elements = new ArrayList<>(initialCapacity);
        this.tableToken = TableFormatModel.Token.getBy(elementClass);
    }

    protected final boolean isCollectionMeasurable() {
        return (!this.elements.isEmpty() && elements.get(0) instanceof Measurable);
    }

    @Override
    public void print() throws IOException {
        int size = this.size();
        if (size > 0) {
            boolean isExtendedPrintingSupported = tableToken.isExtendedPrintingSupported();
            setMaxPrintSize(getPrintSize());
            for (int i = 0; i < size; i++) {
                T element = elements.get(i);
                if (element instanceof Measurable measurable) {
                    measurable.setMaxPrintSize(this.getMaxPrintSize());
                }
                element.setCommentOffset(this.getCommentOffset());
                if (isExtendedPrintingSupported && isTableOutput())
                    element.tablePrint(i, size);
                else
                    element.jasmPrint(i, size);
            }
        }
    }

    public void add(T element) throws IOException {
        if (element instanceof MemberData<?> md) {
            ((MemberData) md).setOwner(this.owner);
        }
        elements.add(element);
    }

    public int size() {
        return elements.size();
    }

    public T get(int index) {
        return elements.get(index);
    }

    @Override
    public boolean isPrintable() {
        return !elements.isEmpty() &&
                elements.stream().anyMatch(Printable::isPrintable);
    }

    /**
     * Calculates max print size of elements in a collection if they are measurable
     * In the case of s collection the print size is equal to max print size
     *
     * @return maxSize or 0
     */
    @Override
    public int getPrintSize() {
        if (!maxSizeCalculated && isCollectionMeasurable()) {
            this.maxSize = isCollectionMeasurable() ?
                    elements.stream().map(e -> (Measurable) e).
                            mapToInt(Measurable::getPrintSize).max().orElse(0) : 0;
            maxSizeCalculated = true;
        }
        return this.maxSize;
    }

    /**
     * Sets max print size to all elements of the collection
     */
    @Override
    public void setMaxPrintSize(int maxSize) {
        if (maxSize > 0 && isCollectionMeasurable()) {
            elements.stream().map(e -> (Measurable) e).peek(e -> e.setMaxPrintSize(maxSize));
        }
    }

    /**
     * Calculates max print size of elements in a collection if they are measurable
     * In the case of s collection the print size is equal to max print size
     *
     * @return maxSize or 0
     */
    @Override
    public int getMaxPrintSize() {
        return getPrintSize();
    }

    /**
     * Set up print-ability of cartage
     *
     * @param value print-ability
     * @return the instance
     */
    public Container<T, M> setPrintable(boolean value) {
        printable = value;
        return this;
    }

    /**
     * Set up measure-ability of cartage
     *
     * @param value can be measured
     * @return the instance
     */
    public Container<T, M> setHasSize(boolean value) {
        hasSize = true;
        return this;
    }

    @Override
    public Iterator<T> iterator() {
        return elements.iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        elements.forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return elements.spliterator();
    }
}
