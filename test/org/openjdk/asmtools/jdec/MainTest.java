package org.openjdk.asmtools.jdec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.asmtools.ThreeStringWriters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

class MainTest {

    @Test
    public void main3StreamsNoSuchFileError() {
        ThreeStringWriters outs = new ThreeStringWriters();
        String nonExisitngFile = "someNonExiostingFile";
        Main decoder = new Main(outs.getToolOutput(), outs.getErrorOutput(), outs.getLoggerOutput(), nonExisitngFile);
        int i = decoder.decode();
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
        Main decoder = new Main(outs.getToolOutput(), outs.getErrorOutput(), outs.getLoggerOutput(), "./target/classes/org/openjdk/asmtools/jdec/Main.class");
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
        File in =  new File("./target/classes/org/openjdk/asmtools/jdec/Main.class");
        InputStream is = System.in;
        try {
            System.setIn(new FileInputStream(in));
            Main decoder = new Main(outs.getToolOutput(), outs.getErrorOutput(), outs.getLoggerOutput());
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