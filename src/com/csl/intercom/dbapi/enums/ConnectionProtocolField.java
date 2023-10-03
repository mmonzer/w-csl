package com.csl.intercom.dbapi.enums;

public enum ConnectionProtocolField {
    ID("id"),
    NAME("name"),
    DESCRIPTION("description"),
    DEFAULT_PORT("default_port"),
    CONNECTION_TEMPLATE_ID("connection_template_id"),
    IS_DYNAMIC("is_dynamic"),
    ;

    private String dbapiName;

    private ConnectionProtocolField(String dbapiName) {
        this.dbapiName = dbapiName;
    }

    public String dbapiName() {
        return dbapiName;
    }
}
