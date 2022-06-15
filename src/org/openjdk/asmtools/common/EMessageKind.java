package org.openjdk.asmtools.common;

public enum EMessageKind {
    ERROR("err."),
    WARNING("warn."),
    INFO("info.");
    final String prefix;

    EMessageKind(String prefix) {
        this.prefix = prefix;
    }

    public static boolean isFromResourceBundle(String msg) {
        for (EMessageKind kind : values()) {
            if (msg.startsWith(kind.prefix)) {
                return true;
            }
        }
        return false;
    }

    public String longForm() {
        return name().substring(0, 1) + name().substring(1).toLowerCase();
    }

    public String shortForm() {
        return name().length() > 5 ? name().substring(0, 4) : name();
    }
}


