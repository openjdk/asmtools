/*
 * Copyright (c) 1996, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jasm;

import org.openjdk.asmtools.asmutils.Pair;
import org.openjdk.asmtools.asmutils.Range;
import org.openjdk.asmtools.jasm.ClassFileConst.ConstType;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType.*;

/**
 * ConstantPool is the class responsible for maintaining constants for a given class file.
 */
public class ConstantPool implements Iterable<ConstCell<?>> {

    static final int PROCESSED = 2;
    static final int NON_PROCESSED = 0;

    private final ConstValue_UTF8 emptyConstValue = new ConstValue_UTF8("");
    private final ConstCell<?> nullConst = new ConstCell(null);
    private final ConstCell<?> zeroConst = new ConstCell(new ConstValue_Zero());
    // For hashing by value
    private final ArrayList<ConstCell<?>> pool = new ArrayList<>(40);
    public JasmEnvironment environment;
    Hashtable<ConstValue<?>, ConstCell<?>> ConstantPoolHashByValue = new Hashtable<>(40);

    private final CPVisitor indexFixerConstantPool = new CPVisitor() {
        @Override
        public void visitConstValueCell(ConstValue_Cell constValue) {
            handleClassIndex(constValue);
        }

        @Override
        public void visitConstValueRefCell(ConstValue_Pair constValue) {
            handleMemberIndex(constValue);
        }

        @Override
        public void visitMethodHandle(ConstValue_MethodHandle constValue) {
            handleIndexCell((ConstCell) constValue.value);
        }
    }, referenceFixerConstantPool = new CPVisitor() {
        @Override
        public void visitConstValueCell(ConstValue_Cell constValue) {
            handleClassRef(constValue);
        }

        @Override
        public void visitConstValueRefCell(ConstValue_Pair constValue) {
            handleMemberRef(constValue);
        }

        @Override
        public void visitMethodHandle(ConstValue_MethodHandle constValue) {
            handleRefCell((ConstCell) constValue.value);
        }
    };

    /**
     * Main constructor
     *
     * @param environment The error reporting environment
     */
    public ConstantPool(JasmEnvironment environment) {
        this.environment = environment;
        pool.add(zeroConst);
    }

    @Override
    public Iterator<ConstCell<?>> iterator() {
        return pool.iterator();
    }

    protected void handleClassRef(ConstValue_Cell cell) {
        ConstCell refCell = (ConstCell) cell.value;
        if (handleRefCell(refCell)) {
            environment.traceln("FIXED ConstPool[" + refCell.cpIndex + "](" + cell.tag.toString() + ") = " + cell.value);
        }
    }

    protected void handleMemberRef(ConstValue_Pair cv) {
        Pair<ConstCell, ConstCell> pair = (Pair<ConstCell, ConstCell>) cv.value;
        if (handleRefCell(pair.first)) {
            environment.traceln("FIXED Left:ConstPool[" + pair.first.cpIndex + "](" + cv.tag.toString() + ") = " + pair.first.ref);
        }
        if (handleRefCell(pair.second)) {
            environment.traceln("FIXED Right:ConstPool[" + pair.second.cpIndex + "](" + cv.tag.toString() + ") = " + pair.second.ref);
        }
    }

    /**
     * Updates  reference cell if there is cpIndex but a cell is not attached.
     *
     * @param refCell a constant cell
     * @return true if the cell is fixed
     */
    private boolean handleRefCell(ConstCell refCell) {
        if (refCell.ref == null) {
            ConstCell refVal = getConstPollCellByIndex(refCell.cpIndex);
            if (refVal != null) {
                checkAndFixCPRef(refVal);
                refCell.ref = refVal.ref;
                return true;
            }
        }
        return false;
    }

    protected void handleClassIndex(ConstValue_Cell cell) {
        ConstCell refCell = (ConstCell) cell.value;
        if (handleIndexCell(refCell)) {
            environment.traceln("FIXED Index of ConstPool[" + refCell.cpIndex + "](" + cell.tag.toString() + ") = " + cell.value);
        }
    }

