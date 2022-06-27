package org.openjdk.asmtools.jdis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.asmtools.ThreeStringWriters;

import java.io.IOException;

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

}