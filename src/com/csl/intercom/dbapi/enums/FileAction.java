package com.csl.intercom.dbapi.enums;

public enum FileAction {
    IMPORT_HTTP_TEMPLATE_DB_BSON(1),
    ;

    private final int value;
    private FileAction(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static FileAction fromValue(int value) {
        for (FileAction fileAction : FileAction.values()) {
            if (fileAction.getValue() == value) {
                return fileAction;
            }
        }
        return null;
    }
}