    protected void handleMemberIndex(ConstValue_Pair cv) {
        Pair<ConstCell, ConstCell> pair = (Pair<ConstCell, ConstCell>) cv.value;
        if (handleIndexCell(pair.first)) {
            environment.traceln("FIXED Index of Left:ConstPool[" + pair.first.cpIndex + "](" + cv.tag.toString() + ") = " + pair.first.ref);
        }
        if (handleIndexCell(pair.second)) {
            environment.traceln("FIXED Index of Right:ConstPool[" + pair.second.cpIndex + "](" + cv.tag.toString() + ") = " + pair.second.ref);
        }
    }

    /**
     * Updates  reference cell if there is an attached cell but cpIndex is missing.
     *
     * @param refCell a constant cell
     * @return true if the cell is fixed
     */
    private boolean handleIndexCell(ConstCell refCell) {
        if (refCell.ref != null && !refCell.isSet()) {
            Optional<ConstCell<?>> cell = getItemizedCell(refCell);
            if (cell.isPresent()) {
                refCell.cpIndex = cell.get().cpIndex;
                return true;
            }
        }
        return false;
    }

    /*
     * Fix Refs in constant pool.
     *
     * This is used when scanning JASM files produced from JDis with the verbose
     * option (eg. where the constant pool is declared in the jasm itself).  In
     * this scenario, we need two passes - the first pass to scan the entries
     * (which creates constant references with indexes, but no reference values);
     * and the second pass, which links references to existing constants.
     */
    public void fixRefsInPool() {
        // Simply iterating through the pool the method is used to fix CP refs
        // when a constant pool is constructed by refs alone.
        environment.traceln("fixRefsInPool: Fixing CP for %d explicit Constant Entries", pool.size());
        for (ConstCell item : pool) {
            checkAndFixCPRef(item);
        }
    }

    /*
     * Fix Indexes in constant pool.
     */
    public void fixIndexesInPool() {
        // Simply iterate through the pool the method is used to fix CP Indexes
        // when a constant pool is constructed by values alone.
        environment.traceln("fixIndexesInPool: Fixing CP for %d explicit Constant Entries.", pool.size());
        for (ConstCell item : pool) {
            checkAndFixCPIndexes(item);
        }
    }

    protected void checkGlobals() {
        environment.traceln("Checking Globals");
        // This fn will put empty UTF8 string entries on any unset
        // CP entries - before the last CP entry.
        for (int cpx = 1; cpx < pool.size(); cpx++) {
            ConstCell cell = pool.get(cpx);
            if (cell == nullConst) { // gap
                cell = new ConstCell(cpx, emptyConstValue);
                pool.set(cpx, cell);
            }
            if (!cell.isSet()) {
                String name = Integer.toString(cpx);
                environment.error("err.const.undecl", name);
            }
        }
    }

    /*
     *  Helper function for "fixRefsInPool"
     *  Does recursive checking of references, using a locally-defined visitor.
     */
    private void checkAndFixCPRef(ConstCell item) {
        ConstValue cv = item.ref;
        if (cv != null) {
            referenceFixerConstantPool.visit(cv);
        }
    }

    /*
     *  Helper function for "fixIndexesInPool"
     *  Does recursive checking of indexes, using a locally-defined visitor.
     */
    private void checkAndFixCPIndexes(ConstCell item) {
        ConstValue cv = item.ref;
        if (cv != null) {
            indexFixerConstantPool.visit(cv);
        }
    }

    /*
     * Help debug Constant Pools
     */
    public void printPool() {
        int i = 0;
        for (ConstCell item : pool) {
            environment.traceln("^^^^^^^^^^^^^  const #" + i + ": " + item);
            i++;
        }
    }

    public Range<Integer> getBounds() {
        return new Range<>(1, pool.size() - 1);
    }

    public ConstCell getConstPollCellByIndex(int cpIndex) {
        if (cpIndex >= pool.size()) {
            return null;
        }
        return pool.get(cpIndex);
    }

