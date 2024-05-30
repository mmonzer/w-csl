package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.SshConnectionField;
import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.ucsl.json.Json;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

public class SshConnection extends Connection {
    private final int port;
    private final String username;
    @Getter
    private final String password;
    @Getter
    private final String privateKey;
    @Getter
    private final String passphrase;

    protected SshConnection(int id, int port, List<String> devices, String username, String password, String privateKey, String passphrase) {
        super(id, devices, StaticConnectionProtocol.SSH);
        this.port = port;
        this.username = username;
        this.password = password;
        this.privateKey = privateKey;
        this.passphrase = passphrase;
    }

    public static SshConnection fromJson(Json connectionJson) {
        int id = 0;
        if (connectionJson.has("id") && connectionJson.get("id").isNumber()) {
            id = connectionJson.get("id").asInteger();
        }

        int port = 0;
        if (connectionJson.has(SshConnectionField.PORT.dbapiName()) && connectionJson.get(SshConnectionField.PORT.dbapiName()).isNumber()) {
            port = connectionJson.get(SshConnectionField.PORT.dbapiName()).asInteger();
        } else {
            port = 22;
        }

        List<String> devices = null;
        if (connectionJson.has("connected_devices") && connectionJson.get("connected_devices").isArray()){
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

        Json otherData = connectionJson.get("read_only_other_data");

        String privateKey = "";
        if (otherData.has(SshConnectionField.PRIVATE_KEY.dbapiName()) && otherData.get(SshConnectionField.PRIVATE_KEY.dbapiName()).isString()) {
            privateKey = otherData.get(SshConnectionField.PRIVATE_KEY.dbapiName()).asString();
        }

        String passphrase = null;
        if (otherData.has(SshConnectionField.PASSPHRASE.dbapiName()) && otherData.get(SshConnectionField.PASSPHRASE.dbapiName()).isString()) {
            passphrase = otherData.get(SshConnectionField.PASSPHRASE.dbapiName()).asString();
        }

        return new SshConnection(id, port, devices, username, password, privateKey, passphrase);
    }

    @Override
    public Json serializeForScanner() {
        Json connectionJson = super.serializeForScanner();
        connectionJson.set(SshConnectionField.PORT.scanName(), port);
        connectionJson.set(SshConnectionField.USERNAME.scanName(), username);
        connectionJson.set(SshConnectionField.PASSWORD.scanName(), password);
        connectionJson.set(SshConnectionField.PRIVATE_KEY.scanName(), privateKey);
        connectionJson.set(SshConnectionField.PASSPHRASE.scanName(), passphrase);
        return connectionJson;
    }

}
