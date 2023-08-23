/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.ToolOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;

import static java.lang.String.format;
import static org.openjdk.asmtools.asmutils.StringUtils.repeat;
import static org.openjdk.asmtools.common.CompilerConstants.OFFSET_BITS;
import static org.openjdk.asmtools.common.EMessageKind.ERROR;
import static org.openjdk.asmtools.common.EMessageKind.WARNING;
import static org.openjdk.asmtools.common.Environment.OK;
import static org.openjdk.asmtools.common.structure.CFVersion.DEFAULT_MAJOR_VERSION;
import static org.openjdk.asmtools.common.structure.CFVersion.DEFAULT_MINOR_VERSION;

// error,warning and info message is general and attached to a position of a scanned file.
public class CompilerLogger extends ToolLogger implements ILogger {

    // Message Container
    private final Map<Integer, Set<Message>> container = new HashMap<>();
    private final List<String> fileContent = new ArrayList<>();

    /**
     * @param programName the tool name
     * @param cls         the environment class of the tool for which to obtain the resource bundle
     * @param outerLog    the logger stream
     */
    public CompilerLogger(String programName, Class<?> cls, DualStreamToolOutput outerLog) {
        super(programName, cls, outerLog);
    }

    @Override
    public void warning(int where, String id, Object... args) {
        Message message = getResourceString(WARNING, id, args);
        if (message.notFound()) {
            if (EMessageKind.isFromResourceBundle(id)) {
                insert(NOWHERE, new Message(ERROR, "(I18NResourceBundle) The warning message '%s' not found", id));
            } else {
                insert(where, new Message((strictWarnings) ? ERROR : WARNING, args.length == 0 ? id : format(id, args)));
            }
        } else {
            insert(where, strictWarnings ? new Message(ERROR, message.text()) : message);
        }
    }

    @Override
    public void error(int where, String id, Object... args) {
        Message message = getResourceString(ERROR, id, args);
        if (message.notFound()) {
            if (EMessageKind.isFromResourceBundle(id)) {
                insert(NOWHERE, new Message(ERROR, "(I18NResourceBundle) The error message '%s' not found", id));
            } else {
                insert(where, new Message(ERROR, args.length == 0 ? id : format(id, args)));
            }
        } else {
            insert(where, message);
        }
    }

    @Override
    public String getInfo(String id, Object... args) {
        String message = getResourceString(id, args);
        if (message == null) {
            if (EMessageKind.isFromResourceBundle(id)) {
                printErrorLn("(I18NResourceBundle) The info message '%s' not found", id);
            } else {
                println(id, args);
            }
        }
        return message;
    }

    @Override
    public void usage(List<String> usageIDs) {
        for (String id : usageIDs) {
            String s = id.equals("info.opt.cv") ? getInfo(id, DEFAULT_MAJOR_VERSION, DEFAULT_MINOR_VERSION) :
                    getInfo(id);
            if (s != null) {
                Matcher m = usagePattern.matcher(s);
                if (m.find()) {
                    println(format("  %-35s %s", m.group(1).trim(), m.group(2).trim()));
                } else {
                    println(s);
                }
            }
        }
    }

    @Override
    public void setInputFileName(ToolInput inputFileName) throws IOException {
        super.setInputFileName(inputFileName);
        fileContent.clear();
        fileContent.addAll(inputFileName.readAllLines());
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
            final int lineNumber = lineNumber(where), absPos = where & ((1 << OFFSET_BITS) - 1);
            int lineOffset = absPos - (fileContent.subList(0, lineNumber - 1).stream().mapToInt(String::length).sum() + lineNumber - 1);
            return new Pair<>(lineNumber, lineOffset);
        }
    }

    public int lineNumber(int where) {
        return where >>> OFFSET_BITS;
    }

    public long getCount(EMessageKind kind) {
        return noMessages() ? 0 : container.values().stream().flatMap(Collection::stream).filter(m -> m.kind() == kind).count();
    }

    public boolean noMessages() {
        return container.isEmpty();
    }

    /**
     * @param printTotals whether to print the total line: N warning(s), K error(s)
     * @return 0 if there are no errors otherwise a number of errors
     */
    public synchronized int flush(boolean printTotals) {
        if (noMessages()) return OK;
        int nErrors = 0, nWarnings = 0;
        List<Map.Entry<Integer, Set<Message>>> list = new ArrayList<>(container.entrySet());
        list.sort(Map.Entry.comparingByKey());
        ToolOutput output = getOutputs().getSToolObject();
        for (Map.Entry<Integer, Set<Message>> entry : list) {
            int where = entry.getKey();
            Pair<Integer, Integer> filePosition = filePosition(where);
            for (Message msg : entry.getValue()) {
                if( msg.kind() == WARNING && ignoreWarnings) {
                   continue;
                }
                if (msg.kind() == ERROR) {
                    output = getOutputs().getEToolObject();
                    nErrors++;
                }
                nWarnings += msg.kind() == WARNING ? 1 : 0;
                if (where == NOWHERE) {
                    // direct message isn't connected to a position in a scanned file
                    output.printlns(msg.text());
                } else {
                    output.printlns(format("%s (%d:%d) %s", getSimpleInputFileName(),
                            filePosition.first, filePosition.second,
                            msg.text()));
                    printAffectedSourceLine(output, filePosition);
                }
            }
        }
        DualStreamToolOutput totalOutput = (printTotals) ? getOutputs() : null;
        if (printTotals) {
            if (nWarnings != 0)
                totalOutput.printe(format("%d warning(s)%s", nWarnings, nErrors != 0 ? ", " : "\n"));
            if (nErrors != 0)
                totalOutput.printlne(format("%d error(s)", nErrors));
        }

        synchronized (output) {
            output.flush();
            if (totalOutput != null) {
                totalOutput.flush();
            }
            container.clear();
        }

        return nErrors;
    }

    // Removes tabs from a source line to get correct line position while printing.
    private void printAffectedSourceLine(ToolOutput output, Pair<Integer, Integer> filePosition) {
        String line = fileContent.get(filePosition.first - 1);
        long countOfExtraSpaces = line.chars().filter(ch -> ch == '\t').count();
        long linePosition = (filePosition.second + countOfExtraSpaces * TAB_REPLACEMENT.length()) - countOfExtraSpaces;
        line = line.replace("\t", TAB_REPLACEMENT);
        output.printlns(line);
        output.printlns(repeat(" ", (int) linePosition) + "^");
    }

    /**
     * Insert a message in the list of outstanding messages. The list is sorted on input position.
     */
    private void insert(int where, Message message) {
        if (where != NOWHERE && fileContent.isEmpty()) {
            addToContainer(NOWHERE,
                    new Message(ERROR, EMessageFormatter.LONG.apply(ERROR, this.getProgramName(),
                            "Content of the file %s not found", getSimpleInputFileName())));
            where = NOWHERE;
        }
        // message format
        addToContainer(where, new Message(message.kind(), where == NOWHERE ?
                EMessageFormatter.VERBOSE.apply(this.getProgramName(), message) :
                EMessageFormatter.LONG.apply(this.getProgramName(), message)));
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
