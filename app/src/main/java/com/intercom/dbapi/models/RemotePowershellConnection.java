package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.csl.intercom.dbapi.enums.RemotePowershellConnectionField;
import com.ucsl.json.Json;

import java.util.List;
import java.util.stream.Collectors;

public class RemotePowershellConnection extends Connection {
    private int port;
    private String username;
    private String password;

    protected RemotePowershellConnection(int id, int port, List<String> devices, String username, String password) {
        super(id, devices, StaticConnectionProtocol.RemotePowershell);
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public static RemotePowershellConnection fromJson(Json connectionJson) {
        try {
            int id = connectionJson.get("id").asInteger();
            int port = connectionJson.get(RemotePowershellConnectionField.PORT.dbapiName()).asInteger();
            List<String> devices = connectionJson.get("connected_devices").asJsonList().stream()
                    .map(Json::asString)
                    .collect(Collectors.toList());
            String username = connectionJson.get(RemotePowershellConnectionField.USERNAME.dbapiName()).asString();
            String password = connectionJson.get(RemotePowershellConnectionField.PASSWORD.dbapiName()).asString();

            return new RemotePowershellConnection(id, port, devices, username, password);
        } catch (NullPointerException | UnsupportedOperationException e) {
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

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
