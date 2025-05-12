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
package org.openjdk.asmtools.lib.transform.pipeline;

public class AsmtoolsType {
    private final FileType type;
    private final boolean isFirstInput;
    private Pipeline.Status status;

    public AsmtoolsType(Pipeline.Status status, FileType type, boolean isFirstInput) {
        this.status = status;
        this.type = type;
        this.isFirstInput = isFirstInput;
    }

    public AsmtoolsType(Pipeline.Status status, FileType type) {
        this(status, type, false);
    }

    public boolean firstInput() {
        return isFirstInput;
    }

    public void setRecord(Pipeline.Status status) {
        this.status = status;
    }

    public Pipeline.Status record() {
        return status;
    }

    @Override
    public String toString() {
        return type + "{" +
               "status=" + status +
               ", isFirstInput=" + isFirstInput +
               '}';
    }

    enum FileType {
        JCOD(".jcod"), JASM(".jasm"), JAVA(".java"), CLAZZ(".class");
        private final String ext;

        FileType(String ext) {
            this.ext = ext;
        }

        public String extension() {
            return ext;
        }
    }

}
