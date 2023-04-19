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

/**
 * A (typed) tagged value in the constant pool like ConstCell<ConstValue_UTF8>
 */
public abstract class ConstValue<T> {

    protected T value;
    protected final ClassFileConst.ConstType tag;

    public ConstValue(ClassFileConst.ConstType tag, T value) {
        this.tag = tag;
        this.value = value;
    }

    public int size() {
        return 1;
    }

    public boolean isSet() { return value != null; }

    public ConstValue<T> setValue(T value) {
        this.value = value;
        return this;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + tag.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ConstValue)) return false;
        ConstValue<?> that = (ConstValue<?>) obj;
        return (this.value.equals(that.value) && this.tag == that.tag);
    }

    public boolean equalsByValue(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ConstValue)) return false;
        ConstValue<?> that = (ConstValue<?>) obj;
        if( this.value instanceof ConstCell<?> ) {
            if( that.value instanceof ConstCell<?> ) {
                return (((ConstCell<?>)this.value).equalsByValue(that.value) && this.tag == that.tag);
            }
            return false;
        }
        return (this.value.equals(that.value) && this.tag == that.tag);
    }

    @Override
    public String toString() {
        return String.format("[%s : '%s']", tag.toString(), asString() ) ;
    }

    /**
     * @return String presentation of the value.
     */
    public String asString() {
        return (value == null) ? "?" : value.toString();
    }

    public void write(CheckedDataOutputStream out) throws IOException {
        out.writeByte(tag.getTag());
    }
} // end ConstValue
