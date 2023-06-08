package com.csl.intercom.dbapi.models;

import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.dbapi.DbapiUtils;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.csl.intercom.dbapi.DbapiUtils.getConnectionById;

/**
 * Model to represent a device from DB-API.
 */
public class Device {
    private String id;
    private String name;
    private String ipAddress;
    private List<Integer> connectionsIds;
    private List<Connection> connections = new ArrayList<>();
    private OffsetDateTime updatedDate;

    protected Device(String id, String name, String ipAddress, List<Integer> connectionsIds, OffsetDateTime updatedDate) {
        this.id = id;
        this.name = name;
        this.ipAddress = ipAddress;
        this.connectionsIds = connectionsIds;
        this.updatedDate = updatedDate;
    }

    /**
     * Create a {@link Device} from the JSON object received from DB-API.
     *
     * @param deviceJson The serialized device as handed by DB-API.
     * @return A {@link Device} deserialized.
     */
    public static Device fromJson(Json deviceJson) {
        try {
            String id = deviceJson.get("uuid").asString();
            String name = deviceJson.get("name").asString();
            String ipAddress = null;
            if (deviceJson.has("ipv4")) {
                ipAddress = deviceJson.get("ipv4").asString();
            } else {
                ipAddress = deviceJson.get("ipv6").asString();
            }
            // Parse connections
            List<Json> connectionsJson = deviceJson.get("connections").asJsonList();
            List<Integer> connections = new ArrayList<>(connectionsJson.size());
            for (Json connectionId: connectionsJson) {
                connections.add(connectionId.asInteger());
            }
            OffsetDateTime updatedDate = DbapiUtils.dbapiDateToLocal(JsonUtil.getStringFromJson(deviceJson, "updated_at", null));

            return new Device(id, name, ipAddress, connections, updatedDate);
        } catch (NullPointerException | UnsupportedOperationException e) {
            return null;
        }
    }

    /**
     * Serialize the device in a suitable format to send to CSL-Scan.
     * Should be called only after setConnections was called.
     *
     * @return A {@link Json} ready to be sent to CSL-Scan.
     */
    public Json serializeForScanner() {
        Json result = Json.object(
                "uuid", this.id,
                "name", this.name,
                "ipAddress", this.ipAddress,
                "updatedAt", ScanUtils.localTimeToScan(this.updatedDate).toString()
        );
        Json connectionsInfo = Json.array();
        for (Connection connection: this.connections) {
            connectionsInfo.add(connection.serializeForScanner());
        }
        if (!connectionsInfo.asList().isEmpty()) {
            result.set("connectionInfos", connectionsInfo);
//            result.set("port", connectionsInfo.asJsonList().get(0).get("port"));
        }

        return result;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public List<Integer> getConnectionsIds() {
        return connectionsIds;
    }

    public void setConnections(List<Connection> connections) {
        for (int id: connectionsIds) {
            Connection connection = DbapiUtils.getConnectionById(connections, id);
            if (connection != null) {
                this.connections.add(connection);
            }
        }
    }
}
