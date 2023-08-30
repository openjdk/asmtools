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

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class TextOutput extends NamedToolOutput {

    // decoration for text output
    private BiFunction<String, String, String> namedSourceOrnament = null;

    private final ArrayList<NamedSource> outputs = new ArrayList<>();
    private StringBuilder currentClass;

    public ArrayList<NamedSource> getOutputs() {
        return outputs;
    }

    @Override
    public String toString() {
        return outputs.stream().map(a -> a.toString()).collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public DataOutputStream getDataOutputStream() throws FileNotFoundException {
        return null; //If you are here, you probably wanted ToolOutput.ByteOutput for assembled binary output
    }

    @Override
    public void startClass(String fullyQualifiedName, Optional<String> suffix, Environment logger) throws IOException {
        super.startClass(fullyQualifiedName, suffix, logger);
        currentClass = new StringBuilder();
    }

    @Override
    public void finishClass(String fullyQualifiedName) throws IOException {
        if (!getCurrentClassName().equals(fullyQualifiedName)) {
            throw new RuntimeException("Ended different class - " + fullyQualifiedName + " - then started - " + super.fqn);
        }
        outputs.add(new NamedSource(fullyQualifiedName, currentClass.toString(), namedSourceOrnament));
        super.fqn = null;
        currentClass = null;
    }

    public TextOutput setNamedSourceOrnament(BiFunction<String, String, String> namedSourceOrnament) {
        this.namedSourceOrnament = namedSourceOrnament;
        return this;
    }

    @Override
    public void printlns(String line) {
        currentClass.append(line).append("\n");
    }

    @Override
    public void prints(String line) {
        currentClass.append(line);
    }

    @Override
    public void prints(char line) {
        currentClass.append(line);
    }

    @Override
    public void flush() {
    }

    public class NamedSource {
        // decoration for text output
        private BiFunction<String, String, String> ornament = (fname, body) ->
                format(
                        """
                        /**
                        %s
                        **/
                        %s
                        /**
                        %s
                        **/        
                        """, fname, body, fname);
        private final String fullyQualifiedName;
        private final String body;

        public NamedSource(String fullyQualifiedName, String body) {
            this.fullyQualifiedName = fullyQualifiedName;
            this.body = body;
        }

        public NamedSource(String fullyQualifiedName, String body, BiFunction<String, String, String> ornament) {
            this.fullyQualifiedName = fullyQualifiedName;
            this.body = body;
            this.ornament = ornament;
        }

        public NamedSource setOrnament(BiFunction<String, String, String> ornament) {
            this.ornament = ornament;
            return this;
        }

        public String getFullyQualifiedName() {
            return fullyQualifiedName;
        }

        public String getBody() {
            return body;
        }

        @Override
        public String toString() {
            return ornament == null ? body : ornament.apply(fullyQualifiedName, body);
        }
    }
}
