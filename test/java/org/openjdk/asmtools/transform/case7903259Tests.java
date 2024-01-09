/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.transform;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openjdk.asmtools.lib.LogAndReturn;
import org.openjdk.asmtools.lib.action.EAsmTools;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.openjdk.asmtools.common.FileUtils.getResourceFilePath;

/**
 * Tests tools against files without extension.
 * CODETOOLS-7903259 jasm: file names without extensions causes a tool crash
 */
@TestInstance(PER_CLASS)
public class case7903259Tests {

    List<String> assemblers = List.of("jasm", "jcoder");
    List<String> disassemblers = List.of("jdec", "jdis");
    List<String> files = Stream.concat(assemblers.stream(), disassemblers.stream()).collect(Collectors.toList());

    File resourceDir;

    @BeforeAll
    public void init() throws IOException {
        resourceDir = getResourceFilePath(this.getClass(),
                "case7903259" + File.separator + assemblers.get(0)).
                getParentFile();
    }

    @Test
    void NoExtensionTest() {
        EAsmTools.Tool tool;
        int testedToolsCount = 0;
        for (String fileName : files) {
            tool = EAsmTools.getTool(fileName);
            if (tool != null) {
                LogAndReturn ret = tool.call(List.of(resourceDir + File.separator + appendBin(fileName)));
                assertTrue(ret.log.toString().isEmpty());
                assertEquals(0, ret.result);
                testedToolsCount++;
            }
        }
        assertEquals(4, testedToolsCount);
    }

    private String appendBin(String fileName) {
        if (disassemblers.contains(fileName)) {
            return fileName+".bin";
        } else {
            return fileName;
        }
    }
}
