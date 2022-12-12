/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.FormatError;
import org.openjdk.asmtools.jasm.JasmTokens;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.openjdk.asmtools.jdis.ConstantPool.TAG.CONSTANT_CLASS;
import static org.openjdk.asmtools.jdis.ConstantPool.TAG.CONSTANT_MODULE;
import static org.openjdk.asmtools.jdis.Options.PR.CPX;

/**
 * The module attribute data.
 */
public class ModuleData extends MemberData<ClassData>{

    protected final boolean printCPIndex = Options.contains(CPX);
    private ModuleContent moduleContent;

    public ModuleData(ClassData classData) {
        super(classData);
    }

    public String getModuleName() {
        return moduleContent == null ? "N/A" : moduleContent.getModuleName();
    }

    public String getModuleVersion() {
        return moduleContent == null ? null : moduleContent.getModuleVersion();
    }

    public String getModuleHeader(String versionString) {
        StringBuilder sb = new StringBuilder(25);
        if (moduleContent == null) {
            sb.append(JasmTokens.Token.MODULE.parseKey());
            sb.append(' ');
            if (printCPIndex) {
                sb.append(String.format("#?? /* %s */", getModuleName()));
            } else {
                sb.append(getModuleName());
            }
            if (versionString != null && versionString.length() > 0) {
                sb.append(' ').append(versionString);
            }
        } else {
            sb.append(moduleContent.getModuleFlags());
            sb.append(JasmTokens.Token.MODULE.parseKey()).append(' ');
            if (printCPIndex) {
                sb.append(String.format("#%d /* %s%s%s",
                        moduleContent.getModuleCPX(),
                        moduleContent.getModuleName(),
                        moduleContent.getModuleVersion() != null ? "@" + moduleContent.getModuleVersion() + " */" : " */",
                        (versionString != null && versionString.length() > 0) ? " " + versionString : ""));
            } else {
                sb.append(moduleContent.getModuleName());
                if (versionString != null && versionString.length() > 0) {
                    sb.append(' ').append(versionString);
                }
                if (moduleContent.getModuleVersion() != null) {
                    sb.append("// @").append(moduleContent.getModuleVersion());
                }
            }
        }
        return sb.toString();
    }

    /**
     * Reads and resolve the method's attribute data called from ClassData.
     */
    public void read(DataInputStream in) throws FormatError {
        int index, moduleFlags, versionIndex;
        String moduleName, version;
        ModuleContent.Builder builder;
        try {
            // u2 module_name_index;
            index = in.readUnsignedShort();
            moduleName = pool.getModuleName(index);

            // u2 module_flags;
            moduleFlags = in.readUnsignedShort();

            // u2 module_version_index;
            versionIndex = in.readUnsignedShort();
            version = pool.getString(versionIndex, ind -> null);

            builder = new ModuleContent.Builder(index, moduleName, moduleFlags, version);

        } catch (IOException ioe) {
            throw new FormatError(environment.getLogger(), "err.invalid_header");
        }

        try {
            int requires_count = in.readUnsignedShort();
            for (int i = 0; i < requires_count; i++) {
                index = in.readUnsignedShort();
                int requiresFlags = in.readUnsignedShort();
                versionIndex = in.readUnsignedShort();

                moduleName = pool.getModuleName(index);
                version = pool.getString(versionIndex, ind -> null);
                builder.require(index, moduleName, requiresFlags, version);
            }
        } catch (IOException ioe) {
            throw new FormatError(environment.getLogger(), "err.invalid_requires");
        }

        try {
            int exports_count = in.readUnsignedShort();
            if (exports_count > 0) {
                for (int i = 0; i < exports_count; i++) {
                    index = in.readUnsignedShort();
                    String packageName = pool.getPackageName(index);
                    int exportsFlags = in.readUnsignedShort();
                    int exports_to_count = in.readUnsignedShort();
                    if (exports_to_count > 0) {
                        Set<ModuleContent.TargetType> targets = new HashSet<>(exports_to_count);
                        for (int j = 0; j < exports_to_count; j++) {
                            int exports_to_index = in.readUnsignedShort();
                            targets.add(new ModuleContent.TargetType(CONSTANT_MODULE, exports_to_index, pool.getModuleName(exports_to_index)));
                        }
                        builder.exports(index, packageName, exportsFlags, targets);
                    } else {
                        builder.exports(index, packageName, exportsFlags);
                    }
                }
            }
        } catch (IOException ioe) {
            throw new FormatError(environment.getLogger(), "err.invalid_exports");
        }

        try {
            int opens_count = in.readUnsignedShort();
            if (opens_count > 0) {
                for (int i = 0; i < opens_count; i++) {
                    index = in.readUnsignedShort();
                    String packageName = pool.getPackageName(index);
                    int opensFlags = in.readUnsignedShort();
                    int opens_to_count = in.readUnsignedShort();
                    if (opens_to_count > 0) {
                        Set<ModuleContent.TargetType> opens = new HashSet<>(opens_to_count);
                        for (int j = 0; j < opens_to_count; j++) {
                            int opens_to_index = in.readUnsignedShort();
                            opens.add(new ModuleContent.TargetType(CONSTANT_MODULE, opens_to_index, pool.getModuleName(opens_to_index)));
                        }
                        builder.opens(index, packageName, opensFlags, opens);
                    } else {
                        builder.opens(index, packageName, opensFlags);
                    }
                }
            }
        } catch (IOException ioe) {
            throw new FormatError(environment.getLogger(), "err.invalid_opens");
        }

        try {
            int uses_count = in.readUnsignedShort();
            if (uses_count > 0) {
                for (int i = 0; i < uses_count; i++) {
                    index = in.readUnsignedShort();
                    String serviceName = pool.getClassName(index);
                    builder.uses(index, serviceName);
                }
            }
        } catch (IOException ioe) {
            throw new FormatError(environment.getLogger(), "err.invalid_uses");
        }

        try {
            int provides_count = in.readUnsignedShort();
            if (provides_count > 0) {
                for (int i = 0; i < provides_count; i++) {
                    index = in.readUnsignedShort();
                    String serviceName = pool.getClassName(index);
                    int provides_with_count = in.readUnsignedShort();
                    Set<ModuleContent.TargetType> implNames = new HashSet<>(provides_with_count);
                    for (int j = 0; j < provides_with_count; j++) {
                        int provides_with_index = in.readUnsignedShort();
                        implNames.add(new ModuleContent.TargetType(CONSTANT_CLASS, provides_with_index, pool.getClassName(provides_with_index)));
                    }
                    builder.provides(index, serviceName, implNames);
                }
            }
        } catch (IOException ioe) {
            throw new FormatError(environment.getLogger(), "err.invalid_provides");
        }
        moduleContent = builder.build();
    }

    /* Print Module Content */
    public void print() {
        if (moduleContent != null ) {
            String s = moduleContent.toString();
            if (!s.isEmpty()) {
                println(s);
            }
        }
    }
}