    private void cpool_set(int cpx, ConstCell cell, int sz) {
        environment.traceln("cpool_set1: " + cpx + " " + cell);
        environment.traceln("param_size: " + sz);
        environment.traceln("pool_size : " + pool.size());
        cell.cpIndex = cpx;
        if (cpx + sz >= pool.size()) {
            environment.traceln("calling ensureCapacity( " + (cpx + sz + 1) + " )");
            int low = pool.size();
            int high = cpx + sz;
            for (int i = 0; i < high - low; i++) {
                pool.add(nullConst);
            }
        }
        pool.set(cpx, cell);
        if (sz == 2) {
            pool.set(cpx + 1, new ConstCell(cpx + 1, emptyConstValue));
        }
        environment.traceln("cpool_set2: " + cpx + " " + cell);
    }

    private void delete(int cpx) {
        environment.traceln("delete cell(" + cpx + ")");
        Consumer<ConstCell<?>> op = cell -> {
            if (cell.getFlag() == NON_PROCESSED) {
                if (cell.cpIndex > cpx) {
                    cell.cpIndex--;
                    environment.traceln("\tcell from " + (cell.cpIndex + 1) + " to " + cell);
                }
                cell.setFlag(PROCESSED);
            }
        };
        for (int i = 1; i < pool.size(); i++) {
            if (i != cpx) {
                ConstCell<?> constCell = uncheckedGetCell(i);
                this.traverseConstantCell(constCell, op);
            }
        }
        pool.remove(cpx);
        pool.forEach(cell -> cell.setFlag(NON_PROCESSED));
    }

    private void traverseConstantCell(ConstCell<?> constCell, Consumer<ConstCell<?>> op) {
        if (constCell != null && constCell instanceof ConstCell<?>) {
            if (constCell instanceof ConstCell) {
                if (op != null)
                    op.accept(constCell);
                ConstValue<?> constValue = constCell.ref;
                if (constValue != null) {
                    switch (constValue.tag) {
                        case CONSTANT_CLASS, CONSTANT_STRING, CONSTANT_MODULE, CONSTANT_PACKAGE, CONSTANT_METHODTYPE,
                                CONSTANT_DYNAMIC, CONSTANT_INVOKEDYNAMIC -> {
                            traverseConstantCell((ConstCell<?>) constValue.value, op);
                        }
                        case CONSTANT_METHODHANDLE, CONSTANT_NAMEANDTYPE, CONSTANT_FIELDREF, CONSTANT_METHODREF, CONSTANT_INTERFACEMETHODREF -> {
                            Pair<ConstCell<?>, ConstCell<?>> pair = (Pair<ConstCell<?>, ConstCell<?>>) constValue.value;
                            traverseConstantCell(pair.first, op);
                            traverseConstantCell(pair.second, op);
                        }
                    }
                }
            }
        }
    }

    protected ConstCell uncheckedGetCell(int cpx) { // by index
        return pool.get(cpx);
    }

    public ConstCell getCell(int cpx) { // by index
        ConstCell cell = getConstPollCellByIndex(cpx);
        if (cell != null) {
            return cell;
        }
        cell = new ConstCell(cpx, null);
        return cell;
    }

    public void setCell(int cpx, ConstCell cell) {
        ConstValue value = cell.ref;
        if (value == null) {
            environment.throwErrorException("err.constcell.null.val", cpx);
        }
        int sz = value.size();

        if (cpx == 0) {
            // It is correct to warn about redeclaring constant zero,
            // since this value is never written out to a class file.
            environment.warning("warn.const0.redecl");
        } else {
            if ((getConstPollCellByIndex(cpx) != null) || ((sz == 2) && (getConstPollCellByIndex(cpx + 1) != null))) {
                String name = "#" + cpx;
                environment.error("err.const.redecl", name);
                return;
            }
            if (cell.isSet() && (cell.cpIndex != cpx)) {
                cell = new ConstCell(value);
                environment.traceln("setCell: new ConstCell %s", cell.toString());
            }
        }
        cpool_set(cpx, cell, sz);
    }

    public Optional<ConstCell<?>> getItemizedCell(ConstCell<?> cell) {
        final ConstValue value = cell.ref;
        if (value == null) {
            if (getBounds().in(cell.cpIndex)) {
                return Optional.ofNullable(getConstPollCellByIndex(cell.cpIndex));
            } else if (cell.isSet()) {
                environment.throwErrorException("err.constcell.null.val", cell.cpIndex);
            } else {
                environment.throwErrorException("err.constcell.is.undef");
            }
        }
        return ConstantPoolHashByValue.values().stream().
                filter(v -> v.isSet() &&
                        v.getType() == value.tag &&
                        v.ref.equalsByValue(value)).
                findAny();
    }

