package com.csl.intercom.dbapi.enums;

import com.csl.intercom.dbapi.models.HttpConnection;

public enum HttpConnectionField {
    PORT("port_number", "port"),
    ENTITY_HTTP_CONNECTION_ID("entityHttpConnectionId", "entityHttpConnectionId"),
    AUTHENTICATION_METHOD("authMethod", "authMethod"),
    USERNAME("username", "username"),
    PASSWORD("read_only_password", "password"),
    REALM("realm", "realm"),
    TOKEN("token", "token"),
    HEADERS("headers", "headers"),

    QUERY_PARAMS("queryParams", "queryParams"),
    INPUTS("inputs", "inputs"),
    STAGES_CONFIG("stagesConfig", "stagesConfig"),
    ;

    private String dbapiName;
    private String scanName;

    private HttpConnectionField(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }

    public static HttpConnectionField fromDbapiName(String dbapiName) {
        for (HttpConnectionField field : HttpConnectionField.values()) {
            if (field.dbapiName.equals(dbapiName)) {
                return field;
            }
        }
        return null;
    }
}
