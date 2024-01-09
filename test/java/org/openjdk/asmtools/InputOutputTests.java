package org.openjdk.asmtools;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.openjdk.asmtools.common.inputs.ByteInput;
import org.openjdk.asmtools.common.inputs.StringInput;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.ByteOutput;
import org.openjdk.asmtools.common.outputs.TextOutput;
import org.openjdk.asmtools.common.outputs.log.StringLog;
import org.openjdk.asmtools.jdis.Options;
import org.openjdk.asmtools.lib.LogAndBinResults;
import org.openjdk.asmtools.lib.LogAndTextResults;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class InputOutputTests extends ClassPathClassWork {

    @AfterAll
    public static void unsetDetailedOutputOptions() {
        Options.unsetDetailedOutputOptions();
    }

    public LogAndTextResults jdec(boolean g, byte[]... clazz) {
        ToolInput[] originalFiles = new ToolInput[clazz.length];
        for (int i = 0; i < clazz.length; i++) {
            originalFiles[i] = new ByteInput(clazz[i]);
        }
        TextOutput decodedFiles = new TextOutput();
        StringLog decodeLog = new StringLog();
        org.openjdk.asmtools.jdec.Main jdec = new org.openjdk.asmtools.jdec.Main(decodedFiles, decodeLog, originalFiles);
        jdec.setVerboseFlag(true);
        jdec.setTraceFlag(true);
        if (g) {
            jdec.setPrintDetails(true);
        } else {
            jdec.setPrintDetails(false);
        }
        int r = jdec.decode();
        return new LogAndTextResults(decodedFiles, decodeLog, r);
    }

    public LogAndBinResults jcod(String... clazz) {
        ToolInput[] originalFiles = new ToolInput[clazz.length];
        for (int i = 0; i < clazz.length; i++) {
            originalFiles[i] = new StringInput(clazz[i]);
        }
        ByteOutput encodedFiles = new ByteOutput();
        StringLog encodeLog = new StringLog();
        org.openjdk.asmtools.jcoder.Main jcod = new org.openjdk.asmtools.jcoder.Main(encodedFiles, encodeLog, originalFiles);
        jcod.setVerboseFlag(true);
        jcod.setTraceFlag(true);
        int r = jcod.compile();
        return new LogAndBinResults(encodedFiles, encodeLog, r);
    }

    public LogAndTextResults jdis(boolean g, byte[]... clazz) {
        ToolInput[] originalFiles = new ToolInput[clazz.length];
        for (int i = 0; i < clazz.length; i++) {
            originalFiles[i] = new ByteInput(clazz[i]);
        }
        TextOutput decodedFiles = new TextOutput();
        StringLog decodeLog = new StringLog();
        org.openjdk.asmtools.jdis.Main jdis = new org.openjdk.asmtools.jdis.Main(decodedFiles, decodeLog, originalFiles);
        jdis.setVerboseFlag(true);
        jdis.setTraceFlag(true);
        if (g) {
            Options.setDetailedOutputOptions();
        } else {
            Options.unsetDetailedOutputOptions();
        }
        int r = jdis.disasm();
        return new LogAndTextResults(decodedFiles, decodeLog, r);
    }

    public LogAndBinResults jasm(String... clazz) {
        ToolInput[] originalFiles = new ToolInput[clazz.length];
        for (int i = 0; i < clazz.length; i++) {
            originalFiles[i] = new StringInput(clazz[i]);
        }
        ByteOutput encodedFiles = new ByteOutput();
        StringLog encodeLog = new StringLog();
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
        LogAndTextResults o1 = jdec(false, data);
        LogAndBinResults o2 = jcod(o1.output.getOutputs().get(0).getBody());
        LogAndTextResults o3 = jdec(false, o2.output.getOutputs().get(0).getBody());
        LogAndBinResults o4 = jcod(o3.output.getOutputs().get(0).getBody());
        Assertions.assertArrayEquals(new int[]{0, 0, 0, 0}, new int[]{o1.result, o2.result, o3.result, o4.result});
    }

    @Test
    public void inMemoryDisasmAsmDisasmAsm() throws IOException {
        initTestClassData(TestedHelloWorld.class);
        byte[] data = Files.readAllBytes(new File(classFile).toPath());
        LogAndTextResults o1 = jdis(false, data);
        LogAndBinResults o2 = jasm(o1.output.getOutputs().get(0).getBody());
        LogAndTextResults o3 = jdis(false, o2.output.getOutputs().get(0).getBody());
        LogAndBinResults o4 = jasm(o3.output.getOutputs().get(0).getBody());
        Assertions.assertArrayEquals(new int[]{0, 0, 0, 0}, new int[]{o1.result, o2.result, o3.result, o4.result});
    }


    @Test
    public void inMemoryDecCodDecCodG() throws IOException {
        initTestClassData(TestedHelloWorld.class);
        byte[] data = Files.readAllBytes(new File(classFile).toPath());
        LogAndTextResults o1 = jdec(true, data);
        LogAndBinResults o2 = jcod(o1.output.getOutputs().get(0).getBody());
        LogAndTextResults o3 = jdec(true, o2.output.getOutputs().get(0).getBody());
        LogAndBinResults o4 = jcod(o3.output.getOutputs().get(0).getBody());
        Assertions.assertArrayEquals(new int[]{0, 0, 0, 0}, new int[]{o1.result, o2.result, o3.result, o4.result});
    }

    @Test
    public void inMemoryDisasmAsmDisasmAsmG() throws IOException {
        initTestClassData(TestedHelloWorld.class);
        byte[] data = Files.readAllBytes(new File(classFile).toPath());
        LogAndTextResults o1 = jdis(true, data);
        LogAndBinResults o2 = jasm(o1.output.getOutputs().get(0).getBody());
        LogAndTextResults o3 = jdis(true, o2.output.getOutputs().get(0).getBody());
        LogAndBinResults o4 = jasm(o3.output.getOutputs().get(0).getBody());
        Assertions.assertArrayEquals(new int[]{0, 0, 0, 0}, new int[]{o1.result, o2.result, o3.result, o4.result});
    }

}
