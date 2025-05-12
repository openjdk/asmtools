/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.openjdk.asmtools.jasm.TableFormatModel.Token.SOURCE_FILE;

/**
 * The SourceFile attribute since 45.3
 * <p>
 * SourceFile_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 sourcefile_index;
 * }
 */
public class SourceFileData extends AttributeData<SourceFileData> {

    public SourceFileData(ClassData classData) {
        super(classData, SOURCE_FILE);
    }

    @Override
    public boolean isPrintable() {
        return !dropSourceFile && calculateName() != null;
    }

    public String calculateName() {
        if (this.name == null) {
            this.name = pool.getString(cpx, index -> null);
        }
        return this.name;
    }

    public SourceFileData getName() {
        name = pool.getString(cpx, index -> "#%d".formatted(index));
        return this;
    }
}
