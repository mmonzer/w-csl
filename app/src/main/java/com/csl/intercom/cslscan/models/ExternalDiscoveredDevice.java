package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.dbapi.DbapiUtilsForCSLScan;
import com.ucsl.json.Json;

import java.time.OffsetDateTime;

public class ExternalDiscoveredDevice {
    private String id;
    private String name;
    private String ipAddress;
    private String connectionInfoUuid;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private boolean isDeleted;

    private ExternalDiscoveredDevice(String id, String name, String ipAddress, String connectionInfoUuid, OffsetDateTime createdAt, OffsetDateTime updatedAt, boolean isDeleted) {
        this.id = id;
        this.name = name;
        this.ipAddress = ipAddress;
        this.connectionInfoUuid = connectionInfoUuid;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

    public static ExternalDiscoveredDevice fromScannerJson(Json json) {
        if (!json.isObject()) {
            return null;
        }

        String id;
        String name;
        String ipAddress;
        String connectionInfoUuid;
        OffsetDateTime createdAt;
        OffsetDateTime updatedAt;
        boolean isDeleted;

        if (json.has("uuid") && json.get("uuid").isString()) {
            id = json.get("uuid").asString();
        } else {
            return null;
        }

        if (json.has("name") && json.get("name").isString()) {
            name = json.get("name").asString();
        } else {
            return null;
        }

        if (json.has("ipAddress") && json.get("ipAddress").isString()) {
            ipAddress = json.get("ipAddress").asString();
        } else {
            return null;
        }

        if (json.has("connectionInfoUuid") && json.get("connectionInfoUuid").isString()) {
            connectionInfoUuid = json.get("connectionInfoUuid").asString();
        } else {
            return null;
        }

        if (json.has("createdAt") && json.get("createdAt").isString()) {
            createdAt = ScanUtils.scanTimeToLocal(OffsetDateTime.parse(json.get("createdAt").asString()));
        } else {
            createdAt = null;
        }

        if (json.has("updatedAt") && json.get("updatedAt").isString()) {
            updatedAt = ScanUtils.scanTimeToLocal(OffsetDateTime.parse(json.get("updatedAt").asString()));
        } else {
            return null;
        }

        if (json.has("deleted") && json.get("deleted").isBoolean()) {
            isDeleted = json.get("deleted").asBoolean();
        } else {
            return null;
        }

        return new ExternalDiscoveredDevice(id, name, ipAddress, connectionInfoUuid, createdAt, updatedAt, isDeleted);
    }

    public Json serializeForDbapi() {
        Json result = Json.object(
                "uuid", id,
                "name", name,
                "ipAddress", ipAddress,
                "connectionInfoUuid", connectionInfoUuid,
                "deleted", isDeleted
        );
        if (createdAt != null) {
            result.set("createdAt", DbapiUtilsForCSLScan.localDateToDbapi(createdAt).toString());
        }

        if (updatedAt != null) {
            result.set("updatedAt", DbapiUtilsForCSLScan.localDateToDbapi(updatedAt).toString());
        }

        return result;
    }
}
