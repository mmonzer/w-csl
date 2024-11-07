package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.RemotePowershellConnectionField;
import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.ucsl.json.Json;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

import static com.ucsl.json.JsonUtil.*;

@Getter
public class RemotePowershellConnection extends Connection {
    public static final String MONGO_ENTITY_ID = "mongo_entity_id";
    public static final String CONNECTED_DEVICES = "connected_devices";
    public static final String NAME = "name";
    public static final String UUID = "uuid";
    private final int port;
    private final String username;
    private final String password;
    private Boolean isKeepPassword;
    private String certificate;

    protected RemotePowershellConnection(String name, String id, int port, List<String> devices, String username, String password, Boolean isKeepPassword, String certificate) {
        super(name, id, devices, StaticConnectionProtocol.RemotePowershell);
        this.port = port;
        this.username = username;
        this.password = password;
        this.isKeepPassword = isKeepPassword;
        this.certificate = certificate;
    }

    protected RemotePowershellConnection(String name, String id, Integer port, List<String> devices, String username, String password, String certificate) {
        this(name, id, port, devices, username, password, true, certificate);
    }

    protected RemotePowershellConnection(String name, String id, Integer port, List<String> devices, String username, String password) {
        this(name, id, port, devices, username, password, true, "");
    }

    protected RemotePowershellConnection(String id, Integer port, List<String> devices, String username, String password) {
        this(null, id, port, devices, username, password);
    }

    public static RemotePowershellConnection fromJson(Json connectionJson) {
        try {
            String uuid;
            if (connectionJson.has(UUID)) {
                uuid = connectionJson.get(UUID).asString();
            } else {
                uuid = getValueStringOrNull(connectionJson, MONGO_ENTITY_ID );
            }
            Integer port = connectionJson.get(RemotePowershellConnectionField.PORT.dbapiName()).asInteger();
            List<String> devices = connectionJson.get(CONNECTED_DEVICES).asJsonList().stream()
                    .map(Json::asString)
                    .toList();
            String username = getValueStringOrNull(connectionJson, RemotePowershellConnectionField.USERNAME.dbapiName());
            String password = getValueStringOrNull(connectionJson, RemotePowershellConnectionField.PASSWORD.dbapiName());
            String certificate = getValueStringOrNull(connectionJson, RemotePowershellConnectionField.CERTIFICATE.dbapiName());
            String name = getValueStringOrNull(connectionJson, NAME);
            return new RemotePowershellConnection(name, uuid, port, devices, username, password, certificate);
        } catch (NullPointerException | UnsupportedOperationException e) {
            return null;
        }
    }

    public static RemotePowershellConnection fromHMIJson(Json connectionJson) {
        try {
            String uuid;
            if (connectionJson.has(UUID)) {
                uuid = connectionJson.get(UUID).asString();
            } else {
                uuid = getValueStringOrNull(connectionJson, MONGO_ENTITY_ID );
            }
            Integer port = connectionJson.get(RemotePowershellConnectionField.PORT.dbapiName()).asInteger();
            List<String> devices;
            if (connectionJson.has(CONNECTED_DEVICES) && connectionJson.get(CONNECTED_DEVICES).isArray()) {
                devices = connectionJson.get(CONNECTED_DEVICES).asJsonList().stream()
                        .map(Json::asString)
                        .toList();
            } else {
                devices = null;
            }
            String name = getValueStringOrNull(connectionJson, NAME);
            String username = getValueStringOrNull(connectionJson, RemotePowershellConnectionField.USERNAME.dbapiName());
            String certificate = getValueStringOrNull(connectionJson, RemotePowershellConnectionField.CERTIFICATE.dbapiName());
            // check if password is present in the json
            if (connectionJson.has(RemotePowershellConnectionField.PASSWORD.dbapiName())) {
                String password = getValueStringOrNull(connectionJson, RemotePowershellConnectionField.PASSWORD.dbapiName());
                return new RemotePowershellConnection(name, uuid, port, devices, username, password, certificate);
            } else {
                return new RemotePowershellConnection(name, uuid, port, devices, username, null, true, certificate);
            }
        } catch (NullPointerException | UnsupportedOperationException e) {
            return null;
        }
    }

    public static RemotePowershellConnection fromScannerJson(Json connectionJson) {
        try {
            String uuid = getValueStringOrNull(connectionJson, UUID);
            Integer port = connectionJson.get(RemotePowershellConnectionField.PORT.dbapiName()).asInteger();
            String name = getValueStringOrNull(connectionJson, NAME);
            String username = getValueStringOrNull(connectionJson, RemotePowershellConnectionField.USERNAME.dbapiName());
            String certificate = getValueStringOrNull(connectionJson, RemotePowershellConnectionField.CERTIFICATE.dbapiName());
            return new RemotePowershellConnection(name, uuid, port, null, username, null, certificate);
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
        result.set(RemotePowershellConnectionField.IS_KEEP_PASSWORD.scanName(), this.isKeepPassword);
        result.set(RemotePowershellConnectionField.CERTIFICATE.scanName(), this.certificate);
        return result;
    }

}
