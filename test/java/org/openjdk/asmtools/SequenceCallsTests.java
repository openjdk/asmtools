/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.asmtools;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openjdk.asmtools.lib.action.CompileAction;
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

    private final List<Error> errors = Collections.synchronizedList(new ArrayList<>());
    private final List<Integer> results = Collections.synchronizedList(new ArrayList<>());

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
                assertAll(() -> {
                            var l = sequenceCompiler.jasm(jasmFiles);
                            results.add(l.result);
                        },
                        () -> {
                            var l = sequenceCompiler.jcoder(jcodFiles);
                            results.add(l.result);
                        }
                );
            } else {
                assertAll(() -> {
                            var l = sequenceCompiler.jcoder(jcodFiles);
                            results.add(l.result);
                        },
                        () -> {
                            var l = sequenceCompiler.jasm(jasmFiles);
                            results.add(l.result);
                        }
                );
            }
        }
    }

    /**
     * This is the test for CODETOOLS-7903401 (https://bugs.openjdk.org/browse/CODETOOLS-7903401)
     * jtreg fails if set of jdk tests process jasm,jdis files with defects
     * <p>
     * jib make -- test TEST=test/hotspot/jtreg/runtime
     * Passed: runtime/classFileParserBug/BadInitMethod.java
     * nonvoidinit.jasm (29:20) Warning: <init> method cannot be an interface method
     * public abstract Method "<init>":"()I";
     * ^
     * Passed: runtime/cds/SharedBaseAddress.java#id1
     * Passed: runtime/classFileParserBug/FakeMethodAcc.java
     * switch from jcoder to jcoder
     * 1 warning(s)
     * jcoder- ERROR: (I18NResourceBundle) The warning message 'warn.init.in_int' not found
     * 1 error(s)
     * --------------------------------------------------
     * TEST: runtime/classFileParserBug/InitInInterface.java
     * TEST JDK: /Users/lkuskov/dev/openjdk/build/macosx-x64/images/jdk
     * <p>
     * ACTION: compile -- Failed. jasm failed
     * REASON: User specified action: run compile nonvoidinit.jasm voidinit.jasm
     * TIME: 0.257 seconds
     * messages:
     * command: compile /Users/lkuskov/dev/openjdk/test/hotspot/jtreg/runtime/classFileParserBug/nonvoidinit.jasm
     * /Users/lkuskov/dev/openjdk/test/hotspot/jtreg/runtime/classFileParserBug/voidinit.jasm
     * reason: User specified action: run compile nonvoidinit.jasm voidinit.jasm
     * <p>
     * The jtreg uses a single instance of asmtool during a test run that leads to error in switching between jasm
     * and jcoder environment. I.e. the environment is set to Jcod while jasm is processing sources
     * and therefore jasm can't find jasm-specific message in Jcoder environment.
     */
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
        // check OK's results
        //"Expected 4 missing plus 2 wrong format files.");
        assertEquals(12, results.size());
        Collections.sort(results);
        assertEquals(results.get(0), 0);
        assertEquals(results.get(5), 0);
        assertEquals(results.get(6), 1);
        assertEquals(results.get(8), 1);
        assertEquals(results.get(9), 2);
        assertEquals(results.get(10), 2);
        assertEquals(results.get(11), 8);
    }
}
