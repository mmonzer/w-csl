package com.csl.intercom.dbapi.enums;

public enum RemotePowershellConnectionField {
    PORT("port", "port"),
    USERNAME("username", "username"),
    PASSWORD("password", "password"),
    ;

    private String dbapiName;
    private String scanName;

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
