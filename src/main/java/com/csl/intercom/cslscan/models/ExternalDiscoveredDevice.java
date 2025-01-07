package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.cslscan.models.scans.ExternalGeneratedConnectionRelatesDevice;
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

    private ExternalGeneratedConnectionRelatesDevice externalGeneratedConnectionRelatesDevice;

    private ExternalDiscoveredDevice(String id, String name, String ipAddress, String connectionInfoUuid, OffsetDateTime createdAt, OffsetDateTime updatedAt, boolean isDeleted,
                                     ExternalGeneratedConnectionRelatesDevice externalGeneratedConnectionRelatesDevice) {
        this.id = id;
        this.name = name;
        this.ipAddress = ipAddress;
        this.connectionInfoUuid = connectionInfoUuid;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
        this.externalGeneratedConnectionRelatesDevice = externalGeneratedConnectionRelatesDevice;
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
        ExternalGeneratedConnectionRelatesDevice externalGeneratedConnectionRelatesDevice = null;

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
        if (json.has("generatedConnectionForDiscoveredDevice")) {
            String username = json.get("generatedConnectionForDiscoveredDevice").get("username").getValue().toString();
            // String password = json.get("generatedConnectionForDiscoveredDevice").get("password").getValue().toString();
            String connectionName = json.get("generatedConnectionForDiscoveredDevice").get("name").getValue().toString();
            int connectionPortNumber = Integer.parseInt(json.get("generatedConnectionForDiscoveredDevice").get("portNumber").toString());
            String vendor = json.get("generatedConnectionForDiscoveredDevice").get("vendor").getValue().toString();
            externalGeneratedConnectionRelatesDevice = new ExternalGeneratedConnectionRelatesDevice();
            externalGeneratedConnectionRelatesDevice.setName(connectionName);
            // externalGeneratedConnectionRelatesDevice.setPassword(password);
            externalGeneratedConnectionRelatesDevice.setUsername(username);
            externalGeneratedConnectionRelatesDevice.setPortNumber(connectionPortNumber);
            externalGeneratedConnectionRelatesDevice.setVendor(vendor);
        }

        return new ExternalDiscoveredDevice(id, name, ipAddress, connectionInfoUuid, createdAt, updatedAt, isDeleted, externalGeneratedConnectionRelatesDevice);
    }

    public Json serializeForDbapi() {
        Json result = Json.object(
                "uuid", id,
                "name", name,
                "ipAddress", ipAddress,
                "connectionInfoUuid", connectionInfoUuid,
                "deleted", isDeleted,
                "generatedConnectionForDiscoveredDevice", externalGeneratedConnectionRelatesDevice.serializeForDbapi()
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