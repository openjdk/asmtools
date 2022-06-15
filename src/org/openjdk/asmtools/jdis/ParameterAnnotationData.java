/*
 * Copyright (c) 1996, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 *  4.7.18. The RuntimeVisibleParameterAnnotations Attribute
 *  4.7.19. The RuntimeInvisibleParameterAnnotations Attribute
 */
public class ParameterAnnotationData<T extends MemberData>  extends MemberData{

    private final boolean invisible;
    private ArrayList<ArrayList<AnnotationData<T>>> array = null;

    public ParameterAnnotationData(T data, boolean invisible) {
        super(data);
        this.invisible = invisible;
    }

    public int numParams() {
        return (array == null) ? 0 : array.size();
    }

    public ArrayList<AnnotationData<T>> get(int i) {
        return array.get(i);
    }

    public void read(DataInputStream in) throws IOException {
        int numParams = in.readByte();
        environment.traceln("ParameterAnnotationData[%d]:", numParams);
        array = new ArrayList<>(numParams);
        for (int paramNum = 0; paramNum < numParams; paramNum++) {
            int numAnnotations = in.readShort();
            environment.traceln(" Param#[%d]: numAnnotations= %d", paramNum, numAnnotations);
            if (numAnnotations > 0) {
                // read annotation
                ArrayList<AnnotationData<T>> paramAnnotationList = new ArrayList<>(numAnnotations);
                for (int annotIndex = 0; annotIndex < numAnnotations; annotIndex++) {
                    AnnotationData annotationData = new AnnotationData(data, invisible);
                    annotationData.read(in);
                    paramAnnotationList.add(annotationData);
                }
                array.add(paramNum, paramAnnotationList);
            } else {
                array.add(paramNum, null);
            }
        }
    }
}
