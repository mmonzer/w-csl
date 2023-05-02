package com.csl.intercom.dbapi.enums;

public enum ScanAction {
    STARTED(1),
    ENDED(2);

    private int id;

    ScanAction(int id) {
        this.id = id;
    }

    public int asInt() {
        return id;
    }
}
