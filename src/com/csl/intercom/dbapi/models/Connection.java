package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.ConnectionProtocol;
import com.ucsl.json.Json;

import java.util.List;

/**
 * Model representing a DB-API connection.
 * Abstract class, concrete children are tied to the actual protocols of a connection.
 */
public abstract class Connection {
    private int id;
    private List<String> devicesIds;
    private ConnectionProtocol protocol;
    private Boolean isSimulated;

    protected Connection(int id, List<String> devicesIds, ConnectionProtocol protocol) {
        this.id = id;
        this.devicesIds = devicesIds;
        this.protocol = protocol;
        this.isSimulated = false;
    }

    protected Connection(int id, List<String> devicesIds, ConnectionProtocol protocol, boolean isSimulated) {
        this.id = id;
        this.devicesIds = devicesIds;
        this.protocol = protocol;
        this.isSimulated = isSimulated;
    }

    /**
     * Parse the JSON serialization received from DB-API.
     *
     * @param connectionJson The serialized connection as handed by DB-API.
     * @return An instance of the correct child if the parsing was successful, or null.
     */
    public static Connection fromJson(Json connectionJson) {
        ConnectionProtocol protocol = ConnectionProtocol.fromDbapiName(connectionJson.get("discovery_protocol_name").asString());

        if (protocol == null) {
            return null;
        }
        switch (protocol) {
            case SNMPv1:
                return SNMPv1Connection.fromJson(connectionJson);
            case SNMPv2c:
                return SNMPv2cConnection.fromJson(connectionJson);

            case SNMPv3:
                return SNMPv3Connection.fromJson(connectionJson);

            case RemotePowershell:
                return RemotePowershellConnection.fromJson(connectionJson);

            case HTTP:
                return HttpConnection.fromJson(connectionJson);

            default:
                return null;
        }
    }

    /**
     * Serialize the connection information in a format suitable to be sent to CSL-Scan (in the ConnectionInfos field).
     * Should be overriden by children.
     *
     * @return The serialized version of the connection ready to be included in a CSL-Scan's entity.
     */
    public Json serializeForScanner() {
        return Json.object(
                "queryProtocol", this.protocol.scanName(),
                "uuid", this.id
        );
    }

    public int getId() {
        return id;
    }

    public ConnectionProtocol getProtocol() {
        return protocol;
    }

    public List<String> getDevicesIds() {
        return devicesIds;
    }

    public Boolean isSimulated() {
        return isSimulated;
    }
}
