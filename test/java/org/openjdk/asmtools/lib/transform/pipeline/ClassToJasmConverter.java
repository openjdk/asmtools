/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.asmtools.lib.transform.pipeline;

import org.openjdk.asmtools.common.inputs.ByteInput;
import org.openjdk.asmtools.common.outputs.ByteOutput;
import org.openjdk.asmtools.common.outputs.log.StringLog;
import org.openjdk.asmtools.jdis.Main;
import org.openjdk.asmtools.lib.utility.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

import static org.openjdk.asmtools.lib.transform.pipeline.AsmtoolsType.FileType.JASM;
import static org.openjdk.asmtools.lib.transform.pipeline.Pipeline.*;

public class ClassToJasmConverter extends Converter implements Function<Clazz, Jasm> {
    private static final String DEFAULT_STAGE_NAME = "ClassToJasm";
    private static final String JDIS_DETAILED_OUTPUT = "-g";

    public ClassToJasmConverter(String stageName) {
        super(Objects.requireNonNullElse(stageName, DEFAULT_STAGE_NAME));
    }

    public ClassToJasmConverter() {
        this(null);
    }


    @Override
    public Jasm apply(Clazz incoming) {
        System.out.println("Generating: class -> jasm");
        Objects.requireNonNull(incoming, "Incoming Clazz cannot be null");

        logDebug(stageName(), incoming.firstInput() ? "Received first input: " + incoming : "Received from previous stage: " + incoming);

        if (!incoming.firstInput() && incoming.record().toolReturn() != SUCCESS) {
            return handleError("Previous stage failed", incoming, incoming.record().log(), incoming.record().toolReturn());
        }

        if (incoming.record().file() == null) {
            return handleError("Incoming Clazz file is null", incoming, null, -1);
        }

        try {
            byte[] clazzBytes = FileUtils.readBytesFromFile(incoming.record().file());
            ByteOutput output = new ByteOutput();
            StringLog log = new StringLog();
            Main compiler = new Main(output, log, new ByteInput(clazzBytes), JDIS_DETAILED_OUTPUT);
            int result = compiler.disasm();

            if (result != SUCCESS) {
                return handleError(stageName() + " failed.", incoming, log, result);
            }

            if (output.getOutputs() == null || output.getOutputs().isEmpty() || output.getOutputs().get(0) == null) {
                return handleError("jdis ToolOutput not available", incoming, null, result);
            }

            logDebug(stageName(), "jdis succeeded.");
            Path classFilePath = FileUtils.writeBytesToFile("temp", JASM.extension(), output.getOutputs().get(0).getBody());
            Jasm r = handleOutput(incoming, classFilePath, log, result, output);
            logDebug(stageName(), "Sending next stage: " + r);
            return r;
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert JASM to CLASS", e);
        }
    }

    protected Jasm handleError(String errorMessage, Clazz type, StringLog log, int result) {
        logWarn(errorMessage);
        Pipeline.Status status = new Pipeline.Status(stageName(), type.record().file(), log, result, null);
        setRecord(status);
        return new Jasm(status);
    }

    protected Jasm handleOutput(Clazz type, Path classFilePath, StringLog log, int result, ByteOutput output) {
        Pipeline.Status status = new Pipeline.Status(stageName(), classFilePath, log, result, output);
        setRecord(status);
        return new Jasm(status);
    }
}
