/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Optional;

import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType.*;

/**
 * AttrData
 * <p>
 * AttrData is the base class for many attributes (or parts of attributes), and it is
 * instantiated directly for simple attributes (like Synthetic or Deprecated).
 */
class AttrData implements ConstantPoolDataVisitor {

    private final EAttribute attribute;
    private final ConstCell attributeNameConstantCell;

    AttrData(ConstantPool pool, EAttribute attribute) {
        this.attribute = attribute;
        this.attributeNameConstantCell = pool.findUTF8Cell(attribute.parseKey());
    }

    @Override
    public <T extends DataWriter> T visit(ConstantPool pool) {
        if (this instanceof CPXAttr cpxAttr) {
            final ConstCell cell = cpxAttr.cell;
            if (! cell.isSet()) {
                if (cell.getType() ==  CONSTANT_STRING)  {
                    if (attribute.getCPTypeOfIndex() == CONSTANT_UTF8) {
                        Optional<ConstCell<?>> strCell = pool.ConstantPoolHashByValue.values().stream().
                                filter(v -> v.isSet() && v.getType() == CONSTANT_STRING && v.equalsByValue(cell)).
                                findAny();
                        cpxAttr.cell = strCell.isPresent() ?
                                strCell.get() :
                                pool.findCell(new ConstantPool.ConstValue_String((ConstCell<ConstantPool.ConstValue_UTF8>) cell.ref.value));
                        }
                    }
                }
            }
        return (T) this;
    }

    protected ConstCell<?> classifyConstCell(ConstantPool pool, ConstCell<?> cell) {
        switch (cell.getType()) {
            case CONSTANT_CLASS -> {
                return cell;
            }
            case CONSTANT_UTF8 -> {
                if( attribute.getCPTypeOfIndex() == CONSTANT_CLASS ) {
                    Optional<ConstCell<?>> clsCell = pool.ConstantPoolHashByValue.values().stream().
                            filter(v -> v.getType() == CONSTANT_CLASS && v.ref.value.equals(cell)).
                            findAny();
                    if (clsCell.isPresent()) {
                        return clsCell.get();
                    } else {
                        // create class cell referencing to UTF8
                        return pool.findCell(new ConstantPool.ConstValue_Class((ConstCell<ConstantPool.ConstValue_UTF8>) cell));
                    }
                }
            }
            default -> {
                // no action
            }
        }
        return cell;
    }

    // full length of the attribute
    // declared in Data
    @Override
    public int getLength() {
        return 6 + attrLength();
    }

    // subclasses must redefine this
    public int attrLength() {
        return 0;
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        out.writeShort(attributeNameConstantCell.cpIndex);
        out.writeInt(attrLength()); // attribute length
    }
} // end class AttrData
