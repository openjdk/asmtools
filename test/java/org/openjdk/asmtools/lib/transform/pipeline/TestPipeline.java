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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.openjdk.asmtools.lib.transform.pipeline.Pipeline.SUCCESS;

/**
 * Class to play around pipeline implementation
 */
public class TestPipeline {
    JcodToClassConverter jcodToClass = new JcodToClassConverter("jcodToClass");
    ClassToJasmConverter classToJasm = new ClassToJasmConverter("classToJasm");
    JasmToClassConverter jasmToClass = new JasmToClassConverter("jasmToClass");
    ClassToJcodConverter classToJcod = new ClassToJcodConverter("classToJcod");
    JcodToClassConverter jcodToClass2 = new JcodToClassConverter("jcodToClass_2");

    private static String getInputFile(String fileName) {
        String resourceName = String.format("/jcod-files/%s", fileName);
        File resourceDir = new File(Objects.requireNonNull(TestPipeline.class.getResource(resourceName)).getFile()).getParentFile();
        return resourceDir + File.separator + fileName;
    }

    @Test
    public void testJcodClass01() {
        Pipeline<Jcod, Clazz> pipeline = new Pipeline<>(jcodToClass);

        // Input file
        String jcodFileName = "atrcvl00101m10p.jcod";
        Jcod jcodInput = new Jcod(new Pipeline.Status(Path.of(getInputFile(jcodFileName))), true);

        // Execute the pipeline
        Clazz finalOutput = pipeline.execute(jcodInput);

        // Output the result
        System.out.println("Final output file: " + finalOutput);
        assertTrue(finalOutput.record().log().toString().isEmpty());
        assertEquals(SUCCESS, finalOutput.record().toolReturn());
        assertTrue(jcodToClass.log().toString().isEmpty());
    }

    @Test
    public void testJcodClass02() {
        Pipeline<Jcod, Clazz> pipeline = new Pipeline<>(jcodToClass);

        // Input file
        String jcodFileName = "atrcvl00101m10p.jcod";

        // Execute the pipeline
        assertThrows(NullPointerException.class, () -> pipeline.execute(null));
    }

    @Test
    public void testJcodClass03() {
        JcodToClassConverter j2c = new JcodToClassConverter();
        Pipeline<Jcod, Clazz> pipeline = new Pipeline<>(jcodToClass);

        // Input file
        String jcodFileName = "abc.txt";
        Jcod jcodInput = new Jcod(new Pipeline.Status(Path.of(getInputFile(jcodFileName))), true);

        // Execute the pipeline
        Clazz finalOutput = pipeline.execute(jcodInput);

        // Output the result
        assertFalse(finalOutput.record().log().toString().isEmpty());
        assertNotEquals(SUCCESS, finalOutput.record().toolReturn());
        assertFalse(jcodToClass.record().log().toString().isEmpty());
    }

    @Disabled       // TODO: "atrcvl00101m10p.jasm not found
    @Test
    public void testJasmClass01() {
        Pipeline<Jasm, Clazz> pipeline = new Pipeline<>(jasmToClass);

        // Input file
        String jasmFileName = "atrcvl00101m10p.jasm";

        // Execute the pipeline
        Clazz finalOutput = pipeline.execute(new Jasm(new Pipeline.Status(Path.of(getInputFile(jasmFileName))), true));

        // Output the result
        System.out.println("Final output file: " + finalOutput.record().file());
        assertTrue(jasmToClass.log().toString().isEmpty());
        assertEquals(SUCCESS, finalOutput.record().toolReturn());
    }

    @Disabled       // TODO: atrcvl00101m10p.jasm not found
    @Test
    public void testJasmClassJcod() {
        Pipeline<Jasm, Jcod> pipeline = new Pipeline<>(jasmToClass).addStage(classToJcod);

        // Input file
        String jasmFileName = "atrcvl00101m10p.jasm";
        jasmFileName = "abc.txt";
        Jasm input = new Jasm(new Pipeline.Status(Path.of(getInputFile(jasmFileName))));

        // Execute the pipeline
        Jcod finalOutput = pipeline.execute(input);

        // Output the result
        System.out.println("Final output file: " + finalOutput);
    }

    @Test
    public void testJcodClassJasmClassJcodClass() {
        JcodToClassConverter jcodToClass = new JcodToClassConverter();
        ClassToJasmConverter classToJasm = new ClassToJasmConverter();
        JasmToClassConverter jasmToClass = new JasmToClassConverter();
        ClassToJcodConverter classToJcod = new ClassToJcodConverter();
        JcodToClassConverter jcodToClass2 = new JcodToClassConverter();

        // Define the pipeline
        Pipeline<Jcod, Clazz> pipeline = new Pipeline<>(jcodToClass)
                .addStage(classToJasm)
                .addStage(jasmToClass)
                .addStage(classToJcod)
                .addStage(jcodToClass2);

        // Input file
        String jcodFileName = "atrcvl00101m10p.jcod";

        Path jcodFilePath = Path.of(getInputFile(jcodFileName));
        Jcod jcodInput = new Jcod(new Pipeline.Status(jcodFilePath));

        // Execute the pipeline
        Clazz finalOutput = pipeline.execute(jcodInput);

        // Output the result
        System.out.println("Final output file: " + finalOutput.record().file());
    }
}