    private ConstCell<?> itemizeCell(ConstCell<?> cell) {
        Optional<ConstCell<?>> optionalCell = getItemizedCell(cell);
        if (optionalCell.isPresent()) {
            ConstCell<?> cpCell = optionalCell.get();
            if (cpCell.rank != cell.rank) {
                cpCell.setRank(cell.rank);
            }
            return cpCell;
        } else {
            final int cellSize = cell.ref.size();
            final int cpIndex = findVacantSlot(cellSize);
            cpool_set(cpIndex, cell, cellSize);
            return uncheckedGetCell(cpIndex);
        }
    }

    protected void itemizePool() {
        environment.traceln("itemizePool");
        for (ReferenceRank rank : ReferenceRank.values()) {
            for (ConstCell cell : ConstantPoolHashByValue.values().stream().filter(v -> !v.isSet() && rank.equals(v.rank)).toList()) {
                // find already set ConstCell having cpIndex.isSet && value == value of ConstCell where cpIndex is not set.
                // they should be equal by value i.e. cpIndex should not be taken into account
                itemizeCell(cell);
            }
        }
        ConstCell firstCell = getConstPollCellByIndex(0);
        firstCell.cpIndex = 0;
    }

    protected ConstCell<?> specifyCell(ConstCell<?> cell) {
        environment.traceln("itemizeCell");
        return cell.isSet() ? cell : itemizeCell(cell);
    }

    private int findVacantSlot(int cellSize) {
        int index = 1;
        for (; index < pool.size(); index++) {
            if (pool.get(index) == nullConst && ((cellSize == 1) || pool.get(index + 1) == nullConst)) {
                break;
            }
        }
        return index;
    }

    public <T extends ConstValue> ConstCell<T> findCell(final T ref) {
        if (ref == null) {
            environment.throwErrorException("err.constcell.is.null");
        }
        ConstCell cell = ConstantPoolHashByValue.get(ref);
        if (cell != null) {
            // If we found a cached ConstValue
            ConstValue value = cell.ref;
            if (!value.equals(ref)) {
                environment.throwErrorException("err.values.not.eq", ref.toString(), value.toString());
            }
            environment.traceln(format("ConstantPoolHashByValue.got ('%s') for '%s'", cell, ref));
        } else {
            // If we didn't find a cached ConstValue add it to the cache
            cell = new ConstCell(ref);
            ConstantPoolHashByValue.put(ref, cell);
            environment.traceln("ConstantPoolHashByValue.put ('%s','%s')", ref, cell);
        }
        return cell;
    }

    public ConstCell findIntegerCell(Integer value) {
        return findCell(new ConstValue_Integer(CONSTANT_INTEGER, value));
    }

    public ConstCell findFloatCell(Integer value) {
        return findCell(new ConstValue_Float(value));
    }

    public ConstCell findLongCell(Long value) {
        return findCell(new ConstValue_Long(value));
    }

    public ConstCell findDoubleCell(Long value) {
        return findCell(new ConstValue_Long(value));
    }

    public ConstCell findUTF8Cell(String value) {
        return findCell(new ConstValue_UTF8(value));
    }

    public ConstCell lookupUTF8Cell(Function<String, Boolean> rule) {
        return ConstantPoolHashByValue.entrySet().stream().
                filter(entry -> entry.getKey().tag == CONSTANT_UTF8 && rule.apply((String) (entry.getKey().value))).
                findAny().map(entry -> entry.getValue()).
                orElse(null);
    }

    public ConstCell findClassCell(NameInfo nameInfo) {
        return findCell(CONSTANT_CLASS, nameInfo);
    }

    public ConstCell findClassCell(String name) {
        return findCell(CONSTANT_CLASS, findUTF8Cell(name));
    }

    public ConstCell findModuleCell(NameInfo nameInfo) {
        return findCell(CONSTANT_MODULE, nameInfo);
    }

