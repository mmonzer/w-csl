package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.RemotePowershellConnectionField;
import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.ucsl.json.Json;
import lombok.Getter;

import java.util.List;

import static com.ucsl.json.JsonUtil.*;

@Getter
public class RemotePowershellConnection extends Connection {
    public static final String MONGO_ENTITY_ID = "mongo_entity_id";
    public static final String CONNECTED_DEVICES = "connected_devices";
    public static final String NAME = "name";
    public static final String USE_SSL = "use_ssl";
    public static final String UUID = "uuid";
    private final int port;
    private final String username;
    private final String password;
    private Boolean isKeepPassword;
    private boolean useSSL = false;
    private String certificate;

    protected RemotePowershellConnection(String name, String id, int port, List<String> devices, String username, String password, Boolean isKeepPassword, String certificate, boolean useSSL) {
        super(name, id, devices, StaticConnectionProtocol.REMOTE_POWERSHELL);
        this.port = port;
        this.username = username;
        this.password = password;
        this.isKeepPassword = isKeepPassword;
        this.certificate = certificate;
        this.useSSL = useSSL;
    }

    protected RemotePowershellConnection(String name, String id, int port, List<String> devices, String username, String password, Boolean isKeepPassword, String certificate) {
        this(name, id, port, devices, username, password, isKeepPassword, certificate, true);
    }

    protected RemotePowershellConnection(String name, String id, Integer port, List<String> devices, String username, String password, String certificate) {
        this(name, id, port, devices, username, password, true, certificate);
    }

    protected RemotePowershellConnection(String name, String id, Integer port, List<String> devices, String username, String password, String certificate, boolean useSSL) {
        this(name, id, port, devices, username, password, true, certificate, useSSL);
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
            boolean useSSL = getValueBooleanOrDefault(connectionJson, RemotePowershellConnectionField.USE_SSL.dbapiName(), false);
            return new RemotePowershellConnection(name, uuid, port, devices, username, password, certificate, useSSL);
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
            boolean useSSL = getValueBooleanOrDefault(connectionJson, RemotePowershellConnectionField.USE_SSL.dbapiName(), false);
            // check if password is present in the json
            if (connectionJson.has(RemotePowershellConnectionField.PASSWORD.dbapiName())) {
                String password = getValueStringOrNull(connectionJson, RemotePowershellConnectionField.PASSWORD.dbapiName());
                return new RemotePowershellConnection(name, uuid, port, devices, username, password, certificate, useSSL);
            } else {
                return new RemotePowershellConnection(name, uuid, port, devices, username, null, true, certificate, useSSL);
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
            boolean useSSL = getValueBooleanOrDefault(connectionJson, RemotePowershellConnectionField.USE_SSL.dbapiName(), false);
            return new RemotePowershellConnection(name, uuid, port, null, username, null, certificate, useSSL);
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
        result.set(RemotePowershellConnectionField.USE_SSL.scanName(), this.useSSL);
        return result;
    }

}
