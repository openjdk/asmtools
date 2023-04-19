package org.openjdk.asmtools.jdec;

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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openjdk.asmtools.ClassPathClassWork;
import org.openjdk.asmtools.ThreeStringWriters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class MainTest extends ClassPathClassWork {

    @BeforeAll
    public static void prepareClass() {
        initMainClassData(org.openjdk.asmtools.jdec.Main.class);
    }

    @Test
    public void main3StreamsNoSuchFileError() {
        ThreeStringWriters outs = new ThreeStringWriters();

        String nonExistingFile = "someNonExistingFile";
        Main decoder = new Main(outs.getToolOutputWrapper(), outs.getLoggers(), nonExistingFile);
        int i = decoder.decode();
        outs.flush();
        Assertions.assertEquals(1, i);
        Assertions.assertTrue(outs.getToolBos().isEmpty());
        Assertions.assertTrue(outs.getLoggerBos().isEmpty());
        Assertions.assertTrue(outs.getErrorBos().contains("No such file"));
        Assertions.assertTrue(outs.getErrorBos().contains(nonExistingFile));
    }

    @Test
    public void main3StreamsFileInCorrectStream() throws IOException {
        ThreeStringWriters outs = new ThreeStringWriters();
        Main decoder = new Main(outs.getToolOutputWrapper(), outs.getLoggers(), classFile);
        int i = decoder.decode();
        outs.flush();
        Assertions.assertEquals(0, i);
        Assertions.assertFalse(outs.getToolBos().isEmpty());
        Assertions.assertTrue(outs.getErrorBos().isEmpty());
        Assertions.assertTrue(outs.getLoggerBos().isEmpty());
        Assertions.assertTrue(outs.getToolBos().contains("0xCAFEBABE;"));
    }

    @Test
    public void main3StreamsStdinCorrectStream() throws IOException {
        ThreeStringWriters outs = new ThreeStringWriters();
        File in =  new File(classFile);
        InputStream is = System.in;
        try {
            System.setIn(new FileInputStream(in));
            Main decoder = new Main(outs.getToolOutputWrapper(), outs.getLoggers(), org.openjdk.asmtools.Main.STDIN_SWITCH);
            int i = decoder.decode();
            outs.flush();
            Assertions.assertEquals(0, i);
            Assertions.assertFalse(outs.getToolBos().isEmpty());
            Assertions.assertTrue(outs.getErrorBos().isEmpty());
            Assertions.assertTrue(outs.getLoggerBos().isEmpty());
            Assertions.assertTrue(outs.getToolBos().contains("0xCAFEBABE"));
        }finally {
            System.setIn(is);
        }
    }

}
