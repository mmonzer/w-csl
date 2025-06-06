package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.RemotePowershellConnectionField;
import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.ucsl.json.Json;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

import static com.ucsl.json.JsonUtil.*;

public class CameraDeployedCertificateConnection extends Connection{
    @Getter
    public static final String MONGO_ENTITY_ID = "mongo_entity_id";
    @Getter
    public static final String CONNECTED_DEVICES = "connected_devices";
    @Getter
    public static final String NAME = "name";
    @Getter
    public static final String UUID = "uuid";
    @Getter
    private final int port;
    @Getter
    private final String username;
    @Getter
    private final String password;
    @Getter
    private Boolean isKeepPassword;
    @Getter
    private final String vendor;


    protected CameraDeployedCertificateConnection(String name, String id, int port, List<String> devices, String username, String password, Boolean isKeepPassword, String vendor) {
        super(name, id, devices, StaticConnectionProtocol.CAMERADEPLOYEDCERTIFICATE);
        this.port = port;
        this.username = username;
        this.password = password;
        this.isKeepPassword = isKeepPassword;
        this.vendor = vendor;
    }
    protected CameraDeployedCertificateConnection(String name, String id, int port, List<String> devices, String username, String password, String vendor) {
        super(name, id, devices, StaticConnectionProtocol.CAMERADEPLOYEDCERTIFICATE);
        this.port = port;
        this.username = username;
        this.password = password;
        this.vendor = vendor;
    }
    public static CameraDeployedCertificateConnection fromJson(Json connectionJson) {
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
            String name = getValueStringOrNull(connectionJson, NAME);
            String vendor = getValueStringOrNull(connectionJson, "vendor");
            return new CameraDeployedCertificateConnection(name, uuid, port, devices, username, password, true ,vendor);
        } catch (Exception e) {
            return null;
        }
    }
    public static CameraDeployedCertificateConnection fromHMIJson(Json connectionJson) {
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
            String vendor = getValueStringOrNull(connectionJson, "vendor");
            String username = getValueStringOrNull(connectionJson, RemotePowershellConnectionField.USERNAME.dbapiName());
            // check if password is present in the json
            if (connectionJson.has(RemotePowershellConnectionField.PASSWORD.dbapiName())) {
                String password = getValueStringOrNull(connectionJson, RemotePowershellConnectionField.PASSWORD.dbapiName());
                return new CameraDeployedCertificateConnection(name, uuid, port, devices, username, password, vendor);
            } else {
                return new CameraDeployedCertificateConnection(name, uuid, port, devices, username, null, true, vendor);
            }
        } catch (NullPointerException | UnsupportedOperationException e) {
            return null;
        }
    }
    public static CameraDeployedCertificateConnection fromAutoCryptJson(Json connectionJson) {
        try {
            String uuid = getValueStringOrNull(connectionJson, UUID);
            String vendor = getValueStringOrNull(connectionJson, "vendor");
            Integer port = connectionJson.get(RemotePowershellConnectionField.PORT.dbapiName()).asInteger();
            String name = getValueStringOrNull(connectionJson, NAME);
            String username = getValueStringOrNull(connectionJson, RemotePowershellConnectionField.USERNAME.dbapiName());
            return new CameraDeployedCertificateConnection(name, uuid, port, null, username, null, vendor);
        } catch (NullPointerException e) {
            return null;
        }
    }
    @Override
    public Json serializeForAutoCrypt() {
        Json result = super.serializeForAutoCrypt();
        result.set("port", this.port);
        result.set("username", this.username);
        result.set("password", this.password);
        result.set("is_keep_password", this.isKeepPassword);
        result.set("vendor", this.vendor);
        result.set("queryProtocol", this.getProtocol().autocryptName());
        return result;
    }

}
