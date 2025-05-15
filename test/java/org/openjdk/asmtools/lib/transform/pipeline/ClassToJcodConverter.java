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
import org.openjdk.asmtools.jdec.Main;
import org.openjdk.asmtools.lib.utility.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

import static org.openjdk.asmtools.lib.transform.pipeline.AsmtoolsType.FileType.JCOD;
import static org.openjdk.asmtools.lib.transform.pipeline.Pipeline.*;

public class ClassToJcodConverter extends Converter implements Function<Clazz, Jcod> {
    private static final String DEFAULT_STAGE_NAME = "ClassToJcod";

    public ClassToJcodConverter(String stageName) {
        super(Objects.requireNonNullElse(stageName, DEFAULT_STAGE_NAME));
    }

    public ClassToJcodConverter() {
        this(null);
    }

    @Override
    public Jcod apply(Clazz incoming) {
        logInfo("Generating: class -> jcod");
        Objects.requireNonNull(incoming, "Incoming Jcod cannot be null");

        if (incoming.firstInput()) {
            logDebug(stageName(), "Received first input: " + incoming);
        } else {
            logDebug(stageName(), "Received from previous stage: " + incoming);
        }

        if (!incoming.firstInput() && incoming.record().toolReturn() != SUCCESS) {
            logWarn(stageName(), "Previous stage failed. Delegating...");
            return handleError("Previous stage failed", incoming, incoming.record().log(), incoming.record().toolReturn());
        }

        if (incoming.record().file() == null) {
            return handleError("Incoming Clazz file is null", incoming, null, -1);
        }

        try {
            byte[] classBytes = FileUtils.readBytesFromFile(incoming.record().file());
            ByteOutput output = new ByteOutput();
            StringLog log = new StringLog();
            Main compiler = new Main(output, log, new ByteInput(classBytes));
            int result = compiler.decode();
            if (result != SUCCESS) {
                return handleError(stageName() + " failed.", incoming, log, result);
            }
            if (output.getOutputs() == null || output.getOutputs().isEmpty() || output.getOutputs().get(0) == null) {
                return handleError("jdec ToolOutput not available", incoming, log, result);
            } else {
                logDebug(stageName(), "jdec succeeded.");
            }
            Path jcodFilePath = FileUtils.writeBytesToFile("temp", JCOD.extension(), output.getOutputs().get(0).getBody());
            Jcod r = new Jcod(new Pipeline.Status(stageName(), jcodFilePath, log, result, output));
            logDebug(stageName(), "Sending next stage: " + r);
            return r;
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert CLASS to JCOD", e);
        }
    }

    protected Jcod handleError(String errorMessage, Clazz type, StringLog log, int result) {
        logWarn(errorMessage);
        Pipeline.Status status = new Pipeline.Status(stageName(), type.record().file(), log, result, null);
        setRecord(status);
        return new Jcod(status);
    }
}
