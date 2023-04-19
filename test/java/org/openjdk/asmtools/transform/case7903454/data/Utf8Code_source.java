//
// Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//
// This code is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License version 2 only, as
// published by the Free Software Foundation.
//
// This code is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
// version 2 for more details (a copy is included in the LICENSE file that
// accompanied this code).
//
// You should have received a copy of the GNU General Public License version
// 2 along with this work; if not, write to the Free Software Foundation,
// Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
//
// Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
// or visit www.oracle.com if you need additional information or have any
// questions.
//
// Java source to generate Utf8Code.class.jasm & Utf8Code.class.jcod
//
package org.openjdk.asmtools.transform.case7903454.data;

import java.util.stream.Collectors;

public class Utf8Code_source {
    static String ČeštinaAlphabet =
            "A Á B C Č D Ď E É Ě F G H Ch I Í J K L M N Ň O Ó P Q R Ř S Š T Ť U Ú Ů V W X Y Ý Z Ž " +
            "a á b c č d ď e é ě f g h ch i í j k l m n ň o ó p q r ř s š t ť u ú ů v w x y ý z ž";

    static String ქართულიენაAlphabet =
            "Ⴀ Ⴁ Ⴂ Ⴃ Ⴄ Ⴅ Ⴆ Ⴇ Ⴈ Ⴉ Ⴊ Ⴋ Ⴌ Ⴍ Ⴎ Ⴏ Ⴐ Ⴑ Ⴒ Ⴓ Ⴔ Ⴕ Ⴖ Ⴗ Ⴘ Ⴙ Ⴚ Ⴛ Ⴜ Ⴝ Ⴞ Ⴟ Ⴠ Ⴡ Ⴢ Ⴣ Ⴤ Ⴥ " +
            "ა ბ გ დ ე ვ ზ თ ი კ ლ მ ნ ო პ ჟ რ ს ტ უ ფ ქ ღ ყ შ ჩ ც ძ წ ჭ ხ ჯ ჰ ჱ ჲ ჳ ჴ ჵ ჶ ჷ ჸ ჹ ჺ ჻ ჼ ჽ ჾ ჿ";

    static String  ΕλληνικάAlphabet =
            "Α α Β β Γ γ Δ δ Ε ε Ζ ζ Η η Θ θ Ι ι Κ κ Λ λ Μ μ Ν ν Ξ ξ Ο ο Π π Ρ ρ Σ σ/ς Τ τ Υ υ Φ φ Χ χ Ψ ψ Ω ω";

    String line;

    private String نتیجہ = "";

    private String вычислитьБольшиеБуквы() {
        return line.codePoints().filter(cp -> Character.isUpperCase(cp)).mapToObj(cp->Character.toString(cp)).
                collect(Collectors.joining());
    }

    private String вычислитьМаленькиеБуквы() {
        return line.codePoints().filter(cp -> Character.isLowerCase(cp)).mapToObj(cp->Character.toString(cp)).
                collect(Collectors.joining());
    }

    public int calculate() {
        if( نتیجہ.isEmpty() )
        نتیجہ = вычислитьБольшиеБуквы().substring(0, 20) + вычислитьМаленькиеБуквы().substring(90);
        int length = نتیجہ.length();
        System.out.println(length + " " + نتیجہ);
        return length;
    }

    public Utf8Code_source() {
        line = ČeštinaAlphabet.replaceAll(" ", "") +
                ქართულიენაAlphabet.replaceAll(" ", "") +
                ΕλληνικάAlphabet.replaceAll(" ", "");
    }
}
