/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType.*;

/**
 * Writable data could be undefined until Constant Pool is filled in
 */
public interface ConstantPoolDataVisitor extends DataWriter {

    <T extends DataWriter> T visit(ConstantPool pool);

    default <T extends DataWriter> T visitData(T cpData, ConstantPool pool) {
        T data = ((ConstantPoolDataVisitor) cpData).visit(pool);
        if (data.getClass().isAssignableFrom(ConstCell.class)) {
            ConstCell cell = (ConstCell) data;
            ClassFileConst.ConstType type = cell.getType();
            if (type.oneOf(CONSTANT_INTEGER, CONSTANT_FLOAT,
                    CONSTANT_LONG, CONSTANT_DOUBLE,
                    CONSTANT_UTF8)) {
                data = (T) new ParserAnnotation.ConstElemValue(type.getAnnotationElementTypeValue(), cell);

            } else {
                data = (T) new ParserAnnotation.ClassElemValue(cell);
            }
//        } else if (type.oneOf(CONSTANT_CLASS)) {
//            data = (T) new ParserAnnotation.ClassElemValue(cell);
//        } else {
//            data = (T) new ParserAnnotation.ClassElemValue(refCell);
//        }
///       annotation value refers to unknown data type
//        pool.env.error("unknown.annotation.data", cpData.getClass().getSimpleName());
//        throw new Scanner.SyntaxError().Fatal();
        }
        return data;
    }

    default <T extends DataWriter> T visitConstCell(ConstCell cell, ConstantPool pool) {
        ConstCell refCell;
        if (!cell.isSet()) {
            refCell = pool.uncheckedGetCell(cell.cpIndex);
        } else {
            refCell = new ConstCell(cell.cpIndex, cell.ref);
        }
        cell.ref = refCell.ref;
        return (T) cell;
    }
}
