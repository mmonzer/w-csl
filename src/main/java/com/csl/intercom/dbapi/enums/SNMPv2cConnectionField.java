package com.csl.intercom.dbapi.enums;

public enum SNMPv2cConnectionField {
    PORT("port_number", "port"),
    COMMUNITY("snmp_community", "community"),
    ;

    private final String dbapiName;
    private final String scanName;

    private SNMPv2cConnectionField(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }

    public static SNMPv2cConnectionField fromDbapiName(String dbapiName) {
        for (SNMPv2cConnectionField field: SNMPv2cConnectionField.values()) {
            if (field.dbapiName.equals(dbapiName)) {
                return field;
            }
        }
        return null;
    }
}
