/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.openjdk.asmtools.jasm.TableFormatModel.Token.NEST_HOST;

/**
 * The NestHost attribute data
 * <p>
 * NestHost_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 host_class_index;
 * }
 * since class file 55.0 (JEP 181)
 */
public class NestHostData extends AttributeData<NestHostData> {

    public NestHostData(ClassData classData) {
        super(classData, NEST_HOST);
    }

    public String calculateName() {
        if (this.name == null) {
            this.name = pool.getClassName(cpx,
                            index -> "%s #%d".formatted(logger.getResourceString("info.invalid_cp_entry"), index));
        }
        return this.name;
    }
}
