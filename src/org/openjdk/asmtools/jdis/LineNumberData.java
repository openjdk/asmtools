package org.openjdk.asmtools.jdis;

import java.io.DataInputStream;
import java.io.IOException;

import static org.openjdk.asmtools.jasm.JasmTokens.Token.LINETABLE_HEADER;
import static org.openjdk.asmtools.jasm.TableFormatModel.Token.LINE_NUMBERS;

/**
 * Represents a single line number data entry within a Line Number Table attribute.
 * This class extends the Indenter class to provide indentation functionality.
 */
public class LineNumberData extends Indenter {
    /**
     * The starting program counter of this line number entry.
     */
    protected short start_pc;

    /**
     * The line number corresponding to the start_pc.
     */
    protected short line_number;

    /**
     * A format string used for printing the line number data.
     */
    protected String format;

    /**
     * Checks if this LineNumberData instance should be printed.
     *
     * @return true if either printLineTable or tableFormat is enabled, false otherwise.
     */
    @Override
    public boolean isPrintable() {
        return printLineTable;
    }

    /**
     * Prints this LineNumberData instance in JASM format.
     *
     * @param index the index of this entry in the Line Number Table
     * @param size the total number of entries in the Line Number Table
     * @throws IOException if an I/O error occurs during printing
     */
    @Override
    protected void jasmPrint(int index, int size) throws IOException {
        incIndent();
        if (index == 0) {
            printIndentLn(getTitle());
        }
        printIndentLn(format.formatted("line", line_number, start_pc));
    }

    @Override
    protected String getTitle() {
        return LINETABLE_HEADER.parseKey() + ":";
    }

    /**
     * Prints this LineNumberData instance in Table(javap) format.
     *
     * @param index the index of this entry in the Line Number Table
     * @param size the total number of entries in the Line Number Table
     * @throws IOException if an I/O error occurs during printing
     */
    @Override
    protected void tablePrint(int index, int size) throws IOException {
        //There are no differences between the simple (jasm) and extended (table) presentations of LineNumberTable info.
        this.jasmPrint(index, size);
    }

    /**
     * Constructs a new LineNumberData instance from the given DataInputStream and MethodData.
     *
     * @param in the DataInputStream containing the line number data
     * @param methodData the MethodData instance associated with this LineNumberData
     * @throws IOException if an I/O error occurs during construction
     */
    public LineNumberData(DataInputStream in, MethodData methodData) throws IOException {
        start_pc = in.readShort();
        line_number = in.readShort();
        super.toolOutput = methodData.toolOutput;
        int n = methodData.printProgramCounter ? PROGRAM_COUNTER_PLACEHOLDER_LENGTH + 4 : INSTR_PREFIX_LENGTH + 2;
        format = "%" + n + "s %4d:  %7d";
    }
}
