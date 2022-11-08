package org.openjdk.asmtools.jasm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.asmtools.common.ToolInput;
import org.openjdk.asmtools.common.ToolOutput;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MultipleFilesInSingleJasmTest {

    @Test
    public void clfacc00610m10pTest() throws IOException {
        byte[] jasmFile = getJasmFile("clfacc00610m10p.jasm");
        ToolInput file = new ToolInput.ByteInput(jasmFile);
        ToolOutput.ByteOutput output = new ToolOutput.ByteOutput();
        ToolOutput.DualStreamToolOutput log = new ToolOutput.SingleDualOutputStreamOutput(); //todo hide to ToolOutput.StringLog once done
        org.openjdk.asmtools.jasm.Main jasm = new org.openjdk.asmtools.jasm.Main(output, log, file, "-v");
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
        ToolInput file = new ToolInput.ByteInput(jasmFile);
        ToolOutput.ByteOutput output = new ToolOutput.ByteOutput();
        ToolOutput.DualStreamToolOutput log = new ToolOutput.SingleDualOutputStreamOutput(); //todo hide to ToolOutput.StringLog once done
        org.openjdk.asmtools.jasm.Main jasm = new org.openjdk.asmtools.jasm.Main(output, log, file, "-v");
        int i = jasm.compile();
        Assertions.assertEquals(0, i);
        Assertions.assertEquals(258, output.getOutputs().size());

    }
}
