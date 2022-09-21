package org.openjdk.asmtools.jdec;

import org.junit.jupiter.api.Assertions;
import org.openjdk.asmtools.BruteForceHelper;
import org.openjdk.asmtools.ThreeStringWriters;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

class JdecJcod {

    private final boolean g;
    private final BruteForceHelper worker;

    public JdecJcod(boolean g, BruteForceHelper worker) {
        this.g = g;
        this.worker = worker;
    }

    public void run() throws IOException {
        BruteForceHelper.AsmToolsExecutable jdec = new BruteForceHelper.AsmToolsExecutable() {
            @Override
            public int run(ThreeStringWriters outs, File clazz) throws IOException {
                Main decoder;
                if (g) {
                    decoder = new Main(outs.getToolOutputWrapper(), outs.getLoggers(), "-g", clazz.getAbsolutePath());
                } else {
                    decoder = new Main(outs.getToolOutputWrapper(), outs.getLoggers(), clazz.getAbsolutePath());
                }
                return decoder.decode();
            }

            @Override
            public void ensure(List<File> all, Map<File, ThreeStringWriters> failures) {
                String gs = "";
                if (g) {
                    gs = "with -g ";
                }
                Assertions.assertEquals(0, failures.size(), "from " + all.size() + "(" + worker.getClassesRoot() + ") failed to decode " + gs + failures.size() + ": " + BruteForceHelper.keySetToString(failures, worker.getClassesRoot()));
            }
        };
        BruteForceHelper.AsmToolsExecutable jasm = new JasmToolExecutable(worker, g);
        worker.work(jdec, jasm);
    }

    private static class JasmToolExecutable implements BruteForceHelper.AsmToolsExecutable {
        private final BruteForceHelper worker;
        private final String g;

        public JasmToolExecutable(BruteForceHelper worker, boolean g) {
            this.worker = worker;
            if (g) {
                this.g = " (from -g decode) ";
            } else {
                this.g = "";
            }
        }

        @Override
        public int run(ThreeStringWriters outs, File clazz) throws IOException {
            File savedCode = BruteForceHelper.saveDecompiledCode(worker.getDecompiledClass(clazz), "JdecJcodWorks");
            org.openjdk.asmtools.jcoder.Main coder = new org.openjdk.asmtools.jcoder.Main(outs.getLoggers(), savedCode.getAbsolutePath(), "-d", worker.getCompileDir().getAbsolutePath());
            BruteForceHelper.createMetadata(outs, clazz, savedCode, worker.getCompileDir(), worker.getClassesRoot());
            return coder.compile();
        }

        @Override
        public void ensure(List<File> all, Map<File, ThreeStringWriters> failures) {
            Assertions.assertEquals(0, failures.size(), "from " + all.size() + " failed to encode " + g + " to (" + worker.getCompileDir() + ") " + failures.size() + ": " + BruteForceHelper.keySetToString(failures, worker.getClassesRoot()));
        }
    }
}

