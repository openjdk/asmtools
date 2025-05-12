/*
 * Copyright (c) 2023, 2025, Oracle, Red Hat  and/or theirs affiliates. All rights reserved.
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
package org.openjdk.asmtools.lib.action;

import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.lib.log.LogAndReturn;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.fail;

public enum EAsmTool {
    UNDEF("", null, null),
    JASM(".jasm",
            (files) -> new Jasm().compile(files),
            (inputs) -> new Jasm().compile(inputs)),
    JCODER(".jcod",
            (files) -> new Jcoder().compile(files),
            (inputs) -> new Jcoder().compile(inputs)),
    JDEC(".class",
            (files) -> new Jdec().decode(files),
            (inputs) -> new Jdec().decode(inputs)),
    JDIS(".class",
            (files) -> new Jdis().decode(files),
            (inputs) -> new Jdis().decode(inputs));
    private final String fileExtension;
    private final Function<List<String>, LogAndReturn> filesTool;
    private final Function<ToolInput[], LogAndReturn> inputsTool;

    EAsmTool(String fileExtension, Function<List<String>, LogAndReturn> filesTool,
             Function<ToolInput[], LogAndReturn> inputsTool) {
        this.fileExtension = fileExtension;
        this.filesTool = filesTool;
        this.inputsTool = inputsTool;
    }

    public LogAndReturn call(List<String> files) {
        return filesTool.apply(files);
    }

    public LogAndReturn call(ToolInput[] inputs) {
        return inputsTool.apply(inputs);
    }

    public String getName() {
        return this.name().toLowerCase();
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public static EAsmTool getToolBy(String toolName) {
        for (EAsmTool t : values()) {
            if (toolName.compareToIgnoreCase(t.name()) == 0)
                return t;
        }
        fail("The tool %s isn't yet supported.".formatted(toolName));
        return null;
    }
}