    public ConstCell findModuleCell(String name) {
        return findCell(CONSTANT_MODULE, findUTF8Cell(name));
    }

    public ConstCell findPackageCell(String name) {
        return findCell(CONSTANT_PACKAGE, findUTF8Cell(name));
    }

    public ConstCell findPackageCell(NameInfo nameInfo) {
        return findCell(CONSTANT_PACKAGE, nameInfo);
    }

    public ConstCell findCell(ConstType tag, ConstCell value) {
        return findCell(new ConstValue_Cell(tag, value));
    }

    public ConstCell findCell(ConstType tag, ConstCell left, ConstCell right) {
        return findCell(new ConstValue_Pair(tag, left, right));
    }

    public ConstCell findCell(ConstType tag, NameInfo nameInfo) {
        if (nameInfo.isEmpty()) {
            // throw exception if empty nameInfo
            environment.throwErrorException("err.constcell.empty.nameInfo", "ConstantPool::FindCell");
        } else if (nameInfo.cpIndex() > 0) {
            // find and check that cpIndex refers to the cell with tag
            ConstCell cell = getConstPollCellByIndex(nameInfo.cpIndex());
            if (cell != null && cell.ref.tag == tag) {
                return cell;
            }
            environment.throwErrorException("err.cpindex.notfound", nameInfo.cpIndex());
        }
        return findCell(tag, findUTF8Cell(nameInfo.name()));
    }

    public void write(CheckedDataOutputStream out) throws IOException {
        // Write the constant pool
        int length = pool.size();
        out.writeShort(length);
        int i;
        environment.traceln("wr.pool:size=" + length);
        for (i = 1; i < length; ) {
            ConstCell cell = pool.get(i);
            ConstValue value = cell.ref;
            if (cell.cpIndex != i) {
                environment.throwErrorException("err.constcell.invarg", Integer.toString(i), cell.cpIndex);
            }
            value.write(out);
            i += value.size();
        }
    }

