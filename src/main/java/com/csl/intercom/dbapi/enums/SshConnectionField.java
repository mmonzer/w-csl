package com.csl.intercom.dbapi.enums;

public enum SshConnectionField {
    PORT("port", "port"),
    USERNAME("username", "username"),
    PASSWORD("password", "password"),
    PRIVATE_KEY("ssh_key", "privateKey"),
    PASSPHRASE("passphrase", "passphrase"),
    IS_KEEP_PASSWORD("is_keep_password", "isKeepPassword"),
    IS_KEEP_SSH_KEY("is_keep_ssh_key", "isKeepSshKey"),
    IS_KEEP_PASSPHRASE("is_keep_passphrase", "isKeepPassphrase")
    ;

    private final String dbapiName;
    private final String scanName;

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
