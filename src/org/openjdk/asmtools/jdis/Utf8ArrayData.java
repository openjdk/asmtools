package org.openjdk.asmtools.jdis;

import org.openjdk.asmtools.common.Environment;
import org.openjdk.asmtools.jasm.ConstantPool;
import org.openjdk.asmtools.jasm.DataVector;
import org.openjdk.asmtools.jasm.JasmTokens;

public class Utf8ArrayData extends ClassArrayData {
    protected <M extends MemberData<ClassData>> Utf8ArrayData(M classData, JasmTokens.Token token) {
        super(classData, token);
    }

    public void jasmPrintShort() {
        StringBuilder indexes = new StringBuilder();
        StringBuilder names = new StringBuilder();
        int lastIndex = this.indexes.length - 1;
        String eoNames = (printCPIndex) ? "" : ";";
        for (int i = 0; i <= lastIndex; i++) {
            if (printCPIndex) {
                indexes.append("#").append(this.indexes[i]).append(i == lastIndex ? ";" : ", ");
            }
            names.append(pool.StringValue(this.indexes[i])).append(i == lastIndex ? eoNames : ", ");
        }
        printIndent(PadRight(token.parseKey(), getPrintAttributeKeyPadding()));
        if (printCPIndex) {
            if (skipComments) {
                println(indexes.toString());
            } else {
                print(PadRight(indexes.toString(), getPrintAttributeCommentPadding())).println(" // " + names);
            }
        } else {
            println(names.toString());
        }
    }

    public void jasmPrintLong() {
        String name = token.parseKey();
        String locIndent = " ".repeat(name.length());
        int lastIndex = indexes.length - 1;
        for (int i = 0; i <= lastIndex; i++) {
            if (printCPIndex) {
                if (skipComments) {
                    printIndent(PadRight((i == 0) ? name : locIndent, getPrintAttributeKeyPadding())).
                            print("#%d".formatted(indexes[i])).println(i == lastIndex ? ";" : ",");
                } else {
                    printIndent(PadRight((i == 0) ? name : locIndent, getPrintAttributeKeyPadding())).
                            print(PadRight("#%d%s".formatted(indexes[i], (i == lastIndex) ? ";" : ","), getPrintAttributeCommentPadding())).
                            println(" // %s".formatted(pool.StringValue(indexes[i])));
                }
            } else {
                printIndent(PadRight((i == 0) ? name : locIndent, getPrintAttributeKeyPadding())).
                        print(pool.StringValue(indexes[i])).println(i == lastIndex ? ";" : ",");
            }
        }
    }

}
