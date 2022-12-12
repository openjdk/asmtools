package org.openjdk.asmtools;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openjdk.asmtools.common.CompileAction;
import org.opentest4j.MultipleFailuresError;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SequenceCallsTests {
    final String TEST_RESOURCE_FOLDER = "sequence" + File.separator;

    private List<Error> errors = Collections.synchronizedList(new ArrayList<>());

    CompileAction sequenceCompiler;
    File resourceDir;
    HashMap<String, List<List<String>>> cases = new HashMap<>() {{
        put("jasm",
                List.of(
                        List.of("EnclMethTest.jcod"),
                        List.of("ConstantPoolTestDummy.jasm"),
                        List.of("Not_Found_1.jasm"),
                        List.of("nonvoidinit.jasm", "nonvoidinit.jasm"),
                        List.of("Not_Found_2.jasm"),
                        List.of("TestInterface01n01.jasm")
                )
        );
        put("jcoder",
                List.of(
                        List.of("EnclMethTest.jcod"),
                        List.of("ConstantPoolTestDummy.jasm"),
                        List.of("Not_Found_I.jcod"),
                        List.of("module-info.jcod"),
                        List.of("atrcod00402m1n.jcod", "stackmap00601m1n.jcod"),
                        List.of("Not_Found_II.jcod")
                )
        );
    }};

    @BeforeAll
    public void init() throws IOException {
        String resName = TEST_RESOURCE_FOLDER + "EnclMethTest.jcod";        // must exist to get a correct path to files
        File file = new File(this.getClass().getResource(resName).getFile());
        resourceDir = file.getParentFile();
        sequenceCompiler = new CompileAction();
    }

    public class CaseExecutor implements Runnable {
        private AtomicInteger counter = new AtomicInteger(0);

        public void run() throws MultipleFailuresError {
            int index = counter.getAndIncrement();
            List<String> jasmFiles = cases.get("jasm").get(index).stream().
                    map(f -> resourceDir + File.separator + f).collect(Collectors.toList());
            List<String> jcodFiles = cases.get("jcoder").get(index).stream().
                    map(f -> resourceDir + File.separator + f).collect(Collectors.toList());
            if (index % 2 == 0) {
                assertAll(() -> sequenceCompiler.jasm(jasmFiles),
                        () -> sequenceCompiler.jcoder(jcodFiles));
            } else {
                assertAll(() -> sequenceCompiler.jcoder(jcodFiles),
                        () -> sequenceCompiler.jasm(jasmFiles));
            }
        }
    }

    @Test
    public void testCompilersWithConcurrency() throws InterruptedException {
        int numberOfThreads = cases.get("jasm").size();
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        CaseExecutor caseExecutor = new CaseExecutor();
        for (int i = 0; i < numberOfThreads; i++) {
            service.execute(() -> {
                        try {
                            caseExecutor.run();
                        } catch (MultipleFailuresError error) {
                            errors.add(error);
                        }
                        latch.countDown();
                    }
            );
        }
        latch.await();
        //
        assertEquals(6,
                errors.stream().mapToInt(e -> ((MultipleFailuresError) e).getFailures().size()).sum(),
                "Expected 4 missing & 2 wrong format files.");
    }
}
