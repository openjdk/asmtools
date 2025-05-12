package org.openjdk.asmtools.jdis;

import java.io.DataInputStream;
import java.io.IOException;

import static org.openjdk.asmtools.jasm.JasmTokens.Token.THROWS;
import static org.openjdk.asmtools.jasm.TableFormatModel.Token.EXCEPTIONS;

/**
 * Exceptions_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u2 number_of_exceptions;
 *     u2 exception_index_table[number_of_exceptions];
 * }
 */
public class ExceptionData extends ClassArrayData {
    private int keywordPadding = -1;

    public ExceptionData(ClassData classData) {
        super(classData, THROWS);
        tableToken = EXCEPTIONS;
    }

    public ExceptionData read(DataInputStream in, int attribute_length) throws IOException, ClassFormatError {
        return (ExceptionData) super.read(in, attribute_length);
    }

    public ExceptionData setKeywordPadding(int keywordPadding) {
        this.keywordPadding = keywordPadding;
        return this;
    }
    @Override
    protected int getPrintAttributeKeyPadding() {
        return keywordPadding == -1  ? super.getPrintAttributeKeyPadding() : keywordPadding;
    }
}
