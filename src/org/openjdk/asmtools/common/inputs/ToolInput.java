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
package org.openjdk.asmtools.common.inputs;

import org.openjdk.asmtools.common.Environment;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Optional;

/**
 * This class is a generic interface, symbolising any input for jdis/jasm/jdec/jcoder.
 * Asmtools as application internally uses FileInput and StdinInput.
 * UnitTests for asmtools uses mainly StringInput for assemblers  and ByteInput for disassemblers.
 * <p>
 * String/Byte/Stream inputs can be used as any 3rd part code which do not need files, aka IDE, instrumentation or similar.
 * <p>
 * The interface methods goes in favor of asmtools, and for details and help see individual implementations
 */
public interface ToolInput {

    String getName();

    DataInputStream getDataInputStream(Optional<Environment> logger) throws URISyntaxException, IOException;

    Collection<String> readAllLines() throws IOException;

    default boolean isDetailedInput() {
        return false;
    }

    default ToolInput setDetailedInput(boolean value) {
        return this;
    }

    default MessageDigest getMessageDigest() {
        return null;
    }

    default int getSize() {
        return 0;
    }

}
