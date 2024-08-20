package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.ConnectionProtocolField;
import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

public class ConnectionProtocol {
    @Getter
    private final int id;
    @Getter
    private final String name;
    @Getter
    private final String description;
    @Getter
    private final int defaultPort;
    @Getter
    private final String connectionTemplateId;
    private final boolean isDynamic;
    @Getter
    private final StaticConnectionProtocol staticConnectionProtocol;

    protected ConnectionProtocol(int id, String name, String description, int defaultPort, String connectionTemplateId, boolean isDynamic) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultPort = defaultPort;
        this.connectionTemplateId = connectionTemplateId;
        this.isDynamic = isDynamic;
        if (!isDynamic) {
            this.staticConnectionProtocol = StaticConnectionProtocol.fromDbapiName(name);
        } else {
            this.staticConnectionProtocol = StaticConnectionProtocol.HTTP;
        }
    }

    public boolean isDynamic() {
        return isDynamic;
    }
    /**
     * Parse the JSON serialization received from DB-API.
     *
     * @param json The serialized connection protocol as handed by DB-API.
     * @return An instance of {@link ConnectionProtocol} if the parsing was successful, null otherwise.
     */
    public static ConnectionProtocol fromJson(Json json) {
        try {
            int id = json.get(ConnectionProtocolField.ID.dbapiName()).asInteger();
            String name = json.get(ConnectionProtocolField.NAME.dbapiName()).asString();
            int defaultPort = Integer.parseInt(json.get(ConnectionProtocolField.DEFAULT_PORT.dbapiName()).asString());  // We receive the default port as a string
            boolean isDynamic = json.get(ConnectionProtocolField.IS_DYNAMIC.dbapiName()).asBoolean();

            String description;
            Json descriptionJson = json.get(ConnectionProtocolField.DESCRIPTION.dbapiName());
            if (descriptionJson == null || descriptionJson.isNull()) {
                description = null;
            } else {
                description = descriptionJson.asString();
            }

            String connectionTemplateId;
            Json connectionTemplateIdJson = json.get(ConnectionProtocolField.CONNECTION_TEMPLATE_ID.dbapiName());
            if (connectionTemplateIdJson == null || connectionTemplateIdJson.isNull()) {
                connectionTemplateId = null;
            } else {
                connectionTemplateId = connectionTemplateIdJson.asString();
            }

            if (isDynamic && connectionTemplateId == null) {
                return null;
            }
            return new ConnectionProtocol(id, name, description, defaultPort, connectionTemplateId, isDynamic);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Create a fake protocol from a template id.
     * This is used to create a protocol for a connection that is not yet in the database when testing.
     *
     * @param templateId The template id of the protocol to create.
     * @return A fake protocol.
     */
    public static ConnectionProtocol createFakeConnectionProtocol(String templateId) {
        return new ConnectionProtocol(0, "Mock protocol", "Mock protocol", 0, templateId, true);
    }

    /**
     * Get a protocol by its id from a list of protocols.
     *
     * @param protocols The list of protocols to search in.
     * @param id The id of the protocol to search for.
     * @return The protocol with the given id, or null if not found.
     */
    public static ConnectionProtocol getProtocolById(List<ConnectionProtocol> protocols, int id) {
        return protocols.stream().filter(p -> p != null && p.id == id).findFirst().orElse(null);
    }
    public static ConnectionProtocol getProtocolFromScanConnectionJson(List<ConnectionProtocol> protocols, Json connectionJson) {
        StaticConnectionProtocol staticConnectionProtocol;
        if (connectionJson.has("queryProtocol") && connectionJson.get("queryProtocol").isString()) {
            staticConnectionProtocol = StaticConnectionProtocol.fromScanName(connectionJson.get("queryProtocol").asString());
            if (staticConnectionProtocol == null) {
                return null;
            }
        } else {
            return null;
        }
        List<ConnectionProtocol> possibleProtocols = protocols.stream()
                .filter(p -> p.staticConnectionProtocol == staticConnectionProtocol)
                .collect(Collectors.toList());
        if (possibleProtocols.size() == 1) {
            return possibleProtocols.get(0);
        } else {
            String connectionTemplateId = JsonUtil.getStringFromJson(connectionJson, "connectionTemplateId", null);
            return possibleProtocols.stream()
                    .filter(p -> p.connectionTemplateId.equals(connectionTemplateId))
                    .findFirst()
                    .orElse(null);
        }
    }

}