    public ArrayList<ConstCell<?>> getPoolCellsByType(ClassFileConst.ConstType... types) {
        return pool.stream().filter(c -> c.getType().oneOf(types)).collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<ConstCell<?>> getPoolValuesByRefType(ClassFileConst.ConstType... types) {
        return pool.stream().filter(c -> c.ref != null && c.getType().oneOf(types)).
                collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Removes ClassCell entry from the Constant Pool
     *
     * @param cell the Constant Pool cell ConstCell<ConstValue_Class>
     */
    public void removeClassCell(ConstCell<ConstValue_Class> cell) {
        int indCls, indUtf8 = 0;
        if (cell != null) {
            if (cell.getType() == CONSTANT_CLASS) {
                if (cell.ref != null) {
                    ConstCell<ConstValue_UTF8> utf8Cell = cell.ref.value;
                    if (!isAllowedToBeDelete(utf8Cell))
                        return;
                    indUtf8 = utf8Cell.cpIndex;
                }
                indCls = cell.cpIndex;
                this.delete(indCls);
                this.delete(indCls > indUtf8 ? indUtf8 : indUtf8 - 1);
            } else {
                environment.warning("warn.cannot.delete.class.cell", cell);
            }
        }
    }

    /**
     * @return true if the class name belongs to JDK public API
     */
    private boolean isAllowedToBeDelete(ConstCell<ConstValue_UTF8> utf8Cell) {
        if (utf8Cell.ref.value != null) {
            String className = utf8Cell.ref.value;
            if (className.startsWith("java/") || className.startsWith("javax/"))
                //  className.startsWith("jdk/") || className.startsWith("com/sun/tools/") || className.startsWith("org/w3c"))
                return false;
        }
        return true;
    }

    public enum ReferenceRank {
        LDC(0),  // 0 - highest - ref from ldc
        ANY(1),  // 1 - any ref
        NO(2);   // 2 - no ref
        final int priority;

        ReferenceRank(int priority) {
            this.priority = priority;
        }
    }

    /**
     * CONSTANT_ZERO: Zero Constant Value presents Constant 0.
     */
    static public class ConstValue_Zero extends ConstValue<Void> {

        public ConstValue_Zero() {
            super(CONSTANT_ZERO, (Void) null);
        }

        @Override
        public boolean isSet() {
            return true;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            throw new RuntimeException("Trying to write Constant 0.");
        }
    }

    /**
     * CONSTANT_UTF8(1) is used to represent constant objects of the type: String
     */
    static public class ConstValue_UTF8 extends ConstValue<String> {

        public ConstValue_UTF8(String value) {
            super(CONSTANT_UTF8, value);
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
            out.writeUTF(value);
        }
    }

    /**
     * CONSTANT_Integer(3) structure represents 4-byte numeric (int) constants
     */
    static public class ConstValue_Integer extends ConstValue<Integer> {

        public ConstValue_Integer(ClassFileConst.ConstType tag, Integer value) {
            super(tag, value);
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
            out.writeInt(value);
        }
    }

    /**
     * CONSTANT_Float(4) structure represents 4-byte numeric (float) constants
     */
    static public class ConstValue_Float extends ConstValue<Integer> {

        public ConstValue_Float(Integer value) {
            super(CONSTANT_FLOAT, value);
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
            out.writeInt(value);
        }
    }

    /**
     * The CONSTANT_Long_info(5) represents 8-byte numeric (long) constants
     */
    static public class ConstValue_Long extends ConstValue<Long> {

        public ConstValue_Long(Long value) {
            super(CONSTANT_LONG, value);
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
            out.writeLong(value);
        }
    }

    /**
     * The CONSTANT_Double(6) represents 8-byte numeric (double) constants
     */
    static public class ConstValue_Double extends ConstValue<Long> {
        public ConstValue_Double(Long value) {
            super(CONSTANT_DOUBLE, value);
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
            out.writeLong(value);
        }
    }

    /**
     * CONSTANT_Cell represents CONSTANT_Class(7),   CONSTANT_String(8),   CONSTANT_MethodType(16),
     * CONSTANT_Module(19), CONSTANT_Package(20) constants
     */
    static public class ConstValue_Cell<T extends ConstValue<?>> extends ConstValue<ConstCell<T>> {

        public ConstValue_Cell(ConstType tag, ConstCell<T> constCell) {
            super(tag, constCell);
        }

        @Override
        public String toString() {
            return format("[%s %s]", super.tag.toString(), value.toString());
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
            value.write(out);
        }

        @Override
        public boolean isSet() {
            return super.isSet() && value.isSet() && value.ref.isSet();
        }

        @Override
        public int hashCode() {
            int result = value != null ? value.hashCode() : 0;
            result = 31 * result + tag.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ConstValue_Cell)) return false;
            ConstValue_Cell<?> that = (ConstValue_Cell<?>) obj;
            return (Objects.equals(this.value, that.value) && this.tag == that.tag);
        }
    }

    /**
     * The CONSTANT_Class(7) structure represents constant objects of the type String
     */
    static public class ConstValue_Class extends ConstValue_Cell<ConstValue_UTF8> {
        public ConstValue_Class(ConstCell<ConstValue_UTF8> value) {
            super(CONSTANT_CLASS, value);
        }
    }

    /**
     * The CONSTANT_Module(19) structure represents a module
     */
    static public class ConstValue_Module extends ConstValue_Cell<ConstValue_UTF8> {
        public ConstValue_Module(ConstCell<ConstValue_UTF8> value) {
            super(CONSTANT_MODULE, value);
        }
    }

    /**
     * The CONSTANT_Package(20) structure represents a method type
     */
    static public class ConstValue_Package extends ConstValue_Cell<ConstValue_UTF8> {
        public ConstValue_Package(ConstCell<ConstValue_UTF8> value) {
            super(CONSTANT_PACKAGE, value);
        }
    }

    /**
     * The CONSTANT_String(8) structure represents a class or an interface
     */
    static public class ConstValue_String extends ConstValue_Cell<ConstValue_UTF8> {
        public ConstValue_String(ConstCell<ConstValue_UTF8> value) {
            super(CONSTANT_STRING, value);
        }
    }

