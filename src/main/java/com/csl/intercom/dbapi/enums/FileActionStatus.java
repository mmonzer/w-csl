package com.csl.intercom.dbapi.enums;

public enum FileActionStatus {
    FILE_UPLOADED(0),
    FILE_PROCESSING(1),
    SUCCEEDED(2),
    FAILED(3),
    ;
    private final int value;

    private FileActionStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static FileActionStatus fromValue(int value) {
        for (FileActionStatus fileActionStatus : FileActionStatus.values()) {
            if (fileActionStatus.getValue() == value) {
                return fileActionStatus;
            }
        }
        return null;
    }
}
