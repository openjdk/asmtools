package org.openjdk.asmtools.jasm;

import org.openjdk.asmtools.common.structure.EAttribute;

import java.io.IOException;

import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType.CONSTANT_METHODREF;

/**
 * EnclosingMethod_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 class_index;
 * u2 method_index;
 * }
 * If the current class is not immediately enclosed by a method or constructor,
 * then the value of the method_index item must be zero.
 */
public class EnclosingMethodAttr extends AttrData {
    private ConstantPool pool;
    private ConstCell classCell;
    // methodCell is null if the current class is not immediately enclosed by a method or constructor.
    private ConstCell methodCell;

    /**
     * @param pool       ConstantPool
     * @param classCell  class_index to be written to class file
     * @param methodCell method_index If the current class is not immediately enclosed by a method or constructor,
     *                   then the value of the method_index item must be zero.
     */
    EnclosingMethodAttr(ConstantPool pool, ConstCell classCell, ConstCell methodCell) {
        super(pool, EAttribute.ATT_EnclosingMethod);
        this.pool = pool;
        this.classCell = classCell;
        this.methodCell = methodCell;
    }

    @Override
    public int attrLength() {
        // attribute_length: The value of the attribute_length item must be four.
        return 4;
    }

    @Override
    protected ConstCell<?> classifyConstCell(ConstantPool pool, ConstCell<?> cell) {
        return pool.findCell(CONSTANT_METHODREF, cell);
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        super.write(out);
        if (!classCell.isSet()) {
            classCell = pool.specifyCell(classCell);
            if (!pool.getBounds().in(classCell.cpIndex)) {
                pool.environment.throwErrorException("err.entity.not.in.cp", classCell);
            }
        }
        out.writeShort(classCell.cpIndex);
        if (methodCell != null && !methodCell.isSet()) {
            methodCell = pool.specifyCell(methodCell);
            if (!pool.getBounds().in(methodCell.cpIndex)) {
                pool.environment.throwErrorException("err.entity.not.in.cp", methodCell);
            }
        }

        // if methodCell is null then method_index is 0
        out.writeShort(methodCell != null ? methodCell.cpIndex : 0);
    }
}
