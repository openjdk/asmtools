package org.openjdk.asmtools.jdis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.asmtools.ThreeStringWriters;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

class MainTest {

    @Test
    public void main3StreamsNoSuchFileError() {
        ThreeStringWriters outs = new ThreeStringWriters();
        String nonExisitngFile = "someNonExiostingFile";
        //for 0 file args, there is hardcoded System.exit
        Main decoder = new Main(outs.getToolOutput(), outs.getErrorOutput(), outs.getLoggerOutput(), nonExisitngFile);
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
        Main decoder = new Main(outs.getToolOutput(), outs.getErrorOutput(), outs.getLoggerOutput(), "./target/classes/org/openjdk/asmtools/jdis/Main.class");
        int i = decoder.disasm();
        outs.flush();
        Assertions.assertEquals(0, i);
        Assertions.assertFalse(outs.getToolBos().isEmpty());
        Assertions.assertTrue(outs.getErrorBos().isEmpty());
        Assertions.assertTrue(outs.getLoggerBos().isEmpty());
    }


    @Test
    public void superIsNotOmited() throws IOException {
        ThreeStringWriters outs = new ThreeStringWriters();
        String testClazz = "org/openjdk/asmtools/jdis/Main";
        String name = testClazz.replaceAll(".*/", "");
        Main decoder = new Main(outs.getToolOutput(), outs.getErrorOutput(), outs.getLoggerOutput(), "./target/classes/" + testClazz + ".class");
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
                checkSupperIsOmitedIfNotPresent(clazz, testClazz);
                return;
            }
        }
        Assertions.assertTrue(false, "class Main was not found in disassembled output");
    }

    private void checkSupperIsOmitedIfNotPresent(String clazzWithSuper, String fqn) throws IOException {
        String name = fqn.replaceAll(".*/", "");
        String classWithoutSuper = clazzWithSuper.replaceFirst(" super ", " ");
        File sourceWithoutSuper = File.createTempFile("jasmTest", name + ".java");
        sourceWithoutSuper.deleteOnExit();
        Files.write(sourceWithoutSuper.toPath(), classWithoutSuper.getBytes(StandardCharsets.UTF_8));
        File dir = File.createTempFile("asmtools-jasmtest", "tmp.dir");
        dir.delete();
        dir.mkdir();
        dir.deleteOnExit();
        org.openjdk.asmtools.jasm.Main jasmTool = new org.openjdk.asmtools.jasm.Main(sourceWithoutSuper.getAbsolutePath(), "-d", dir.getAbsolutePath());
        jasmTool.compile();
        ThreeStringWriters outs = new ThreeStringWriters();
        Main decoder = new Main(outs.getToolOutput(), outs.getErrorOutput(), outs.getLoggerOutput(), dir.getAbsolutePath() + "/" + fqn + ".class");
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

}