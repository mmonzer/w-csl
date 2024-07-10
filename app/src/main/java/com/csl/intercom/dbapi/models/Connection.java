package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.csl.interfaces.models.IScannerSerializable;
import com.ucsl.json.Json;

import java.util.List;

/**
 * Model representing a DB-API connection.
 * Abstract class, concrete children are tied to the actual protocols of a connection.
 */
public abstract class Connection implements IScannerSerializable {
    private int id;
    private List<String> devicesIds;
    private StaticConnectionProtocol protocol;
    private Boolean isSimulated;

    protected Connection(int id, List<String> devicesIds, StaticConnectionProtocol protocol) {
        this.id = id;
        this.devicesIds = devicesIds;
        this.protocol = protocol;
        this.isSimulated = false;
    }

    protected Connection(int id, List<String> devicesIds, StaticConnectionProtocol protocol, boolean isSimulated) {
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
    public static Connection fromDbapiJson(Json connectionJson, List<ConnectionProtocol> protocols) {
        ConnectionProtocol connectionProtocol;
        StaticConnectionProtocol protocol;
        try {
            int protocolId = connectionJson.get("discovery_protocol").asInteger();
            connectionProtocol = ConnectionProtocol.getProtocolById(protocols, protocolId);
            protocol = connectionProtocol.getStaticConnectionProtocol();
        } catch (UnsupportedOperationException | NullPointerException e) {
            return null;
        }

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
                return HttpConnection.fromJson(connectionJson, connectionProtocol);

            case SSH:
                return SshConnection.fromJson(connectionJson);

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

    public StaticConnectionProtocol getProtocol() {
        return protocol;
    }

    public List<String> getDevicesIds() {
        return devicesIds;
    }

    public Boolean isSimulated() {
        return isSimulated;
    }
}
