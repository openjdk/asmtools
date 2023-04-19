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

/**
 * FormatError is the generic error thrown by jdis, jdec while parsing a class file.
 */
public class FormatError extends ClassFormatError {

    /**
     * Checks and returns formatted string if id isn't a reference in i18n.properties
     *
     * @param id   either format string or a resource id
     * @param args arguments of the format string
     * @return null id isn't format string otherwise formatted string
     */
    private static String getResourceMsg(String id, Object... args) {
        return id.startsWith("err.") || id.startsWith("warn.") ? null : String.format(id, args);
    }

    public <T extends ToolLogger> FormatError(T logger, String id, Object... args) {
        super(
                logger.getResourceString(id, args) == null
                        ? FormatError.getResourceMsg(id, args) == null ?
                        "(i18n.properties) The message '" + id + "' not found" :
                        FormatError.getResourceMsg(id, args)
                        : logger.getResourceString(id, args));
    }
}
