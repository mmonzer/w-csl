package com.csl.intercom.dbapi.enums;

import com.csl.intercom.dbapi.models.Connection;

public enum ConnectionProtocol {
    SNMPv1("SNMPv1", "SNMPV1"),
    SNMPv2c("SNMPv2c", "SNMPV2c"),
    SNMPv3("SNMPv3", "SNMPV3"),
    RemotePowershell("Powershell", "POWERSHELL"),
    ;

    private String dbapiName;
    private String scanName;

    private ConnectionProtocol(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }

    public static ConnectionProtocol fromDbapiName(String dbapiName) {
        for (ConnectionProtocol protocol: ConnectionProtocol.values()) {
            if (protocol.dbapiName.equals(dbapiName)) {
                return protocol;
            }
        }
        return null;
    }
}
