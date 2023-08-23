package com.csl.intercom.cslscan.enums;

public enum ScanCollection {
    GENERAL_INFO("general_info"),
    CPE_ITEMS("cpeItem"),
    ENTITIES("entity"),
    SNMP_WALKS("snmp_walk"),
    ;

    private String name;

    private ScanCollection(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
