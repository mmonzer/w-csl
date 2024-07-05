package com.csl.intercom.cslscan.enums;

public enum EntityHttpConnectionStageField {
    UUID("uuid", "uuid"),
    NAME("stageName", "name"),
    SHOULD_DO_A_REQUEST("shouldDoARequest", "shouldDoARequest"),
    URL("url", "url"),
    IP_ADDRESS("ipAddress", "ipAddress"),
    PORT("port", "port"),
    USERNAME("username", "username"),
    PASSWORD("password", "password"),
    REALM("realm", "realm"),
    TOKEN("token", "token"),
    PROTOCOL("protocol", "protocol"),
    METHOD("method", "method"),
    AUTHENTICATION_METHOD("authenticationMethod", "authenticationMethod"),
    PATH("path", "path"),
    CONTENT_TYPE("contentType", "contentType"),
    HEADERS("headers", "headers"),
    QUERY_PARAMS("queryParams", "queryParams"),
    BODY("body", "body"),
    JS_PARSER("jsParser", "jsParser"),
    ENABLED("isEnabled", "enabled"),
    VISIBLE("isVisible", "visible"),
    ;
    private String dbapiName;
    private String scanName;

    EntityHttpConnectionStageField(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }

    public static EntityHttpConnectionStageField fromDbapiName(String dbapiName) {
        for (EntityHttpConnectionStageField field: EntityHttpConnectionStageField.values()) {
            if (field.dbapiName.equals(dbapiName)) {
                return field;
            }
        }
        return null;
    }

    public static EntityHttpConnectionStageField fromScanName(String scanName) {
        for (EntityHttpConnectionStageField field: EntityHttpConnectionStageField.values()) {
            if (field.scanName.equals(scanName)) {
                return field;
            }
        }
        return null;
    }
}
