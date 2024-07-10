package com.csl.intercom.cslscan.enums;

public enum MicrosoftKBField {
    MONGO_ENTITY_ID("mongo_entity_id", "uuid"),
    DEVICE_ID("device_id", "entityUuid"),
    DISCOVERY_CONNECTION_ID("connection_id", "connectionInfoUuid"),
    CREATION_DATE("created_at", "createdAt"),
    DISCOVERED_DATE("discovered_date", "updatedAt"),
    IS_DELETED("is_deleted", "deleted"),
    KB_NUMBER("kb_number", "number"),
    INSTALLED_DATE("installed_date", "installedDate"),
    ;

    private String dbapiName;
    private String scanName;

    private MicrosoftKBField(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }

    public static MicrosoftKBField fromDbapiName(String dbapiName) {
        for (MicrosoftKBField field : MicrosoftKBField.values()) {
            if (field.dbapiName().equals(dbapiName)) {
                return field;
            }
        }
        return null;
    }

    public static MicrosoftKBField fromScanName(String scanName) {
        for (MicrosoftKBField field : MicrosoftKBField.values()) {
            if (field.scanName().equals(scanName)) {
                return field;
            }
        }
        return null;
    }
}
