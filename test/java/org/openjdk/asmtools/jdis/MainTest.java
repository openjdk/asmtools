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
package org.openjdk.asmtools.jdis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openjdk.asmtools.lib.helper.ClassPathClassWork;
import org.openjdk.asmtools.lib.helper.ThreeStringWriters;
import org.openjdk.asmtools.common.outputs.StdoutOutput;
import org.openjdk.asmtools.common.outputs.log.StderrLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

class MainTest extends ClassPathClassWork {

    @BeforeAll
    public static void prepareClass() {
        Options.setDefaultOutputOptions();
        initMainClassData(org.openjdk.asmtools.jdis.Main.class);
    }

    @Test
    public void main3StreamsNoSuchFileError() {
        ThreeStringWriters outs = new ThreeStringWriters();
        String nonExisitngFile = "someNonExistingFile";
        Main decoder = new Main(outs.getToolOutputWrapper(), outs.getLoggers(), nonExisitngFile);
        int i = decoder.disasm();
        outs.flush();
        Assertions.assertEquals(1, i);
        Assertions.assertTrue(outs.getToolBos().isEmpty());
        Assertions.assertTrue(outs.getLoggerBos().isEmpty());
        Assertions.assertTrue(outs.getErrorBos().contains("No such file"));
        Assertions.assertTrue(outs.getErrorBos().contains(nonExisitngFile));
    }

    @Test
    public void main3StreamsFileInCorrectStream() throws IOException {
        ThreeStringWriters outs = new ThreeStringWriters();
        Main decoder = new Main(outs.getToolOutputWrapper(), outs.getLoggers(), classFile);
        int i = decoder.disasm();
        outs.flush();
        Assertions.assertEquals(0, i);
        Assertions.assertFalse(outs.getToolBos().isEmpty());
        Assertions.assertTrue(outs.getErrorBos().isEmpty());
        Assertions.assertTrue(outs.getLoggerBos().isEmpty());
        Assertions.assertTrue(outs.getToolBos().contains("invoke"));
        Assertions.assertEquals(1, packageName.matcher(outs.getToolBos()).results().count());
        Assertions.assertEquals(1, className.matcher(outs.getToolBos()).results().count());
    }

    @Test
    public void main3StreamsStdinCorrectStream() throws IOException {
        ThreeStringWriters outs = new ThreeStringWriters();
        File in = new File(classFile);
        InputStream is = System.in;
        try {
            System.setIn(new FileInputStream(in));
            Main decoder = new Main(outs.getToolOutputWrapper(), outs.getLoggers(), org.openjdk.asmtools.Main.STDIN_SWITCH);
            int i = decoder.disasm();
            outs.flush();
            Assertions.assertEquals(0, i);
            //pise to do stder:-/
            Assertions.assertFalse(outs.getToolBos().isEmpty());
            Assertions.assertTrue(outs.getErrorBos().isEmpty());
            Assertions.assertTrue(outs.getLoggerBos().isEmpty());
            Assertions.assertTrue(outs.getToolBos().contains("invoke"));
            Assertions.assertEquals(1, packageName.matcher(outs.getToolBos()).results().count());
            Assertions.assertEquals(1, className.matcher(outs.getToolBos()).results().count());
        } finally {
            System.setIn(is);
        }
    }

    @Test
    public void superIsNotOmitted() throws IOException {
        ThreeStringWriters outs = new ThreeStringWriters();
        String testClazz = clazz.getName().replace('.', '/');
        String name = testClazz.replaceAll(".*/", "");
        Main decoder = new Main(outs.getToolOutputWrapper(), outs.getLoggers(), classFile);
        int i = decoder.disasm();
        outs.flush();
        Assertions.assertEquals(0, i);
        Assertions.assertFalse(outs.getToolBos().isEmpty());
        Assertions.assertTrue(outs.getErrorBos().isEmpty());
        Assertions.assertTrue(outs.getLoggerBos().isEmpty());
        String clazz = outs.getToolBos();
        for (String line : clazz.split("\n")) {
            if (line.contains("class " + name + " extends JdisTool")) {
                Assertions.assertTrue(line.contains("super"), "class declaration had super omitted - " + line);
                checkSupperIsOmittedIfNotPresent(clazz, testClazz);
                return;
            }
        }
        Assertions.assertTrue(false, "class Main was not found in disassembled output");
    }

    private void checkSupperIsOmittedIfNotPresent(String clazzWithSuper, String fqn) throws IOException {
        String name = fqn.replaceAll(".*/", "");
        String classWithoutSuper = clazzWithSuper.replaceFirst(" super ", " ");
        File sourceWithoutSuper = File.createTempFile("jasmTest", name + ".jasm");
        sourceWithoutSuper.deleteOnExit();
        Files.write(sourceWithoutSuper.toPath(), classWithoutSuper.getBytes(StandardCharsets.UTF_8));
        File dir = File.createTempFile("asmtools-jasmtest", "tmp.dir");
        dir.delete();
        dir.mkdir();
        dir.deleteOnExit();
        org.openjdk.asmtools.jasm.Main jasmTool = new org.openjdk.asmtools.jasm.Main(
                new StdoutOutput(), new StderrLog(), sourceWithoutSuper.getAbsolutePath(),
                "-d", dir.getAbsolutePath());
        int ii = jasmTool.compile();
        Assertions.assertEquals(0, ii);
        ThreeStringWriters outs = new ThreeStringWriters();
        Main decoder = new Main(
                outs.getToolOutputWrapper(), outs.getLoggers(), dir.getAbsolutePath() + "/" + fqn + ".class");
        int i = decoder.disasm();
        outs.flush();
        Assertions.assertEquals(0, i);
        Assertions.assertFalse(outs.getToolBos().isEmpty());
        Assertions.assertTrue(outs.getErrorBos().isEmpty());
        Assertions.assertTrue(outs.getLoggerBos().isEmpty());
        String clazz = outs.getToolBos();
        for (String line : clazz.split("\n")) {
            if (line.contains("class " + name + " extends JdisTool")) {
                Assertions.assertFalse(line.contains("super"), "class declaration had NOT super omitted - " + line);
                return;
            }
        }
        Assertions.assertTrue(false, "class Main was not found in disassembled output");
    }

    @Test
    public void mainBothFileAndStreamIsRead() throws IOException {
        ThreeStringWriters outs = new ThreeStringWriters();
        File in = new File(classFile);
        InputStream is = System.in;
        try {
            System.setIn(new FileInputStream(in));
            Main decoder = new Main(outs.getToolOutputWrapper(), outs.getLoggers(), classFile, org.openjdk.asmtools.Main.STDIN_SWITCH, classFile, org.openjdk.asmtools.Main.STDIN_SWITCH);
            int i = decoder.disasm();
            outs.flush();
            Assertions.assertEquals(0, i);
            Assertions.assertFalse(outs.getToolBos().isEmpty());
            Assertions.assertTrue(outs.getErrorBos().isEmpty());
            Assertions.assertTrue(outs.getLoggerBos().isEmpty());
            Assertions.assertTrue(outs.getToolBos().contains("invoke"));
            //3, both files, but stream only once, despite two are sets
            Assertions.assertEquals(3, packageName.matcher(outs.getToolBos()).results().count());
            Assertions.assertEquals(3, className.matcher(outs.getToolBos()).results().count());
        } finally {
            System.setIn(is);
        }
    }

}
