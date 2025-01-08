package com.csl.intercom.dbapi.enums;

public enum StaticConnectionProtocol {
    SNMP_V1("SNMPv1", "SNMPV1"),
    SNMP_V2C("SNMPv2c", "SNMPV2c"),
    SNMP_V3("SNMPv3", "SNMPV3"),
    REMOTE_POWERSHELL("Powershell", "POWERSHELL"),
    HTTP("HTTP", "HTTP"),
    SSH("SSH", "SSH"),
    ;

    private final String dbapiName;
    private final String scanName;

    StaticConnectionProtocol(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }

    public static StaticConnectionProtocol fromDbapiName(String dbapiName) {
        for (StaticConnectionProtocol protocol: StaticConnectionProtocol.values()) {
            if (protocol.dbapiName.equals(dbapiName)) {
                return protocol;
            }
        }
        return null;
    }
    public static StaticConnectionProtocol fromScanName(String scanName) {
        for (StaticConnectionProtocol protocol: StaticConnectionProtocol.values()) {
            if (protocol.scanName.equals(scanName)) {
                return protocol;
            }
        }
        return null;
    }

}
