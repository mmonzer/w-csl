package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.SshConnectionField;
import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.ucsl.json.Json;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

public class SshConnection extends Connection {
    @Getter
    private final int port;
    @Getter
    private final String username;
    @Getter
    private final String password;
    @Getter
    private final String privateKey;
    @Getter
    private final String passphrase;

    private Boolean isKeepPassword;
    private Boolean isKeepSshKey;
    private Boolean isKeepPassPhrase;


    protected SshConnection(String id, int port, List<String> devices, String username, String password, String privateKey, String passphrase) {
        super(id, devices, StaticConnectionProtocol.SSH);
        this.port = port;
        this.username = username;
        this.password = password;
        this.privateKey = privateKey;
        this.passphrase = passphrase;
    }

    protected SshConnection(String name, String id, int port, List<String> devices, String username, String password, String privateKey, String passphrase) {
        super(name, id, devices, StaticConnectionProtocol.SSH);
        this.port = port;
        this.username = username;
        this.password = password;
        this.privateKey = privateKey;
        this.passphrase = passphrase;
    }
    protected SshConnection(String name, String id, int port, List<String> devices, String username, String password, String privateKey, String passphrase, Boolean isKeepPassword, Boolean isKeepSshKey, Boolean isKeepPassPhrase) {
        super(name, id, devices, StaticConnectionProtocol.SSH);
        this.port = port;
        this.username = username;
        this.password = password;
        this.privateKey = privateKey;
        this.passphrase = passphrase;
        this.isKeepPassword = isKeepPassword;
        this.isKeepSshKey = isKeepSshKey;
        this.isKeepPassPhrase = isKeepPassPhrase;
    }

