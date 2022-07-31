package org.openjdk.asmtools.jdis;

import org.junit.jupiter.api.Test;
import org.openjdk.asmtools.BruteForceHelper;

import java.io.IOException;

class JdisJasmTest {

    @Test
    public void jdisJasmAllDecompileCompileAndLoad() throws IOException {
        BruteForceHelper worker = new BruteForceHelper();
        new JdisJasm(false, worker).run();
    }

    @Test
    public void jdisGJasmAllDecompileCompileAndLoad() throws IOException {
        BruteForceHelper worker = new BruteForceHelper();
        new JdisJasm(true, worker).run();

    }
}

