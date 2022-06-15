/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.common;

import org.openjdk.asmtools.asmutils.Pair;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.String.format;
import static org.openjdk.asmtools.asmutils.StringUtils.repeat;
import static org.openjdk.asmtools.common.CompilerConstants.OFFSETBITS;
import static org.openjdk.asmtools.common.EMessageKind.ERROR;
import static org.openjdk.asmtools.common.EMessageKind.WARNING;
import static org.openjdk.asmtools.common.Environment.OK;

// error,warning and info message is general and attached to a position of a scanned file.
public class CompilerLogger extends ToolLogger implements ILogger {

    // Message Container
    private final Map<Integer, Set<Message>> container = new HashMap<>();
    private final List<String> fileContent = new ArrayList<>();

    public CompilerLogger(PrintWriter errLog, PrintWriter outLog) {
        super(errLog, outLog);
    }

    @Override
    public void warning(int where, String id, Object... args) {
        Message message = getResourceString(WARNING, id, args);
        if (message.notFound()) {
            if (EMessageKind.isFromResourceBundle(id))
                insert(NOWHERE, new Message(ERROR, "(I18NResourceBundle) The warning message '%s' not found", id));
            else
                message = new Message(WARNING, args.length == 0 ? id : format(id, args));
        }
        insert(where, message);
    }

    @Override
    public void error(int where, String id, Object... args) {
        Message message = getResourceString(ERROR, id, args);
        if (message.notFound()) {
            if (EMessageKind.isFromResourceBundle(id))
                insert(NOWHERE, new Message(ERROR, "(I18NResourceBundle) The error message '%s' not found", id));
            else
                message = new Message(ERROR, args.length == 0 ? id : format(id, args));
        }
        insert(where, message);
    }

    @Override
    public void info(String id, Object... args) {
        String message = getResourceString(id, args);
        if (message == null) {
            if (EMessageKind.isFromResourceBundle(id))
                printErrorLn("(I18NResourceBundle) The info message '%s' not found", id);
            else
                println(id, args);
        } else {
            println(message);
        }
    }

    @Override
    public void setInputFileName(String inputFileName) throws IOException {
        super.setInputFileName(inputFileName);
        fileContent.clear();
        fileContent.addAll(Files.readAllLines(Paths.get(inputFileName)));
    }

    /**
     * Gets a pair of line number and line position for the pointer where A position consists of: ((linenr >> OFFSETBITS) | offset)
     * this means that both the line number and the exact offset into the file are encoded in each position value.
     *
     * @param where absolute position in file = (linepos << OFFSETBITS) | bytepos;
     * @return the pair: [line number, line offset]
     */
    Pair<Integer, Integer> filePosition(int where) {
        if (where == NOWHERE || fileContent.isEmpty()) {
            return null;
        } else {
            final int lineNumber = lineNumber(where), absPos = where & ((1 << OFFSETBITS) - 1);
            int lineOffset = absPos - (fileContent.subList(0, lineNumber - 1).stream().mapToInt(String::length).sum() + lineNumber - 1);
            return new Pair<>(lineNumber, lineOffset);
        }
    }

    public int lineNumber(int where) {
        return where >>> OFFSETBITS;
    }

    public long getCount(EMessageKind kind) {
        return noMessages() ? 0 : container.values().stream().flatMap(Collection::stream).filter(m -> m.kind() == kind).count();
    }

    public boolean noMessages() {
        return container.isEmpty();
    }

    /**
     * @param printTotals whether to print the total line: N warning(s), K error(s)
     * @return 0 if there are no errors otherwise a numner of errors
     */
    public int flush(boolean printTotals) {
        if (noMessages()) return OK;
        int nErrors = 0, nWarnings = 0;
        List<Map.Entry<Integer, Set<Message>>> list = new ArrayList<>(container.entrySet());
        list.sort(Map.Entry.comparingByKey());
        for (Map.Entry<Integer, Set<Message>> entry : list) {
            int where = entry.getKey();
            Pair<Integer, Integer> filePosition = filePosition(where);
            for (Message msg : entry.getValue()) {
                PrintWriter output = msg.kind() == ERROR ? getErrLog() : getOutLog();
                nErrors += msg.kind() == ERROR ? 1 : 0;
                nWarnings += msg.kind() == WARNING ? 1 : 0;
                if (where == NOWHERE) {
                    // direct message isn't connected to a position in a scanned file
                    output.println(msg.text());
                } else {
                    output.println(format("%s (%d:%d) %s", getSimpleInputFileName(),
                            filePosition.first, filePosition.second,
                            msg.text()));
                    printAffectedSourceLine(output, filePosition);
                }
                output.flush();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {
                }
            }
        }
        if (printTotals && ( nWarnings !=0 || nErrors != 0)) {
            if (nWarnings != 0)
                getOutLog().print(format("%d warning(s)%s", nWarnings, nErrors != 0 ? ", " : ""));
            if (nErrors != 0)
                getOutLog().println(format("%d error(s)", nErrors));
            getOutLog().flush();
        }
        container.clear();
        return nErrors;
    }

    // Removes tabs from a source line to get correct line position while printing.
    private void printAffectedSourceLine(PrintWriter output, Pair<Integer, Integer> filePosition) {
        String line = fileContent.get(filePosition.first - 1);
        long countOfExtraSpaces = line.chars().filter(ch -> ch == '\t').count();
        long linePosition =  (filePosition.second + countOfExtraSpaces*TAB_REPLACEMENT.length()) - countOfExtraSpaces;
        line = line.replace("\t", TAB_REPLACEMENT);
        output.println(line);
        output.println(repeat(" ",  (int)linePosition) + "^");
    }

    /**
     * Insert a message in the list of outstanding messages. The list is sorted on input position.
     */
    private void insert(int where, Message message) {
        if (where != NOWHERE && fileContent.isEmpty()) {
            addToContainer(NOWHERE,
                    new Message(ERROR, EMessageFormatter.LONG.apply(ERROR,
                            "Content of the file %s not found", getSimpleInputFileName())));
            where = NOWHERE;
        }
        // message format
        addToContainer(where, new Message(message.kind(), where == NOWHERE ? EMessageFormatter.VERBOSE.apply(message) :
                EMessageFormatter.LONG.apply(message)));
    }

    private void addToContainer(int where, Message msg) {
        Set<Message> messages = container.get(where);
        if (messages != null) {
            messages.add(msg);
        } else {
            Set<Message> msgSet = new HashSet<>(1);
            msgSet.add(msg);
            container.put(where, msgSet);
        }
    }
}