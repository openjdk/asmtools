package org.openjdk.asmtools.jcoder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.asmtools.common.inputs.ByteInput;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.ByteOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.outputs.log.StderrLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MultipleFilesInSingleJcoderTest {

    @Test
    public void jcod12Test() throws IOException {
        byte[] jcodFile = getJcodFile("12.jcod");
        ToolInput file = new ByteInput(jcodFile);
        ByteOutput output = new ByteOutput();
        DualStreamToolOutput log = new StderrLog(); //todo hide to ToolOutput.StringLog once done
        org.openjdk.asmtools.jcoder.Main jcod = new org.openjdk.asmtools.jcoder.Main(output, log, file, "-v");
        int i = jcod.compile();
        Assertions.assertEquals(0, i);
        Assertions.assertEquals(2, output.getOutputs().size());
    }

    private byte[] getJcodFile(String s) throws IOException {
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

}
