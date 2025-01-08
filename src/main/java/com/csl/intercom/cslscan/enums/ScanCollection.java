package com.csl.intercom.cslscan.enums;

import lombok.Getter;

@Getter
public enum ScanCollection {
    GENERAL_INFO("general_info"),
    CPE_ITEMS("cpeItem"),
    MICROSOFT_KB("microsoft_kb"),
    ENTITIES("entity"),
    SNMP_WALKS("snmp_walk"),
    ;

    private final String name;

    private ScanCollection(String name) {
        this.name = name;
    }

}
