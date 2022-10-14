package org.openjdk.asmtools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.asmtools.common.ToolInput;
import org.openjdk.asmtools.common.ToolOutput;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class InputOutputTests extends ClassPathClassWork{

    public static class LogAndReturn {
        final ToolOutput.StringLog log;
        final int result;

        public LogAndReturn(ToolOutput.StringLog log, int result) {
            this.log = log;
            this.result = result;
        }
    }
    public static class LogAndTextResults extends LogAndReturn {
        final ToolOutput.TextOutput output;

        public LogAndTextResults(ToolOutput.TextOutput output, ToolOutput.StringLog log, int result) {
            super(log, result);
            this.output = output;
        }
    }
    public static class LogAndBinResults extends  LogAndReturn{
        final ToolOutput.ByteOutput output;

        public LogAndBinResults(ToolOutput.ByteOutput output, ToolOutput.StringLog log, int result) {
            super(log, result);
            this.output = output;
        }
    }

    public LogAndTextResults jdec(byte[]... clazz) {
        ToolInput[] originalFiles = new ToolInput[clazz.length];
        for (int i = 0; i < clazz.length; i++) {
                originalFiles[i]=new ToolInput.ByteInput(clazz[i]);
        }
        ToolOutput.TextOutput decodedFiles = new ToolOutput.TextOutput();
        ToolOutput.StringLog decodeLog = new ToolOutput.StringLog();
        org.openjdk.asmtools.jdec.Main jdec = new org.openjdk.asmtools.jdec.Main(decodedFiles, decodeLog, originalFiles);
        jdec.setVerboseFlag(true);
        jdec.setTraceFlag(true);
        int r = jdec.decode();
        return new LogAndTextResults(decodedFiles, decodeLog, r);
    }
    public LogAndBinResults jcod(String... clazz) {
        ToolInput[] originalFiles = new ToolInput[clazz.length];
        for (int i = 0; i < clazz.length; i++) {
            originalFiles[i]=new ToolInput.ByteInput(clazz[i]);
        }
        ToolOutput.ByteOutput encodedFiles = new ToolOutput.ByteOutput();
        ToolOutput.StringLog encodeLog = new ToolOutput.StringLog();
        org.openjdk.asmtools.jcoder.Main jcod = new org.openjdk.asmtools.jcoder.Main(encodedFiles, encodeLog, originalFiles);
        jcod.setVerboseFlag(true);
        jcod.setTraceFlag(true);
        int r = jcod.compile();
        return new LogAndBinResults(encodedFiles, encodeLog, r);
    }

    public LogAndTextResults jdis(byte[]... clazz) {
        ToolInput[] originalFiles = new ToolInput[clazz.length];
        for (int i = 0; i < clazz.length; i++) {
            originalFiles[i]=new ToolInput.ByteInput(clazz[i]);
        }
        ToolOutput.TextOutput decodedFiles = new ToolOutput.TextOutput();
        ToolOutput.StringLog decodeLog = new ToolOutput.StringLog();
        org.openjdk.asmtools.jdis.Main jdis = new org.openjdk.asmtools.jdis.Main(decodedFiles, decodeLog, originalFiles);
        jdis.setVerboseFlag(true);
        jdis.setTraceFlag(true);
        int r = jdis.disasm();
        return new LogAndTextResults(decodedFiles, decodeLog, r);
    }
    public LogAndBinResults jasm(String... clazz) {
        ToolInput[] originalFiles = new ToolInput[clazz.length];
        for (int i = 0; i < clazz.length; i++) {
            originalFiles[i]=new ToolInput.ByteInput(clazz[i]);
        }
        ToolOutput.ByteOutput encodedFiles = new ToolOutput.ByteOutput();
        ToolOutput.StringLog encodeLog = new ToolOutput.StringLog();
        org.openjdk.asmtools.jasm.Main jasm = new org.openjdk.asmtools.jasm.Main(encodedFiles, encodeLog, originalFiles);
        jasm.setVerboseFlag(true);
        jasm.setTraceFlag(true);
        int r = jasm.compile();
        return new LogAndBinResults(encodedFiles, encodeLog, r);
    }

    @Test
    public void inMemoryDecCodDecCod() throws IOException {
        initTestClassData(TestedHelloWorld.class);
        byte[] data = Files.readAllBytes(new File(classFile).toPath());
        LogAndTextResults o1 = jdec(data);
        LogAndBinResults o2 = jcod(o1.output.getOutputs().get(0).getBody());
        LogAndTextResults o3 = jdec(o2.output.getOutputs().get(0).getBody());
        LogAndBinResults o4 = jcod(o3.output.getOutputs().get(0).getBody());
        Assertions.assertArrayEquals(new int[]{0,0,0,0}, new int[]{o1.result, o2.result, o3.result, o4.result});
    }

    @Test
    public void inMemoryDisasmAsmDisasmAsm() throws IOException {
        initTestClassData(TestedHelloWorld.class);
        byte[] data = Files.readAllBytes(new File(classFile).toPath());
        LogAndTextResults o1 = jdis(data);
        LogAndBinResults o2 = jasm(o1.output.getOutputs().get(0).getBody());
        LogAndTextResults o3 = jdis(o2.output.getOutputs().get(0).getBody());
        LogAndBinResults o4 = jasm(o3.output.getOutputs().get(0).getBody());
        Assertions.assertArrayEquals(new int[]{0,0,0,0}, new int[]{o1.result, o2.result, o3.result, o4.result});
    }

}
