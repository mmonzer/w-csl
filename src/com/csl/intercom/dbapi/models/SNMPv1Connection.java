package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.ConnectionProtocol;
import com.csl.intercom.dbapi.enums.SNMPv2cConnectionField;
import com.ucsl.json.Json;

import java.util.ArrayList;
import java.util.List;

/**
 * Model to represent a SNMPv2c connection.
 */
public class SNMPv1Connection extends Connection {
    private int port;
    private String community;

    protected SNMPv1Connection(int id, int port, List<String> devices, String community) {
        super(id, devices, ConnectionProtocol.SNMPv1);
        this.port = port;
        this.community = community;
    }

    /**
     * Parse the JSON serialization received from DB-API.
     *
     * @param connectionJson The serialized connection as handed by DB-API.
     * @return An instance of {@link SNMPv1Connection} if the parsing was successful, null otherwise.
     */
    public static SNMPv1Connection fromJson(Json connectionJson) {
        try {
            int id = connectionJson.get("id").asInteger();
            int port = connectionJson.get(SNMPv2cConnectionField.PORT.dbapiName()).asInteger();
            List<String> devices = new ArrayList<>();
            for (Json device: connectionJson.get("connected_devices").asJsonList()) {
                devices.add(device.asString());
            }
            String community = connectionJson.get("read_only_other_data").get(SNMPv2cConnectionField.COMMUNITY.dbapiName()).asString();

            return new SNMPv1Connection(id, port, devices, community);
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
