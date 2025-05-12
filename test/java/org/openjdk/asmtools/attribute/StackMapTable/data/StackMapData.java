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
package org.openjdk.asmtools.attribute.StackMapTable.data;

import static java.lang.Math.round;

public class StackMapData {
    static final int fldI = 0;
    static long fldL = 1;

    protected String fldS = "string";
    public final String fldFS = "string";

    public void generateFrames(int y) {
        int x = 0;
        Object obj = null;

        try {
            x = 1 / x;
        } catch (ArithmeticException e) {
            obj = new Object();
        } finally {
            obj.toString();
        }

        if (x == 0) {
            obj = null;
        }

        switch (x) {
            case 1:
                obj = new Object();
                break;
            case 2:
                break;
        }

        if (y == 0) {
            System.out.println("frame");
        }

        if (y == 1) {
            int temp = 42;
            System.out.println("item_frame" + (int) (temp * round(100)));
        }

        if (y == 2) {
            int a = 0;
            for (int i = 0; i < 3000; i++) {
                float temp = 3.14f;
                a += (int) (temp * round(100));
            }
            System.out.println("item_frame_" + a);
        }
        int d = round(10);
        if (y == 3) {
            int a = round(110f);
            long b = round(100d);
            System.out.println("_frame: " + (a + b) * d);
        }

        if (y == 4) {
            int c = 50 * d;
            double dd = 99.99 * d;
            System.out.println("Before chop_frame: " + (c + dd));
        } else {
            System.out.println("chop_frame");
        }

        if (y == 5) {
            int n = 0;
            for (int i = 0; i < 2500; i++) {
                float temp = 3.14f;
                n += (int) (temp * round(i));
            }
            if (n > 100) {
                System.out.println("_frame_");
            } else if (n < 50) {
                int c = 50 * d;
                double dd = 99.99 * d;
                System.out.println(c + dd);
            }
        }

        if (y == 6) {
            String str = "n_frame";
            char ch = 'F';
            int num = 100;
            System.out.println(str + ": " + ch + num);
        }

        int z = 0;
        Object obj1 = null;

        try {
            z = 1 / z;
        } catch (ArithmeticException e) {
            obj1 = new Object();
        } finally {
            obj1.toString();
        }

        if (z == 0) {
            obj1 = null;
        }

        switch (x) {
            case 1:
                obj1 = new Object();
                break;
            case 2:
                break;
        }
    }

    public int complexMethod(int x, Object o) {
        int a = 0;
        int b = 1;
        Object c = null;

        try {
            if (x > 0) {
                a = x + 1;
                if (o instanceof String) {
                    c = (String) o;
                    b = c.hashCode();
                    return b; // Stack has one item (return value)
                }
                b = a * 2;
            } else if (x == -1) {
                a = -x;
                b = a + 3;
                throw new RuntimeException("Test exception"); // Jumps to catch
            } else {
                for (int i = 0; i < 2; i++) {
                    a += i;
                    if (a > 1) {
                        b = a - i;
                        break; // Early exit from loop
                    }
                }
            }
        } catch (RuntimeException e) {
            c = e; // New local variable (exception)
            b = a + 5;
        }

        return a + b; // Final return
    }

    public int generateFullFrame(int x) {
        int result;
        Object temp;
        boolean flag;

        try {
            if (x > 0) {
                // Path 1: Push something onto the stack and set locals
                temp = "positive";
                flag = true;
                result = temp.hashCode(); // Pushes an int onto the stack
            } else {
                // Path 2: Different stack state and locals
                result = x * 2; // Simple computation, stack briefly has an int
                throw new RuntimeException("non-positive");
            }
        } catch (RuntimeException e) {
            // Path 3: Exception path with different locals
            temp = e;
            flag = false;
            result = -x;
        }
        // Merge point after try-catch
        if (flag) {
            return result + 1; // Stack has one item from Path 1 or 3
        }
        // Simulate a different stack state by adding a method call
        System.out.println("Computing result");
        return result; // Stack is empty after println
    }

    public static void main(String[] args) {
        StackMapData data = new StackMapData();
        for (int i = 0; i <= 6; i++) {
            data.generateFrames(i);
        }
        data.complexMethod(fldI, "test");
        data.generateFullFrame(fldI + 23);
    }
}
