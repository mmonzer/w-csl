package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.csl.interfaces.models.IScannerSerializable;
import com.ucsl.json.Json;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Model representing a DB-API connection.
 * Abstract class, concrete children are tied to the actual protocols of a connection.
 */
public abstract class Connection implements IScannerSerializable {
    @Getter @Setter
    private String name;
    @Getter @Setter
    private String uuid;
    private List<String> devicesIds;
    private StaticConnectionProtocol protocol;
    @Getter
    private Boolean isSimulated;

    protected Connection(String name, String uuid, List<String> devicesIds, StaticConnectionProtocol protocol) {
        this(name, uuid, devicesIds, protocol, null);
    }

    protected Connection(String uuid, List<String> devicesIds, StaticConnectionProtocol protocol) {
        this(null, uuid, devicesIds, protocol, false);
    }

    protected Connection(String uuid, List<String> devicesIds, StaticConnectionProtocol protocol, boolean isSimulated) {
        this(null, uuid, devicesIds, protocol, isSimulated);
    }

    protected Connection(String name, String id, List<String> devices, StaticConnectionProtocol staticConnectionProtocol, Boolean isSimulated) {
        this.name = name;
        this.uuid = id;
        this.devicesIds = devices;
        this.protocol = staticConnectionProtocol;
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
        return switch (protocol) {
            case SNMP_V1 -> SNMPv1Connection.fromJson(connectionJson);
            case SNMP_V2C -> SNMPv2cConnection.fromJson(connectionJson);
            case SNMP_V3 -> SNMPv3Connection.fromJson(connectionJson);
            case REMOTE_POWERSHELL -> RemotePowershellConnection.fromJson(connectionJson);
            case HTTP -> HttpConnection.fromJson(connectionJson, connectionProtocol);
            case SSH -> SshConnection.fromJson(connectionJson);
            default -> null;
        };
    }

    public static Connection fromHMIJson(Json connectionJson, List<ConnectionProtocol> protocols) {
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
        return switch (protocol) {
            case SNMP_V1 -> SNMPv1Connection.fromJson(connectionJson);
            case SNMP_V2C -> SNMPv2cConnection.fromJson(connectionJson);
            case SNMP_V3 -> SNMPv3Connection.fromJson(connectionJson);
            case REMOTE_POWERSHELL -> RemotePowershellConnection.fromHMIJson(connectionJson);
            case HTTP -> HttpConnection.fromJson(connectionJson, connectionProtocol);
            case SSH -> SshConnection.fromJson(connectionJson);
            default -> null;
        };
    }

    public static Connection fromScannerJson(Json connectionJson, List<ConnectionProtocol> protocols) {
        ConnectionProtocol protocol = ConnectionProtocol.getProtocolFromScanConnectionJson(protocols, connectionJson);
        if (protocol == null) {
            return null;
        }
        return switch (protocol.getStaticConnectionProtocol()) {
            case SNMP_V1 -> SNMPv1Connection.fromScannerJson(connectionJson);
            case SNMP_V2C -> SNMPv2cConnection.fromScannerJson(connectionJson);
            case SNMP_V3 -> SNMPv3Connection.fromScannerJson(connectionJson);
            case REMOTE_POWERSHELL -> RemotePowershellConnection.fromScannerJson(connectionJson);
            case HTTP -> HttpConnection.fromScannerJson(connectionJson);
            case SSH -> SshConnection.fromScannerJson(connectionJson);
            default -> null;
        };
    }
    /**
     * Serialize the connection information in a format suitable to be sent to CSL-Scan (in the ConnectionInfos field).
     * Should be overriden by children.
     *
     * @return The serialized version of the connection ready to be included in a CSL-Scan's entity.
     */
    public Json serializeForScanner() {
        Json result = Json.object(
                "queryProtocol", this.protocol.scanName()
        );
        if (this.uuid != null) {
            result.set("uuid", this.uuid);
        }
        if (this.devicesIds != null) {
            result.set("connected_devices", this.devicesIds);
        }
        if (this.name != null) {
            result.set("name", this.name);
        }
        return result;
    }

    public Json serializeForAutoCrypt() {
        Json result = Json.object(
                "protocol", this.protocol.autocryptName()
        );
        if (this.uuid != null) {
            result.set("uuid", this.uuid);
        }
        if (this.devicesIds != null) {
            result.set("connected_devices", this.devicesIds);
        }
        if (this.name != null) {
            result.set("name", this.name);
        }
        return result;
    }

    public String getId() {
        return uuid;
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
