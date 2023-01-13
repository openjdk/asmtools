package org.openjdk.asmtools.jasm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.asmtools.common.inputs.ByteInput;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.ByteOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.outputs.log.StderrLog;
import org.openjdk.asmtools.common.outputs.log.StringLog;
import org.openjdk.asmtools.common.outputs.log.TextLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MultipleFilesInSingleJasmTest {

    @Test
    public void clfacc00610m10pTest() throws IOException {
        byte[] jasmFile = getJasmFile("clfacc00610m10p.jasm");
        ToolInput file = new ByteInput(jasmFile);
        ByteOutput output = new ByteOutput();
        TextLog log = new TextLog();
        org.openjdk.asmtools.jasm.Main jasm = new org.openjdk.asmtools.jasm.Main(output, log, file, "-v", "-t");
        int i = jasm.compile();
        Assertions.assertEquals(0, i);
        Assertions.assertEquals(2, output.getOutputs().size());
    }

    private byte[] getJasmFile(String s) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(s);
        byte[] bytes = null;
        try (is) {
            bytes = is.readAllBytes();
        }
        Assertions.assertNotNull(bytes);
        String jasm = new String(bytes, StandardCharsets.UTF_8);
        Assertions.assertNotNull(jasm);
        return bytes;
    }

    @Test
    public void spinum00101m10pTest() throws IOException {
        byte[] jasmFile = getJasmFile("spinum00101m10p.jasm");
        ToolInput file = new ByteInput(jasmFile);
        ByteOutput output = new ByteOutput();
        TextLog log = new TextLog();
        org.openjdk.asmtools.jasm.Main jasm = new org.openjdk.asmtools.jasm.Main(output, log, file, "-v", "-t");
        int i = jasm.compile();
        Assertions.assertEquals(0, i);
        Assertions.assertEquals(258, output.getOutputs().size());

    }
}