    /**
     * The CONSTANT_MethodType(16) structure represents a method type
     */
    static public class ConstValue_MethodType extends ConstValue_Cell<ConstValue_UTF8> {
        public ConstValue_MethodType(ConstCell<ConstValue_UTF8> value) {
            super(CONSTANT_METHODTYPE, value);
        }
    }

    /**
     * ConstValue_Pair represents CONSTANT_NameAndType(12), CONSTANT_Fieldref(9), CONSTANT_Methodref(10), and
     * CONSTANT_InterfaceMethodref(11) structures
     */
    static public class ConstValue_Pair<L extends ConstValue, R extends ConstValue> extends ConstValue<Pair<ConstCell<L>, ConstCell<R>>> {

        public ConstValue_Pair(ConstType tag, Pair<ConstCell<L>, ConstCell<R>> pair) {
            super(tag, pair);
        }

        public ConstValue_Pair(ConstType tag, ConstCell<L> left, ConstCell<R> right) {
            this(tag, new Pair(left, right));
        }

        @Override
        public boolean isSet() {
            return super.isSet() &&
                    value.first.isSet() &
                            value.second.isSet();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ConstValue_Pair)) return false;
            ConstValue_Pair<?, ?> that = (ConstValue_Pair<?, ?>) obj;
            if (this.tag == that.tag) {
                if ((this.value.first).equals(that.value.first) &&
                        (this.value.second).equals(that.value.second)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equalsByValue(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ConstValue_Pair)) return false;
            ConstValue_Pair<?, ?> that = (ConstValue_Pair<?, ?>) obj;
            if (this.tag == that.tag) {
                if ((this.value.first).equalsByValue(that.value.first) &&
                        (this.value.second).equalsByValue(that.value.second)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return format("%s[%s,%s]", super.tag.toString(), value.first.toString(), value.second.toString());
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
            if (tag == CONSTANT_METHODHANDLE) {
                out.writeByte(value.first.cpIndex); // write subtag value
            } else {
                out.writeShort(value.first.cpIndex);
            }
            out.writeShort(value.second.cpIndex);
        }
    }

    /**
     * The CONSTANT_NameAndType(12) structure is used to represent a field or method, without indicating which class or
     * interface type it belongs to
     */
    static public class ConstValue_NameAndType extends ConstValue_Pair<ConstValue_UTF8, ConstValue_UTF8> {
        public ConstValue_NameAndType(ConstCell<ConstValue_UTF8> name, ConstCell<ConstValue_UTF8> descriptor) {
            super(CONSTANT_NAMEANDTYPE, name, descriptor);
        }
    }

    /**
     * The CONSTANT_Methodref(10) structure is used to represent a method
     */
    static public class ConstValue_MethodRef extends ConstValue_Pair<ConstValue_Class, ConstValue_NameAndType> {
        public ConstValue_MethodRef(ConstCell<ConstValue_Class> classCell, ConstCell<ConstValue_NameAndType> nameAndType) {
            super(CONSTANT_METHODREF, classCell, nameAndType);
        }
    }

    /**
     * The CONSTANT_InterfaceMethodref(11) structure is used to represent an interface method
     */
    static public class ConstValue_InterfaceMethodRef extends ConstValue_Pair<ConstValue_Class, ConstValue_NameAndType> {
        public ConstValue_InterfaceMethodRef(ConstCell<ConstValue_Class> interfaceCell, ConstCell<ConstValue_NameAndType> nameAndType) {
            super(CONSTANT_INTERFACEMETHODREF, interfaceCell, nameAndType);
        }
    }

    /**
     * The CONSTANT_Fieldref(9) structure is used to represent a field
     */
    static public class ConstValue_FieldRef extends ConstValue_Pair<ConstValue_Class, ConstValue_NameAndType> {
        public ConstValue_FieldRef(ConstCell<ConstValue_Class> classCell, ConstCell<ConstValue_NameAndType> nameAndType) {
            super(CONSTANT_FIELDREF, classCell, nameAndType);
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public boolean equalsByValue(Object obj) {
            return super.equalsByValue(obj);
        }
    }

    /**
     * The CONSTANT_MethodHandle(15) structure is used to represent a method handle
     * T : ConstValue_MethodRef, ConstValue_InterfaceMethodRef or ConstValue_FieldRef
     */
    static public class ConstValue_MethodHandle<P extends ConstValue_Pair<ConstValue_Class, ConstValue_NameAndType>>
            extends ConstValue<ConstCell<P>> {

        final ClassFileConst.SubTag kind;

        public ConstValue_MethodHandle(ClassFileConst.SubTag kind, ConstCell<P> value) {
            super(CONSTANT_METHODHANDLE, value);
            this.kind = kind;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
            out.writeByte(kind.value());
            value.write(out);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ConstValue_MethodHandle)) return false;
            if (!super.equals(obj)) return false;
            ConstValue_MethodHandle<?> that = (ConstValue_MethodHandle<?>) obj;
            return kind == that.kind;
        }

        @Override
        public boolean equalsByValue(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ConstValue_MethodHandle)) return false;
            if (!super.equalsByValue(obj)) return false;
            ConstValue_MethodHandle<?> that = (ConstValue_MethodHandle<?>) obj;
            return kind == that.kind;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + kind.hashCode();
            return result;
        }
    }

    static public class ConstValue_BootstrapMethod extends ConstValue<ConstCell> {

        private BootstrapMethodData bsmData;

        public ConstValue_BootstrapMethod(ConstType tag, BootstrapMethodData bsmdata, ConstCell value) {
            super(tag, value);
            this.bsmData = bsmdata;
        }

        public BootstrapMethodData bsmData() {
            return bsmData;
        }

        public void setBsmData(BootstrapMethodData bsmData, int methodAttrIndex) {
            this.bsmData = bsmData;
            this.bsmData.cpIndex = methodAttrIndex;
        }

        public void setBsmData(BootstrapMethodData bsmData) {
            this.bsmData = bsmData;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ConstValue_BootstrapMethod)) return false;
            if (!super.equals(obj)) return false;
            ConstValue_BootstrapMethod that = (ConstValue_BootstrapMethod) obj;
            return bsmData.equals(that.bsmData);
        }

        @Override
        public boolean equalsByValue(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ConstValue_BootstrapMethod)) return false;
            if (!super.equalsByValue(obj)) return false;
            ConstValue_BootstrapMethod that = (ConstValue_BootstrapMethod) obj;
            return bsmData.equalsByValue(that.bsmData);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + bsmData.hashCode();
            return result;
        }

        @Override
        public boolean isSet() {
            return super.isSet() && bsmData != null;
        }

        @Override
        public String toString() {
            return super.toString() + "{" + bsmData + "," + value + "}";
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
            out.writeShort(bsmData.cpIndex);
            out.writeShort(value.cpIndex);
        }
    }

    /**
     * The CONSTANT_Dynamic (17) structure is used to represent a dynamically-computed constant, an arbitrary value
     * that is produced by invocation of a bootstrap method in the course of a ldc instruction, among others.
     * The auxiliary type specified by the structure constrains the type of the dynamically-computed constant.
     */
    static public class ConstValue_Dynamic extends ConstValue_BootstrapMethod {
        public ConstValue_Dynamic(BootstrapMethodData bsmData, ConstCell napeCell) {
            super(CONSTANT_DYNAMIC, bsmData, napeCell);
            assert (tag == CONSTANT_DYNAMIC && ConstValue_Dynamic.class.isAssignableFrom(getClass())) ||
                    tag == CONSTANT_INVOKEDYNAMIC && ConstValue_InvokeDynamic.class.isAssignableFrom(getClass());
        }
    }

    /**
     * The CONSTANT_InvokeDynamic_info(18) structure is used to represent a dynamically-computed call site, an instance of
     * java.lang.invoke.CallSite that is produced by invocation of a bootstrap method in the course of an invokedynamic instruction.
     * The auxiliary type specified by the structure constrains the method type of the dynamically-computed call site.
     */
    static public class ConstValue_InvokeDynamic extends ConstValue_BootstrapMethod {
        public ConstValue_InvokeDynamic(BootstrapMethodData bsmData, ConstCell napeCell) {
            super(CONSTANT_INVOKEDYNAMIC, bsmData, napeCell);
        }
    }
}
