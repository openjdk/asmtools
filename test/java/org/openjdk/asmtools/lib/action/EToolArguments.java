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
package org.openjdk.asmtools.lib.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum EToolArguments {
    NONE(new String[0], EAsmTool.UNDEF, 0),
    JDIS(new String[0], EAsmTool.JDIS, 0),
    JDIS_T(new String[]{"-table"}, EAsmTool.JDIS, 1),
    JDIS_G(new String[]{"-g"}, EAsmTool.JDIS, 2),
    JDIS_GG(new String[]{"-gg"}, EAsmTool.JDIS, 3),
    JDIS_G_T(new String[]{"-g", "-table"}, EAsmTool.JDIS, 4),
    JDIS_GG_T_NC(new String[]{"-gg", "-table", "-nc"}, EAsmTool.JDIS, 5),
    JDIS_G_T_LNT_LVT(new String[]{"-g", "-table", "-lnt", "-lvt"}, EAsmTool.JDIS, 6),
    JDIS_GG_NC_LNT_LVT(new String[]{"-gg", "-table", "-lnt", "-lvt", "-nc"}, EAsmTool.JDIS, 7),

    JDEC(new String[0], EAsmTool.JDEC, 1),
    JDEC_G(new String[]{"-g"}, EAsmTool.JDEC, 2),

    JASM(new String[0], EAsmTool.JASM, 0),
    JASM_STRICT(new String[]{"-strict"}, EAsmTool.JASM, 1),
    JASM_NOWARN(new String[]{"-nowarn"}, EAsmTool.JASM, 2);

    final String[] args;
    final EAsmTool tool;
    final int priority;

    EToolArguments(String[] args, EAsmTool tool, int priority) {
        this.args = args;
        this.tool = tool;
        this.priority = priority;
    }

    public String getPostfix() {
        String str = Arrays.stream(args).map(s -> s.replace("-", "")).
                collect(Collectors.joining("."));
        return (!str.isEmpty()) ? "." + str : "";
    }

    public String[] getArgs() {
        return args;
    }

    public static List<EToolArguments> ofTool(EAsmTool eAsmTool) {
        return Arrays.stream(values()).
                filter(v -> v.tool == eAsmTool).
                collect(Collectors.toCollection(ArrayList::new));
    }

    public static EToolArguments getArgumentsByPriority(EAsmTool tool, int priority) {
        return Arrays.stream(values()).filter(v -> v.tool == tool && v.priority == priority).
                findFirst().orElse(NONE);
    }
}
