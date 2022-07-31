package org.openjdk.asmtools.jdis;

import org.junit.jupiter.api.Test;
import org.openjdk.asmtools.BruteForceHelper;

import java.io.IOException;
import java.nio.file.Files;

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

    @Test
    public void jdisGJasmGsonWeirdDecompileCompileAndLoad() throws IOException {
        BruteForceHelper.SingleTestClassProvider cp = new BruteForceHelper.SingleTestClassProvider("/com/google/gson/Gson.class", "/com/google/gson/GsonWeird.class");
        BruteForceHelper worker = new BruteForceHelper(cp);
        try {
            new JdisJasm(true, worker).run();
        } finally {
            cp.getClasses().get(0).delete();
        }
    }

    @Test
    public void jdisJasmGsonWeirdDecompileCompileAndLoad() throws IOException {
        BruteForceHelper.SingleTestClassProvider cp = new BruteForceHelper.SingleTestClassProvider("/com/google/gson/Gson.class", "/com/google/gson/GsonWeird.class");
        BruteForceHelper worker = new BruteForceHelper(cp);
        try {
            new JdisJasm(false, worker).run();
        } finally {
            cp.getClasses().get(0).delete();
        }
    }


    @Test
    public void jdisGJasmGsonOrigDecompileCompileAndLoad() throws IOException {
        BruteForceHelper.SingleTestClassProvider cp = new BruteForceHelper.SingleTestClassProvider("/com/google/gson/Gson.class", "/com/google/gson/GsonOrig.class");
        BruteForceHelper worker = new BruteForceHelper(cp);
        try {
            new JdisJasm(true, worker).run();
        } finally {
            cp.getClasses().get(0).delete();
        }
    }
}

