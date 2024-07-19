package com.csl.intercom.dbapi.enums;

public enum SNMPv3ConnectionField {
    PORT("port_number", "port"),
    USERNAME("username", "user"),
    PASSWORD("read_only_password", "pass"),
    PASSPHRASE("snmp_privacy_key", "privPassPhrase"),
    AUTHENTICATION_ALGORITHM("snmp_authentication_algorithm", "authProtocolName"),
    PRIVACY_ALGORITHM("snmp_privacy_algorithm", "privProtocolName"),
    ;

    private final String dbapiName;
    private final String scanName;

    private SNMPv3ConnectionField(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }
}
