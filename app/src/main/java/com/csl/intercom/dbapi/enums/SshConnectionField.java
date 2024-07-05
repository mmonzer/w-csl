package com.csl.intercom.dbapi.enums;

public enum SshConnectionField {
    PORT("port", "port"),
    USERNAME("username", "username"),
    PASSWORD("read_only_password", "password"),
    PRIVATE_KEY("ssh_key", "privateKey"),
    PASSPHRASE("passphrase", "passphrase"),
    ;

    private String dbapiName;
    private String scanName;

    SshConnectionField(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }

    public static SshConnectionField fromDbapiName(String dbapiName) {
        for (SshConnectionField field : values()) {
            if (field.dbapiName.equals(dbapiName)) {
                return field;
            }
        }
        return null;
    }
}
