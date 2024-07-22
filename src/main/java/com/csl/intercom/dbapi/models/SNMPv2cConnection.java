package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.SNMPv2cConnectionField;
import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.ucsl.json.Json;

import java.util.ArrayList;
import java.util.List;

/**
 * Model to represent a SNMPv2c connection.
 */
public class SNMPv2cConnection extends Connection {
    private final int port;
    private final String community;

    protected SNMPv2cConnection(String id, int port, List<String> devices, String community) {
        super(id, devices, StaticConnectionProtocol.SNMPv2c);
        this.port = port;
        this.community = community;
    }

    /**
     * Parse the JSON serialization received from DB-API.
     *
     * @param connectionJson The serialized connection as handed by DB-API.
     * @return An instance of {@link SNMPv2cConnection} if the parsing was successful, null otherwise.
     */
    public static SNMPv2cConnection fromJson(Json connectionJson) {
        try {
            String id = connectionJson.get("id").asString();
            int port = connectionJson.get(SNMPv2cConnectionField.PORT.dbapiName()).asInteger();
            List<String> devices = new ArrayList<>();
            for (Json device: connectionJson.get("connected_devices").asJsonList()) {
                devices.add(device.asString());
            }
            String community = connectionJson.get("read_only_other_data").get(SNMPv2cConnectionField.COMMUNITY.dbapiName()).asString();

            return new SNMPv2cConnection(id, port, devices, community);
        } catch (NullPointerException e) {
            return null;
        }
    }
    public static SNMPv2cConnection fromScannerJson(Json connectionJson) {
        try {
            String uuid = null;
            if (connectionJson.has("uuid") && connectionJson.get("uuid").isString()) {
                uuid = connectionJson.get("uuid").asString();
            }
            int port = connectionJson.get(SNMPv2cConnectionField.PORT.scanName()).asInteger();
            String community = connectionJson.get(SNMPv2cConnectionField.COMMUNITY.scanName()).asString();

            return new SNMPv2cConnection(uuid, port, null, community);
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public Json serializeForScanner() {
        Json result = super.serializeForScanner();
        result.set("community", this.community);
        result.set("port", port);
        return result;
    }

    public String getCommunity() {
        return community;
    }
}
