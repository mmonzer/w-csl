package com.csl.intercom.dbapi.enums;

public enum SNMPv3PrivacyAlgorithm {
    AES("AES", "PrivAES128"),
    AES128("AES-128", "PrivAES128"),
    AES192("AES-192", "PrivAES192"),
    AES256("AES-256", "PrivAES256"),
    DES("DES", "PrivDES"),
    ;
    private final String dbapiName;
    private final String scanName;

    private SNMPv3PrivacyAlgorithm(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }

    public static SNMPv3PrivacyAlgorithm fromDbapiName(String dbapiName) {
        for (SNMPv3PrivacyAlgorithm algorithm: SNMPv3PrivacyAlgorithm.values()) {
            if (algorithm.dbapiName.equals(dbapiName)) {
                return algorithm;
            }
        }
        return null;
    }
}
