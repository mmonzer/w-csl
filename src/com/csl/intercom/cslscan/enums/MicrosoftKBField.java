package com.csl.intercom.cslscan.enums;

public enum MicrosoftKBField {
    MONGO_ENTITY_ID("mongoEntityId", "uuid"),
    DEVICE_ID("deviceId", "entityUuid"),
    DISCOVERY_CONNECTION_ID("discoveryConnectionId", "connectionInfoUuid"),
    CREATTION_DATE("creationDate", "createdAt"),
    DISCOVERED_DATE("discoveredDate", "updatedAt"),
    IS_DELETED("isDeleted", "deleted"),
    KB_NUMBER("kbNumber", "number"),
    INSTALLED_DATE("installedDate", "installedDate"),
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
