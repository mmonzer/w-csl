package com.csl.intercom.dbapi.enums;

public enum SNMPv1ConnectionField {
    PORT("port_number", "port"),
    COMMUNITY("snmp_community", "community"),
    ;

    private final String dbapiName;
    private final String scanName;

    private SNMPv1ConnectionField(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }

    public static SNMPv1ConnectionField fromDbapiName(String dbapiName) {
        for (SNMPv1ConnectionField field: SNMPv1ConnectionField.values()) {
            if (field.dbapiName.equals(dbapiName)) {
                return field;
            }
        }
        return null;
    }
}
