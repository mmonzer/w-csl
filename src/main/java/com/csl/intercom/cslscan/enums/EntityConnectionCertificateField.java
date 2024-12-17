package com.csl.intercom.cslscan.enums;

public enum EntityConnectionCertificateField {
    UUID("uuid", "uuid"),
    CONTENT("content", "content"),
    FILENAME("filename", "certificateFileName"),
    CREATED_AT("created_at", "created_at"),
    UPDATED_AT("updated_at", "updated_at");

    private final String dbapiName;
    private final String scanName;

    EntityConnectionCertificateField(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }

    public static EntityConnectionCertificateField fromDbapiName(String dbapiName) {
        for (EntityConnectionCertificateField field: EntityConnectionCertificateField.values()) {
            if (field.dbapiName.equals(dbapiName)) {
                return field;
            }
        }
        return null;
    }

    public static EntityConnectionCertificateField fromScanName(String scanName) {
        for (EntityConnectionCertificateField field: EntityConnectionCertificateField.values()) {
            if (field.scanName.equals(scanName)) {
                return field;
            }
        }
        return null;
    }
}
