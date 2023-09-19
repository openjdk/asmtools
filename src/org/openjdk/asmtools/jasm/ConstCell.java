/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Objects;

/**
 * ConstantCell is a type of data that can be in a constant pool.
 */
public class ConstCell<V extends ConstValue> extends Indexer implements ConstantPoolDataVisitor {

    V ref;
    // 0 - highest - ref from ldc, 1 - any ref, 2 - no ref
    ConstantPool.ReferenceRank rank = ConstantPool.ReferenceRank.NO;
    // status flag
    private int flag;

    ConstCell(int id, V ref) {
        this.cpIndex = id;
        this.ref = ref;
    }

    ConstCell(V ref) {
        this(NotSet, ref);
    }

    ConstCell(int id) {
        this(id, null);
    }

    public int getFlag() {
        return flag;
    }
    public ConstCell<V> setFlag(int flag) {
        this.flag = flag;
        return this;
    }

    @Override
    public final boolean isSet() {
        return super.isSet() &&
                ((this.ref != null && this.ref.value != null) ||
                        (ref instanceof ConstantPool.ConstValue_Zero));
    }

    public ClassFileConst.ConstType getType() {
        return ref == null ? ClassFileConst.ConstType.CONSTANT_UNKNOWN : ref.tag;
    }

    public char getAnnotationElementTypeValue() {
        return ref == null ? ClassFileConst.AnnotationElementType.AE_NOT_APPLICABLE.tag() :
                ref.tag.getAnnotationElementTypeValue();
    }

    @Override
    public int getLength() {
        return 2;
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        out.writeShort(cpIndex);
    }

    public void setRank(ConstantPool.ReferenceRank rank) {
        // don't change a short ref to long due to limitation of ldc - max 256 indexes allowed
        // ConstantPool.ReferenceRank.LDC has high priority
        if (this.rank.priority > rank.priority) {
            this.rank = rank;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ConstCell)) return false;
        ConstCell<?> constCell = (ConstCell<?>) obj;
        return (Objects.equals(ref, constCell.ref) && rank == constCell.rank && cpIndex == constCell.cpIndex);
    }

    public boolean equalsByValue(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ConstCell)) return false;
        ConstCell<?> constCell = (ConstCell<?>) obj;
        // (a == b) || (a != null && a.equals(b))
        if (this.ref == constCell.ref) return true;
        if (this.ref != null && ref instanceof ConstValue) {
            if (constCell.ref instanceof ConstValue) {
                return ((ConstValue) this.ref).equalsByValue(constCell.ref);
            }
            return false;
        } else {
            return Objects.equals(ref, constCell.ref);
        }
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * (result + (ref != null ? ref.hashCode() : 0));
        return result;
    }

    @Override
    public String toString() {
        return "#" + ((cpIndex == NotSet) ? "?" : cpIndex) + "=" + ref;
    }

    @Override
    public <T extends DataWriter> T visit(ConstantPool pool) {
        return visitConstCell(this, pool);
    }
}
