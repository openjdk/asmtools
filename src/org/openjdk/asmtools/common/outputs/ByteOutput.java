/*
 * Copyright (c) 2023, Oracle, Red Hat  and/or theirs affiliates. All rights reserved.
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
package org.openjdk.asmtools.common.outputs;

import org.openjdk.asmtools.common.Environment;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class ByteOutput extends NamedToolOutput {
    private final ArrayList<NamedBinary> outputs = new ArrayList<>();
    private ByteArrayOutputStream currentClass;

    public ArrayList<NamedBinary> getOutputs() {
        return outputs;
    }

    @Override
    public String toString() {
        return outputs.stream().map(a -> a.toString()).collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public DataOutputStream getDataOutputStream() throws FileNotFoundException {
        return new DataOutputStream(currentClass);
    }


    @Override
    public void startClass(String fullyQualifiedName, Optional<String> suffix, Environment logger) throws IOException {
        super.startClass(fullyQualifiedName, suffix, logger);
        currentClass = new ByteArrayOutputStream(1024);
    }

    @Override
    public void finishClass(String fqn) throws IOException {
        if (!getCurrentClassName().equals(fqn)) {
            throw new RuntimeException("Ended different class - " + fqn + " - then started - " + super.fqn);
        }
        outputs.add(new NamedBinary(fqn, currentClass.toByteArray()));
        super.fqn = null;
        currentClass = null;

    }

    @Override
    public void printlns(String line) {
        try {
            currentClass.write((line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void prints(String line) {
        try {
            currentClass.write(line.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void prints(char line) {
        currentClass.write(line);
    }

    @Override
    public void flush() {

    }

    public class NamedBinary {
        private final String fqn;
        private final byte[] body;

        public NamedBinary(String fqn, byte[] body) {
            this.fqn = fqn;
            this.body = body;
        }

        public String getFqn() {
            return fqn;
        }

        public byte[] getBody() {
            return body;
        }

        @Override
        public String toString() {
            return fqn + ": " + body.length + "b";
        }
    }
}
