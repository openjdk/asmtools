/*
 * Copyright (c) 2017, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.common.structure;

/*
 * Class File Version
 */
public class CFVersion {
    /**
     * Default versions of class file
     */
    public static final int DEFAULT_MAJOR_VERSION = 45;
    public static final int DEFAULT_MINOR_VERSION = 3;
    public static final int DEFAULT_MODULE_MAJOR_VERSION = 53;
    public static final int DEFAULT_MODULE_MINOR_VERSION = 0;
    public static final int UNDEFINED_VERSION = -1;
    /* The version of a class file since which the compact format of stack map is necessary */
    public static final int SPLIT_VERIFIER_CFV = 50;

    private int major_version;
    private int minor_version;

    private int threshold_major_version;
    private int threshold_minor_version;

    private boolean frozen;

    // Whether is the CVF set as a tool parameter -cv?
    private boolean isSetByParameter;

    public CFVersion() {
        frozen = false;
        isSetByParameter = false;
        major_version = UNDEFINED_VERSION;
        minor_version = UNDEFINED_VERSION;
        threshold_major_version = UNDEFINED_VERSION;
        threshold_minor_version = UNDEFINED_VERSION;
    }

    public CFVersion(int major_version, int minor_version) {
        frozen = false;
        this.major_version = major_version;
        this.minor_version = minor_version;
    }

    public CFVersion setFrozen(boolean frozen) {
        this.frozen = frozen;
        return this;
    }

    public CFVersion setThreshold(int major_version, int minor_version) {
        this.threshold_major_version = major_version;
        this.threshold_minor_version = minor_version;
        return this;
    }

    public CFVersion setVersion(int major_version, int minor_version) {
        this.major_version = major_version;
        this.minor_version = minor_version;
        return this;
    }

    public CFVersion setFileVersion(int major_version, int minor_version) {
        if (isSet() && isFrozen()) {
            if (isThresholdSet()) {
                if ((major_version < threshold_major_version) ||
                        (major_version == threshold_major_version && minor_version < threshold_minor_version)) {
                    return this;
                }
            } else {
                return this;
            }
        }
        this.major_version = major_version;
        this.minor_version = minor_version;
        return this;
    }


    public CFVersion setMajorVersion(int major_version) {
        if (!frozen)
            this.major_version = major_version;
        return this;
    }

    public CFVersion setMinorVersion(int minor_version) {
        if (!frozen) this.minor_version = minor_version;
        return this;
    }

    public CFVersion setByParameter(boolean parameter) {
        isSetByParameter = parameter;
        return this;
    }

    public boolean isSet() {
        return major_version != UNDEFINED_VERSION && minor_version != UNDEFINED_VERSION;
    }

    public boolean isThresholdSet() {
        return threshold_major_version != UNDEFINED_VERSION && threshold_minor_version != UNDEFINED_VERSION;
    }

    public boolean isSetByParameter() {
        return this.isSetByParameter;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public String asString() {
        return String.format("%s:%s", major_version == UNDEFINED_VERSION ? "(undef)" : major_version,
                minor_version == UNDEFINED_VERSION ? "(undef)" : minor_version);
    }

    public String asThresholdString() {
        return String.format("%s:%s-%s:%s",
                threshold_major_version == UNDEFINED_VERSION ? "(undef)" : threshold_major_version,
                threshold_minor_version == UNDEFINED_VERSION ? "(undef)" : threshold_minor_version,
                major_version == UNDEFINED_VERSION ? "(undef)" : major_version,
                minor_version == UNDEFINED_VERSION ? "(undef)" : minor_version
        );
    }

    // A class file whose version number is 50.0 or above (§4.1) must be verified using the type checking rules given
    // in the section 4.10.1. Verification by Type Checking
    public boolean isTypeCheckingVerifier() {
        return isSet() ? major_version >= SPLIT_VERIFIER_CFV : false;
    }

    public CFVersion initModuleDefaultVersion() {
        major_version = (major_version == UNDEFINED_VERSION) ? DEFAULT_MODULE_MAJOR_VERSION : major_version;
        minor_version = (minor_version == UNDEFINED_VERSION) ? DEFAULT_MODULE_MINOR_VERSION : minor_version;
        return this;
    }

    public CFVersion initClassDefaultVersion() {
        major_version = (major_version == UNDEFINED_VERSION) ? DEFAULT_MAJOR_VERSION : major_version;
        minor_version = (minor_version == UNDEFINED_VERSION) ? DEFAULT_MINOR_VERSION : minor_version;
        return this;
    }

    public static CFVersion copyOf(CFVersion cfv) {
        CFVersion cfVersion = new CFVersion();
        cfVersion.major_version = cfv.major_version;
        cfVersion.minor_version = cfv.minor_version;
        cfVersion.threshold_major_version = cfv.threshold_major_version;
        cfVersion.threshold_minor_version = cfv.threshold_minor_version;
        cfVersion.isSetByParameter = cfv.isSetByParameter;
        cfVersion.frozen = cfv.frozen;
        return cfVersion;
    }

    public int minor_version() {
            return this.minor_version;
    }

    public int major_version() {
        return this.major_version;
    }
}
