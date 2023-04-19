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
package org.openjdk.asmtools.ext;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.*;
import java.util.function.Function;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE, METHOD})
@Retention(RUNTIME)
@Documented
@ExtendWith(CaptureSystemOutputExtension.class)
public @interface CaptureSystemOutput {
    Kind value() default Kind.OUTPUT;

    boolean mute() default false;

    enum Kind {OUTPUT, ERROR, BOTH}

    class OutputCapture {
        List<Matcher<? super String>> strMatchers = new ArrayList<>();
        List<Matcher<? super Collection<String>>> listMatchers = new ArrayList<>();
        Optional<Function<String, String>> transformer = Optional.empty();
        Optional<Function<String, List<String>>> listTransformer = Optional.empty();
        private CaptureOutputStream captureOut;
        private CaptureOutputStream captureErr;
        private OutputStream log;
        private OutputStream err;

        CaptureSystemOutput.OutputCapture capture(Kind kind, boolean muted) {
            if (kind == Kind.OUTPUT || kind == Kind.BOTH) {
                log = new ByteArrayOutputStream();
                this.captureOut = new CaptureOutputStream(System.out, log, muted);
                System.setOut(new PrintStream(this.captureOut));
            }
            if (kind == Kind.ERROR || kind == Kind.BOTH) {
                err = new ByteArrayOutputStream();
                this.captureErr = new CaptureOutputStream(System.err, err, muted);
                System.setErr(new PrintStream(this.captureErr));
            }
            return this;
        }

        public CaptureSystemOutput.OutputCapture release() {
            flush();
            if (this.captureOut != null)
                System.setOut(this.captureOut.getOriginalStream());
            if (this.captureErr != null)
                System.setErr(this.captureErr.getOriginalStream());
            strMatchers = new ArrayList<>();
            try {
                if (log != null)
                    log.close();
                if (err != null)
                    err.close();
            } catch (IOException ex) { /* no-op */ }
            return this;
        }

        CaptureSystemOutput.OutputCapture flush() {
            try {
                if (this.captureOut != null)
                    this.captureOut.flush();
            } catch (IOException ex) { /* no-op */ }
            try {
                if (this.captureErr != null)
                    this.captureErr.flush();
            } catch (IOException ex) { /* no-op */ }
            return this;
        }

        public void expect(Matcher<? super String> matcher) {
            this.strMatchers.add(matcher);
        }

        public void expectForList(Matcher<? super Collection<String>> matcher) {
            this.listMatchers.add(matcher);
        }

        public OutputCapture useStringTransformer(Function<String, String> transformer) {
            this.transformer = Optional.of(transformer);
            return this;
        }

        public OutputCapture useListTransformer(Function<String, List<String>> transformer) {
            this.listTransformer = Optional.of(transformer);
            return this;
        }

        public String getLogAsString(Kind kind) {
            return this.transformer.orElse(s -> s).apply(getCapture(kind).toString());
        }

        public List<String> getLogAsList(Kind kind) {
            return this.listTransformer.
                    orElse(s -> Arrays.stream(s.split(System.lineSeparator())).toList()).
                    apply(getCapture(kind).toString());
        }

        public String[] getLogAsArray(Kind kind) {
            return getLogAsList(kind).toArray(String[]::new);
        }

        private String getCapture(Kind kind) {
            return switch (kind) {
                case OUTPUT -> (log != null) ? log.toString() : "";
                case ERROR -> (err != null) ? err.toString() : "";
                case BOTH -> ((log != null) ? log.toString() : "").concat((err != null) ? err.toString() : "");
            };
        }

        private static class CaptureOutputStream extends OutputStream {
            private final PrintStream originalStream;
            private final OutputStream log;
            private final boolean muted;

            CaptureOutputStream(PrintStream originalStream, OutputStream log, boolean muted) {
                this.originalStream = originalStream;
                this.log = log;
                this.muted = muted;
            }

            PrintStream getOriginalStream() {
                return this.originalStream;
            }

            @Override
            public void write(int b) throws IOException {
                if (!muted)
                    originalStream.write(b);
                log.write(b);
            }

            @Override
            public void flush() throws IOException {
                this.originalStream.flush();
            }

            @Override
            public void close() {
                this.originalStream.flush();
            }
        }
    }
}
