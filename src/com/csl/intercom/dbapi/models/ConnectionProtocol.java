package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.ConnectionProtocolField;
import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import java.util.List;

public class ConnectionProtocol {
    private int id;
    private String name;
    private String description;
    private int defaultPort;
    private String connectionTemplateId;
    private boolean isDynamic;
    private StaticConnectionProtocol staticConnectionProtocol;

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

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String getConnectionTemplateId() {
        return connectionTemplateId;
    }

    public boolean isDynamic() {
        return isDynamic;
    }

    public StaticConnectionProtocol getStaticConnectionProtocol() {
        return staticConnectionProtocol;
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
    public static ConnectionProtocol fromTemplateId(String templateId) {
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
}
