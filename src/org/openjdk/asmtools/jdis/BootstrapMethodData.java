/*
 * Copyright (c) 1996, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jdis;

import org.openjdk.asmtools.jasm.JasmTokens;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 *  BootstrapMethods_attribute {
 *     ...
 *     {   u2 bootstrap_method_ref;
 *         u2 num_bootstrap_arguments;
 *         u2 bootstrap_arguments[num_bootstrap_arguments];
 *     }
 *  }
 */
public class BootstrapMethodData extends Indenter {

    int bsmRef;                              //  u2 bootstrap_method_ref;
    ArrayList<Integer> bsmArguments;         //  u2 bootstrap_arguments[num_bootstrap_arguments];
    int numBsmArgs;                          //  num_bootstrap_arguments


    public BootstrapMethodData(ClassData cls) {
        super(cls.toolOutput);
    }

    @Override
    public boolean isPrintable() {
        return printCPIndex && bsmArguments != null && !bsmArguments.isEmpty();
    }

    // suppress setting comment offset
    @Override
    public Indenter setCommentOffset(int commentOffset) {
        return this;
    }

    /**
     * Read and resolve the bootstrap method data called from ClassData.
     */
    public void read(DataInputStream in) throws IOException {
        // read the Methods CP indexes
        bsmRef = in.readUnsignedShort();
        numBsmArgs = in.readUnsignedShort();
        bsmArguments = new ArrayList<>(numBsmArgs);
        for (int i = 0; i < numBsmArgs; i++) {
            bsmArguments.add(in.readUnsignedShort());
        }
    }

    public void print() throws IOException {
        printIndent(JasmTokens.Token.BOOTSTRAPMETHOD.parseKey() + " #" + bsmRef);
        for (int i = 0; i < numBsmArgs; i++) {
            print(" #" + bsmArguments.get(i));
        }
        println(";");
    }
}
