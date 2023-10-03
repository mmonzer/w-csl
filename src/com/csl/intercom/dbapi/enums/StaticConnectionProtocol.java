package com.csl.intercom.dbapi.enums;

public enum StaticConnectionProtocol {
    SNMPv1("SNMPv1", "SNMPV1"),
    SNMPv2c("SNMPv2c", "SNMPV2c"),
    SNMPv3("SNMPv3", "SNMPV3"),
    RemotePowershell("Powershell", "POWERSHELL"),
    HTTP("HTTP", "HTTP"),
    ;

    private String dbapiName;
    private String scanName;

    private StaticConnectionProtocol(String dbapiName, String scanName) {
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
}
