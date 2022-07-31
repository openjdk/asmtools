package org.openjdk.asmtools.jdec;

import org.junit.jupiter.api.Test;
import org.openjdk.asmtools.BruteForceHelper;

import java.io.IOException;


class JdecJcodTest {

    @Test
    public void jdecJcodAllDecompileCompileAndLoad() throws IOException {
        BruteForceHelper worker = new BruteForceHelper();
        new JdecJcod(false, worker).run();
    }

    @Test
    public void jdecGJcodAllDecompileCompileAndLoad() throws IOException {
        BruteForceHelper worker = new BruteForceHelper();
        new JdecJcod(true, worker).run();

    }
}

