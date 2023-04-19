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
/**
 * Facilities for behavioral testing of transitions from a binary to jcod/jasm presentation and vice versa.
 * The stdout/stderr should be equal before and after transition as follows:
 * <p>
 * javac class(output1) -> jasm->class(output2)  => output1 == output2
 * javac class(output1) -> jcod->class(output2)  => output1 == output2
 * (expected output):      class(output)         => expected output == output
 * (expected output):      jasm-> class(output)  => expected output == output
 * (expected output):      jcod-> class(output)  => expected output == output
 */
package org.openjdk.asmtools.transform;
