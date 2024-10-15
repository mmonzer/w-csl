package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.SNMPv1ConnectionField;
import com.csl.intercom.dbapi.enums.SNMPv2cConnectionField;
import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.ucsl.json.Json;

import java.util.ArrayList;
import java.util.List;

/**
 * Model to represent a SNMPv2c connection.
 */
public class SNMPv1Connection extends Connection {
    private final int port;
    private final String community;

    protected SNMPv1Connection(String uuid, int port, List<String> devices, String community) {
        super(uuid, devices, StaticConnectionProtocol.SNMPv1);
        this.port = port;
        this.community = community;
    }
    protected SNMPv1Connection(String name, String uuid, int port, List<String> devices, String community) {
        super(name, uuid, devices, StaticConnectionProtocol.SNMPv1);
        this.port = port;
        this.community = community;
    }
    public int getPort() {
        return port;
    }

    /**
     * Parse the JSON serialization received from DB-API.
     *
     * @param connectionJson The serialized connection as handed by DB-API.
     * @return An instance of {@link SNMPv1Connection} if the parsing was successful, null otherwise.
     */
    public static SNMPv1Connection fromJson(Json connectionJson) {
        try {
            // check if connectionJson has id field
            String uuid;
            if (connectionJson.has("uuid")) {
                uuid = connectionJson.get("uuid").asString();
            } else {
                if(connectionJson.has("mongo_entity_id") && !connectionJson.get("mongo_entity_id").isNull())
                    uuid = connectionJson.get("mongo_entity_id").asString();
                else
                    uuid = null;
            }

            int port = connectionJson.get(SNMPv1ConnectionField.PORT.dbapiName()).asInteger();
            List<String> devices = new ArrayList<>();
            for (Json device: connectionJson.get("connected_devices").asJsonList()) {
                devices.add(device.asString());
            }
            Json otherData = connectionJson.get("other_data");
            Json readOnlyOtherData = connectionJson.get("read_only_other_data");
            String community = null;
            if (otherData != null) {
                community = String.valueOf(otherData.get(SNMPv1ConnectionField.COMMUNITY.dbapiName()).getValue());
            } else if (readOnlyOtherData != null) {
                community = String.valueOf(readOnlyOtherData.get(SNMPv1ConnectionField.COMMUNITY.dbapiName()).getValue());
            }
            String name = connectionJson.get("name").asString();
            return new SNMPv1Connection(name, uuid, port, devices, community);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public static SNMPv1Connection fromScannerJson(Json connectionJson) {
        try {
            String uuid = null;
            if (connectionJson.has("uuid") && connectionJson.get("uuid").isString()) {
                uuid = connectionJson.get("uuid").asString();
            }
            int port = connectionJson.get(SNMPv2cConnectionField.PORT.scanName()).asInteger();
            String community = connectionJson.get(SNMPv2cConnectionField.COMMUNITY.scanName()).asString();
            String name = connectionJson.get("name").asString();
            return new SNMPv1Connection(name, uuid, port, null, community);
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
