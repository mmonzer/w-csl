package com.csl.intercom.cslscan.enums;

public enum ExternalConnectionInfoField {
    ID("uuid", "uuid"),
    NAME("name", "name"),
    CREATED_AT("createdAt", "createdAt"),
    UPDATED_AT("updatedAt", "updatedAt"),
    IS_DELETED("deleted", "deleted"),
    QUERY_PROTOCOL("queryProtocol", "queryProtocol"),
    QUERY_PROTOCOL_ID("queryProtocolId", "queryProtocolId"),
    ;

    private final String dbapiName;
    private final String scanName;

    ExternalConnectionInfoField(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String getDbapiName() {
        return dbapiName;
    }

    public String getScanName() {
        return scanName;
    }

    public static ExternalConnectionInfoField fromDbapiName(String dbapiName) {
        for (ExternalConnectionInfoField field : values()) {
            if (field.dbapiName.equals(dbapiName)) {
                return field;
            }
        }
        return null;
    }

    public static ExternalConnectionInfoField fromScanName(String scanName) {
        for (ExternalConnectionInfoField field : values()) {
            if (field.scanName.equals(scanName)) {
                return field;
            }
        }
        return null;
    }
}
