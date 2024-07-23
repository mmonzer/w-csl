package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.RemotePowershellConnectionField;
import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.ucsl.json.Json;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class RemotePowershellConnection extends Connection {
    private final int port;
    private final String username;
    private final String password;

    protected RemotePowershellConnection(String id, int port, List<String> devices, String username, String password) {
        super(id, devices, StaticConnectionProtocol.RemotePowershell);
        this.port = port;
        this.username = username;
        this.password = password;
    }
    protected RemotePowershellConnection(String name, String id, int port, List<String> devices, String username, String password) {
        super(name, id, devices, StaticConnectionProtocol.RemotePowershell);
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public static RemotePowershellConnection fromJson(Json connectionJson) {
        try {
            String uuid;
            if (connectionJson.has("uuid")) {
                uuid = connectionJson.get("uuid").asString();
            } else {
                uuid = null;
            }
            int port = connectionJson.get(RemotePowershellConnectionField.PORT.dbapiName()).asInteger();
            List<String> devices = connectionJson.get("connected_devices").asJsonList().stream()
                    .map(Json::asString)
                    .collect(Collectors.toList());
            String username = connectionJson.get(RemotePowershellConnectionField.USERNAME.dbapiName()).asString();
            String password = connectionJson.get(RemotePowershellConnectionField.PASSWORD.dbapiName()).asString();
            String name = connectionJson.get("name").asString();
            return new RemotePowershellConnection(uuid, port, devices, username, password);
        } catch (NullPointerException | UnsupportedOperationException e) {
            return null;
        }
    }

    public static RemotePowershellConnection fromHMIJson(Json connectionJson) {
        try {
            String uuid;
            if (connectionJson.has("uuid")) {
                uuid = connectionJson.get("uuid").asString();
            } else {
                uuid = null;
            }
            int port = connectionJson.get(RemotePowershellConnectionField.PORT.dbapiName()).asInteger();
            List<String> devices;
            if (connectionJson.has("connected_devices") && connectionJson.get("connected_devices").isArray()) {
                devices = connectionJson.get("connected_devices").asJsonList().stream()
                        .map(Json::asString)
                        .collect(Collectors.toList());
            } else {
                devices = null;
            }
            String username = connectionJson.get(RemotePowershellConnectionField.USERNAME.dbapiName()).asString();
            String password = connectionJson.get(RemotePowershellConnectionField.PASSWORD.dbapiName()).asString();
            String name = connectionJson.get("name").asString();
            return new RemotePowershellConnection(name, uuid, port, devices, username, password);
        } catch (NullPointerException | UnsupportedOperationException e) {
            return null;
        }
    }

    public static RemotePowershellConnection fromScannerJson(Json connectionJson) {
        try {
            String uuid = null;
            if (connectionJson.has("uuid")) {
                uuid = connectionJson.get("uuid").asString();
            }
            int port = connectionJson.get(RemotePowershellConnectionField.PORT.scanName()).asInteger();
            String username = connectionJson.get(RemotePowershellConnectionField.USERNAME.scanName()).asString();
            String name = connectionJson.get("name").asString();
            return new RemotePowershellConnection(name, uuid, port, null, username, null);
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public Json serializeForScanner() {
        Json result = super.serializeForScanner();
        result.set(RemotePowershellConnectionField.PORT.scanName(), this.port);
        result.set(RemotePowershellConnectionField.USERNAME.scanName(), this.username);
        result.set(RemotePowershellConnectionField.PASSWORD.scanName(), this.password);
        return result;
    }

}
