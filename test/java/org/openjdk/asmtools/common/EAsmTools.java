/*
 * Copyright (c) 2023, Oracle, Red Hat  and/or theirs affiliates. All rights reserved.
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
package org.openjdk.asmtools.common;

import org.openjdk.asmtools.InputOutputTests;

import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

public enum EAsmTools {
    JASM(".jasm", (files, args) -> CompileAction.JAsm(files, args)),
    JCODER(".jcod", (files, args) -> CompileAction.JCoder(files, args)),
    JDEC(".class", (files, args) -> GenerateAction.JDec(files, args)),
    JDIS(".class", (files, args) -> GenerateAction.JDis(files, args));
    private final String fileExtension;
    private final Tool tool;

    EAsmTools(String fileExtension, Tool tool) {
        this.fileExtension = fileExtension;
        this.tool = tool;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public static Tool getTool(String toolName) {
        for (EAsmTools t : values()) {
            if (toolName.compareToIgnoreCase(t.name()) == 0)
                return t.tool;
        }
        fail("The tool " + toolName + " isn't yet implemented.");
        return null;
    }

    @FunctionalInterface
    public interface Tool {
        InputOutputTests.LogAndReturn call(List<String> files, String... args);
    }


}
