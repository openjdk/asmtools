package org.openjdk.asmtools.jdis;

import org.junit.jupiter.api.Assertions;
import org.openjdk.asmtools.BruteForceHelper;
import org.openjdk.asmtools.ThreeStringWriters;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

class JdisJasm {

    private final boolean g;
    private final BruteForceHelper worker;

    public JdisJasm(boolean g, BruteForceHelper worker) {
        this.g = g;
        this.worker = worker;
    }

    public void run() throws IOException {
        BruteForceHelper.AsmToolsExecutable jdis = new BruteForceHelper.AsmToolsExecutable() {
            @Override
            public int run(ThreeStringWriters outs, File clazz) throws IOException {
                Main disassem;
                if (g) {
                    disassem = new Main(outs.getToolOutputWrapper(), outs.getLoggers(), "-g", clazz.getAbsolutePath());
                } else {
                    disassem = new Main(outs.getToolOutputWrapper(), outs.getLoggers(), clazz.getAbsolutePath());
                }
                return disassem.disasm();
            }

            @Override
            public void ensure(List<File> all, Map<File, ThreeStringWriters> failures) {
                String gs = "";
                if (g) {
                    gs = "with -g ";
                }
                Assertions.assertEquals(0, failures.size(), "from " + all.size() + "(" + worker.getClassesRoot() + ") failed to disassemble " + gs + failures.size() + ": " + BruteForceHelper.keySetToString(failures, worker.getClassesRoot()));
            }
        };
        BruteForceHelper.AsmToolsExecutable jasm = new JasmToolExecutable(worker, g);
        worker.work(jdis, jasm);
    }

    private static class JasmToolExecutable implements BruteForceHelper.AsmToolsExecutable {
        private final BruteForceHelper worker;
        private final String g;

        public JasmToolExecutable(BruteForceHelper worker, boolean g) {
            this.worker = worker;
            if (g) {
                this.g = " (from -g disasm) ";
            } else {
                this.g = "";
            }
        }

        @Override
        public int run(ThreeStringWriters outs, File clazz) throws IOException {
            File savedAsm = BruteForceHelper.saveDecompiledCode(worker.getDecompiledClass(clazz), "JdisJasmWorks");
            org.openjdk.asmtools.jasm.Main asm = new org.openjdk.asmtools.jasm.Main(outs.getLoggers(), savedAsm.getAbsolutePath(), "-d", worker.getCompileDir().getAbsolutePath());
            BruteForceHelper.createMetadata(outs, clazz, savedAsm, worker.getCompileDir(), worker.getClassesRoot());
            return asm.compile();
        }

        @Override
        public void ensure(List<File> all, Map<File, ThreeStringWriters> failures) {
            //three classes now fails, they will fail again in attempt to be loaded on NPE
            Assertions.assertEquals(0, failures.size(), "from " + all.size() + " failed to assemble " + g + " to (" + worker.getCompileDir() + ") " + failures.size() + ": " + BruteForceHelper.keySetToString(failures, worker.getClassesRoot()));
        }
    }
}

