package com.csl.intercom.dbapi.enums;

public enum RemotePowershellConnectionField {
    PORT("port_number", "port"),
    USERNAME("username", "username"),
    PASSWORD("password", "password"),
    CERTIFICATE("certificate", "certificate"),
    IS_KEEP_PASSWORD("is_keep_password", "isKeepPassword"),
    USE_SSL("use_ssl", "useSSL")
    ;

    private final String dbapiName;
    private final String scanName;

    private RemotePowershellConnectionField(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return this.dbapiName;
    }

    public String scanName() {
        return this.scanName;
    }

    public static RemotePowershellConnectionField fromDbapiName(String dbapiName) {
        for (RemotePowershellConnectionField field: RemotePowershellConnectionField.values()) {
            if (field.dbapiName.equals(dbapiName)) {
                return field;
            }
        }
        return null;
    }
}
