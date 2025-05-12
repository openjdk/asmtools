/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.structure.EAttribute;

import java.io.IOException;
import java.util.ArrayList;

import static org.openjdk.asmtools.jasm.JasmTokens.Token.*;

/**
 * StackMapTable_attribute {
 *     u2              attribute_name_index;
 *     u4              attribute_length;
 *     u2              number_of_entries;
 *     stack_map_frame entries[number_of_entries];
 * }
 */
public class StackMapTable extends Container<StackMapData, CodeData> {

    private final EAttribute attribute;
    // Container for flags of elements: true is for a container element is a wrapper
    // (i.e. early_larval_frame) for the next one.
    protected final ArrayList<Boolean> wrappers;
    private String jasmHeader = ": number_of_entries = %d";
    private String intLine = "n/a";
    private String strLine = "n/a";
    private int shift;

    /**
     * @param attribute       either Implicit stack map attribute or the StackMapTable attribute of the container
     * @param owner
     * @param initialCapacity initial capacity of the  stack_map_frame entries[number_of_entries];
     */
    public StackMapTable(EAttribute attribute, CodeData owner, int initialCapacity) {
        super(owner, StackMapData.class, initialCapacity);
        this.wrappers = new ArrayList<>(initialCapacity);
        this.attribute = attribute;
        switch (this.attribute) {
            case ATT_StackMap -> {
                shift = owner.calculateInlinedTitleShift(LOCALSMAP.parseKey());
                jasmHeader = STACKMAP_HEADER.parseKey() + jasmHeader;
            }
            case ATT_StackMapTable -> {
                shift = owner.calculateInlinedTitleShift(FRAMETYPE.parseKey());
                jasmHeader = STACKMAPTABLE_HEADER.parseKey() + jasmHeader;
            }
            default -> throw new IllegalStateException("Unexpected value: " + this.attribute);
        }
        intLine = "%" + shift + "s = %-3d";
        strLine = "%" + shift + "s = %s";
    }

    public void add(StackMapData element, boolean isWrapper) throws IOException {
        element.setPrintParticles(intLine, strLine, shift);
        super.add(element);
        wrappers.add(isWrapper);
    }

    public int real_size() {
        int mWrappers = (int) wrappers.stream()
                .filter(Boolean::booleanValue)
                .count();
        return elements.size() - mWrappers;
    }

    /**
     * Prints StackMapTable_attribute {
     *     u2              attribute_name_index;
     *     u4              attribute_length;
     *     u2              number_of_entries;
     *     stack_map_frame entries[number_of_entries];
     * }
     *
     * There are no differences between the simple (jasm) and extended (table) presentations of StackMapTable attribute.
     *
     * @throws IOException if an I/O error occurs while printing
     */
    @Override
    public void print() throws IOException {
        int size = this.size();
        if (size > 0) {
            elements.get(0).setHeader(jasmHeader.formatted(real_size()));
            setMaxPrintSize(getPrintSize());
            for (int i = 0; i < size; i++) {
                StackMapData element = elements.get(i);
                element.setCommentOffset(this.getCommentOffset());
                element.jasmPrint(i, size);
            }
        }
    }
}
