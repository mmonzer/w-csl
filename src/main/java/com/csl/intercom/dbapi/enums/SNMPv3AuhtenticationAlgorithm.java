package com.csl.intercom.dbapi.enums;

public enum SNMPv3AuhtenticationAlgorithm {
    SHA224("SHA-224", "AuthHMAC128SHA224"),
    SHA256("SHA-256", "AuthHMAC192SHA256"),
    SHA384("SHA-384", "AuthHMAC256SHA384"),
    SHA512("SHA-512", "AuthHMAC384SHA512"),
    SHA("SHA", "AuthSHA"),
    SHA2("SHA2", "AuthSHA2"),
    MD5("MD5", "AuthMD5"),
    ;

    private final String dbapiName;
    private final String scanName;

    private SNMPv3AuhtenticationAlgorithm(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }

    public static SNMPv3AuhtenticationAlgorithm fromDbapiName(String dbapiName) {
        for (SNMPv3AuhtenticationAlgorithm algorithm: SNMPv3AuhtenticationAlgorithm.values()) {
            if (algorithm.dbapiName.equals(dbapiName)) {
                return algorithm;
            }
        }
        return null;
    }
    public static SNMPv3AuhtenticationAlgorithm fromScanName(String scanName) {
        for (SNMPv3AuhtenticationAlgorithm algorithm: SNMPv3AuhtenticationAlgorithm.values()) {
            if (algorithm.scanName.equals(scanName)) {
                return algorithm;
            }
        }
        return null;
    }

}
