package com.csl.intercom.dbapi.enums;

public enum Module {
    CPE_DISCOVERY(5);

    int id;

    Module(int id) {
        this.id = id;
    }

    public int asInt() {
        return id;
    }
}