    public static SshConnection fromJson(Json connectionJson) {
        try {
            String uuid = null;
            if (connectionJson.has("uuid") && connectionJson.get("uuid").isNumber()) {
                uuid = String.valueOf(connectionJson.get("uuid").asInteger());
            } else {
                if(connectionJson.has("mongo_entity_id"))
                    uuid = connectionJson.get("mongo_entity_id").asString();
            }
            int port = 0;
            if (connectionJson.has(SshConnectionField.PORT.dbapiName()) && connectionJson.get(SshConnectionField.PORT.dbapiName()).isNumber()) {
                port = connectionJson.get(SshConnectionField.PORT.dbapiName()).asInteger();
            } else {
                port = 22;
            }

            List<String> devices = null;
            if (connectionJson.has("connected_devices") && connectionJson.get("connected_devices").isArray()) {
                devices = connectionJson.get("connected_devices").asJsonList().stream()
                        .map(Json::asString)
                        .collect(Collectors.toList());
            }

            String username = null;
            if (connectionJson.has(SshConnectionField.USERNAME.dbapiName()) && connectionJson.get(SshConnectionField.USERNAME.dbapiName()).isString()) {
                username = connectionJson.get(SshConnectionField.USERNAME.dbapiName()).asString();
            }

            String password = null;
            if (connectionJson.has(SshConnectionField.PASSWORD.dbapiName()) && connectionJson.get(SshConnectionField.PASSWORD.dbapiName()).isString()) {
                password = connectionJson.get(SshConnectionField.PASSWORD.dbapiName()).asString();
            }
            Json otherData = connectionJson.get("other_data");
            Json readOnlyOtherData = connectionJson.get("read_only_other_data");

            String privateKey = "";
            if (otherData != null && otherData.has(SshConnectionField.PRIVATE_KEY.dbapiName()) && otherData.get(SshConnectionField.PRIVATE_KEY.dbapiName()).isString()) {
                privateKey = otherData.get(SshConnectionField.PRIVATE_KEY.dbapiName()).asString();
            } else if (readOnlyOtherData != null && readOnlyOtherData.has(SshConnectionField.PRIVATE_KEY.dbapiName()) && readOnlyOtherData.get(SshConnectionField.PRIVATE_KEY.dbapiName()).isString()) {
                privateKey = readOnlyOtherData.get(SshConnectionField.PRIVATE_KEY.dbapiName()).asString();
            }

            String passphrase = null;
            if (otherData != null && otherData.has(SshConnectionField.PASSPHRASE.dbapiName()) && otherData.get(SshConnectionField.PASSPHRASE.dbapiName()).isString()) {
                passphrase = otherData.get(SshConnectionField.PASSPHRASE.dbapiName()).asString();
            } else if (readOnlyOtherData != null && readOnlyOtherData.has(SshConnectionField.PASSPHRASE.dbapiName()) && readOnlyOtherData.get(SshConnectionField.PASSPHRASE.dbapiName()).isString()) {
                passphrase = readOnlyOtherData.get(SshConnectionField.PASSPHRASE.dbapiName()).asString();
            }
            String name = connectionJson.get("name").asString();
            boolean isKeepPassword = false;
            boolean isKeepSshKey = false;
            boolean isKeepPassPhrase = false;
            // check if is_keep_password is present in the json
            if (connectionJson.has(SshConnectionField.IS_KEEP_PASSWORD.dbapiName()) && connectionJson.get(SshConnectionField.IS_KEEP_PASSWORD.dbapiName()).isBoolean()) {
                isKeepPassword = connectionJson.get(SshConnectionField.IS_KEEP_PASSWORD.dbapiName()).asBoolean();
            }
            // check if is_keep_ssh_key is present in the json
            if (connectionJson.has(SshConnectionField.IS_KEEP_SSH_KEY.dbapiName()) && connectionJson.get(SshConnectionField.IS_KEEP_SSH_KEY.dbapiName()).isBoolean()) {
                isKeepSshKey = connectionJson.get(SshConnectionField.IS_KEEP_SSH_KEY.dbapiName()).asBoolean();
            }
            // check if is_keep_passphrase is present in the json
            if (connectionJson.has(SshConnectionField.IS_KEEP_PASSPHRASE.dbapiName()) && connectionJson.get(SshConnectionField.IS_KEEP_PASSPHRASE.dbapiName()).isBoolean()) {
                isKeepPassPhrase = connectionJson.get(SshConnectionField.IS_KEEP_PASSPHRASE.dbapiName()).asBoolean();
            }

            return new SshConnection(name, uuid, port, devices, username, password, privateKey, passphrase, isKeepPassword, isKeepSshKey, isKeepPassPhrase);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public static SshConnection fromScannerJson(Json connectionJson) {
        String uuid = null;
        if (connectionJson.has("uuid") && connectionJson.get("uuid").isString()) {
            uuid = connectionJson.get("uuid").asString();
        }

        int port = 0;
        if (connectionJson.has(SshConnectionField.PORT.scanName()) && connectionJson.get(SshConnectionField.PORT.scanName()).isNumber()) {
            port = connectionJson.get(SshConnectionField.PORT.scanName()).asInteger();
        } else {
            port = 22;
        }

        String username = "";
        if (connectionJson.has(SshConnectionField.USERNAME.scanName()) && connectionJson.get(SshConnectionField.USERNAME.scanName()).isString()) {
            username = connectionJson.get(SshConnectionField.USERNAME.scanName()).asString();
        }

        String password = "";
        if (connectionJson.has(SshConnectionField.PASSWORD.scanName()) && connectionJson.get(SshConnectionField.PASSWORD.scanName()).isString()) {
            password = connectionJson.get(SshConnectionField.PASSWORD.scanName()).asString();
        }

        String privateKey = "";
        if (connectionJson.has(SshConnectionField.PRIVATE_KEY.scanName()) && connectionJson.get(SshConnectionField.PRIVATE_KEY.scanName()).isString()) {
            privateKey = connectionJson.get(SshConnectionField.PRIVATE_KEY.scanName()).asString();
        }

        String passphrase = "";
        if (connectionJson.has(SshConnectionField.PASSPHRASE.scanName()) && connectionJson.get(SshConnectionField.PASSPHRASE.scanName()).isString()) {
            passphrase = connectionJson.get(SshConnectionField.PASSPHRASE.scanName()).asString();
        }
        String name = connectionJson.get("name").asString();

        return new SshConnection(name, uuid, port, null, username, password, privateKey, passphrase);
    }

    @Override
    public Json serializeForScanner() {
        Json connectionJson = super.serializeForScanner();
        connectionJson.set(SshConnectionField.PORT.scanName(), port);
        connectionJson.set(SshConnectionField.USERNAME.scanName(), username);
        connectionJson.set(SshConnectionField.PASSWORD.scanName(), password);
        connectionJson.set(SshConnectionField.PRIVATE_KEY.scanName(), privateKey);
        connectionJson.set(SshConnectionField.PASSPHRASE.scanName(), passphrase);
        connectionJson.set(SshConnectionField.IS_KEEP_PASSWORD.scanName(), isKeepPassword);
        connectionJson.set(SshConnectionField.IS_KEEP_SSH_KEY.scanName(), isKeepSshKey);
        connectionJson.set(SshConnectionField.IS_KEEP_PASSPHRASE.scanName(), isKeepPassPhrase);
        return connectionJson;
    }

}
