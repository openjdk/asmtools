package org.openjdk.asmtools.lib.script;

import org.junit.jupiter.api.Assertions;
import org.openjdk.asmtools.common.inputs.StringInput;
import org.openjdk.asmtools.lib.action.*;
import org.openjdk.asmtools.lib.log.LogAndBinResults;
import org.openjdk.asmtools.lib.log.LogAndTextResults;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.fail;
import static org.openjdk.asmtools.lib.utility.StringUtils.funcNormalizeText;

public class TestScript {
    protected Jasm jasm = new Jasm();
    private Jcoder jcoder = new Jcoder();
    protected File resourceDir;
    protected boolean warningsEnabled = false;

    public void init(String resource) throws IOException {
        if (resource == null) {
            throw new IOException("Resource not declared");
        }
        URL url = this.getClass().getResource(resource);
        if (url == null) {
            fail("Resource \"%s\" not found.".formatted(resource));
        }
        this.resourceDir = new File(url.getFile()).getParentFile();
        if (!this.resourceDir.exists() || !this.resourceDir.isDirectory()) {
            fail("Resource directory does not exist or is not a directory: " + this.resourceDir);
        } else if (DebugHelper.isDebug()) {
            fail("Resource directory initialized: " + this.resourceDir.getAbsolutePath());
        }
    }

    public void jasmTest(String resourceName, EToolArguments args, List<Consumer<String>> tests) {
        // jasm to class in memory
        // jasm.setDebug(true);
        LogAndBinResults binResult = jasm.compile(List.of(resourceDir + File.separator + resourceName));
        // class produced correctly
        if( !warningsEnabled )
            Assertions.assertTrue(binResult.log.toString().isEmpty());
        Assertions.assertEquals(0, binResult.result);

        // class to jasm
        LogAndTextResults textResult = new Jdis().setArgs(args).decode(binResult.getAsByteInput());

        Assertions.assertEquals(0, textResult.result);
        String jasmText = textResult.getResultAsString(Function.identity());
        String normJasmText = funcNormalizeText.apply(jasmText);
        for (Consumer<String> testConsumer : tests) {
            testConsumer.accept(normJasmText);
        }
        // jasm to class
        binResult = jasm.compile(new StringInput(jasmText));
        // class produced correctly
        Assertions.assertEquals(0, binResult.result);
        if( !warningsEnabled )
            Assertions.assertTrue(binResult.log.toString().isEmpty());
        // class to jasm
        textResult = new Jdis().setArgs(args).decode(binResult.getAsByteInput());
        Assertions.assertEquals(0, textResult.result);
        jasmText = textResult.getResultAsString(Function.identity());
        normJasmText = funcNormalizeText.apply(jasmText);
        for (Consumer<String> testConsumer : tests) {
            testConsumer.accept(normJasmText);
        }
        // class to jcod
        textResult = new Jdec().setArgs(EToolArguments.JDEC_G).decode(binResult.getAsByteInput());
        Assertions.assertEquals(0, textResult.result);
        // jcod to class
        binResult = jcoder.compile(new StringInput(textResult.getResultAsString(Function.identity())));
        Assertions.assertEquals(0, binResult.result);
    }

    public void jcoderTest(String resourceName, EToolArguments args, List<Consumer<String>> tests) {
        // jcod to class in memory
        LogAndBinResults binResult = jcoder.compile(List.of(resourceDir + File.separator + resourceName));
        // class produced correctly
        Assertions.assertEquals(0, binResult.result);
        if( !warningsEnabled )
            Assertions.assertTrue(binResult.log.toString().isEmpty());
        // class to jcod
        LogAndTextResults textResult = new Jdec().setArgs(args).decode(binResult.getAsByteInput());
        String jcoderText = textResult.getResultAsString(funcNormalizeText);
        Assertions.assertEquals(0, textResult.result);
        for (Consumer<String> testConsumer : tests) {
            testConsumer.accept(jcoderText);
        }
        // class to jasm twice
        textResult = new Jdis().setArgs(EToolArguments.JDIS_G_T_LNT_LVT).decode(binResult.getAsByteInput());
        Assertions.assertEquals(0, textResult.result);
        textResult = new Jdis().setArgs(EToolArguments.JDIS_GG_NC_LNT_LVT).decode(binResult.getAsByteInput());
        Assertions.assertEquals(0, textResult.result);
    }

    protected void enableToolsWarnings() {
        this.warningsEnabled = true;
    }
}
