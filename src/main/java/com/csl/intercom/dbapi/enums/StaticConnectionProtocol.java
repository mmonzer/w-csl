package com.csl.intercom.dbapi.enums;

public enum StaticConnectionProtocol {
    SNMP_V1("SNMPv1", "SNMPV1", "SNMPV1"),
    SNMP_V2C("SNMPv2c", "SNMPV2c", "SNMPV2c"),
    SNMP_V3("SNMPv3", "SNMPV3", "SNMPV3"),
    REMOTE_POWERSHELL("Powershell", "POWERSHELL", "POWERSHELL"),
    HTTP("HTTP", "HTTP", "HTTP"),
    SSH("SSH", "SSH", "SSH"),
    CAMERADEPLOYEDCERTIFICATE("CAMERADEPLOYEDCERTIFICATE", "CAMERADEPLOYEDCERTIFICATE", "CAMERADEPLOYEDCERTIFICATE"),
    ;

    private final String dbapiName;
    private final String scanName;
    private final String autocryptName;

//    StaticConnectionProtocol(String dbapiName, String scanName) {
//        this.dbapiName = dbapiName;
//        this.scanName = scanName;
//    }
//    StaticConnectionProtocol(String dbapiName, String autocryptName) {
//        this.dbapiName = dbapiName;
//        this.autocryptName = autocryptName;
//    }
    StaticConnectionProtocol(String dbapiName, String scanName, String autocryptName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
        this.autocryptName = autocryptName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }
    public String autocryptName() {
        return autocryptName;
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
    public static StaticConnectionProtocol fromAutoCryptName(String autocryptName) {
        for (StaticConnectionProtocol protocol: StaticConnectionProtocol.values()) {
            if (protocol.autocryptName.equals(autocryptName)) {
                return protocol;
            }
        }
        return null;
    }

}
