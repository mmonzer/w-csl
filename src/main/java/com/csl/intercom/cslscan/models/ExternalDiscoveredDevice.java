package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.cslscan.models.scans.ExternalGeneratedConnectionRelatesDevice;
import com.csl.intercom.dbapi.DbapiUtilsForCSLScan;
import com.ucsl.json.Json;

import java.time.OffsetDateTime;

public class ExternalDiscoveredDevice {
    public static final String CREATED_AT = "createdAt";
    public static final String UPDATED_AT = "updatedAt";
    public static final String DELETED = "deleted";
    public static final String CONNECTION_INFO_UUID = "connectionInfoUuid";
    public static final String IP_ADDRESS = "ipAddress";
    public static final String DEVICE_NAME = "name";
    public static final String DISCOVERED_DEVICE_UUID = "uuid";
    public static final String GENERATED_CONNECTION_FOR_DISCOVERED_DEVICE = "generatedConnectionForDiscoveredDevice";
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

        if (json.has(DISCOVERED_DEVICE_UUID) && json.get(DISCOVERED_DEVICE_UUID).isString()) {
            id = json.get(DISCOVERED_DEVICE_UUID).asString();
        } else {
            return null;
        }

        if (json.has(DEVICE_NAME) && json.get(DEVICE_NAME).isString()) {
            name = json.get(DEVICE_NAME).asString();
        } else {
            return null;
        }

        if (json.has(IP_ADDRESS) && json.get(IP_ADDRESS).isString()) {
            ipAddress = json.get(IP_ADDRESS).asString();
        } else {
            return null;
        }

        if (json.has(CONNECTION_INFO_UUID) && json.get(CONNECTION_INFO_UUID).isString()) {
            connectionInfoUuid = json.get(CONNECTION_INFO_UUID).asString();
        } else {
            return null;
        }

        if (json.has(CREATED_AT) && json.get(CREATED_AT).isString()) {
            createdAt = ScanUtils.scanTimeToLocal(OffsetDateTime.parse(json.get(CREATED_AT).asString()));
        } else {
            createdAt = null;
        }

        if (json.has(UPDATED_AT) && json.get(UPDATED_AT).isString()) {
            updatedAt = ScanUtils.scanTimeToLocal(OffsetDateTime.parse(json.get(UPDATED_AT).asString()));
        } else {
            return null;
        }

        if (json.has(DELETED) && json.get(DELETED).isBoolean()) {
            isDeleted = json.get(DELETED).asBoolean();
        } else {
            return null;
        }
        if (json.has(GENERATED_CONNECTION_FOR_DISCOVERED_DEVICE)) {
            Json generatedConnectionForDiscoveredDevice = json.get(GENERATED_CONNECTION_FOR_DISCOVERED_DEVICE);
            String username = generatedConnectionForDiscoveredDevice.get("username").getValue().toString();
            String connectionName = generatedConnectionForDiscoveredDevice.get(DEVICE_NAME).getValue().toString();
            int connectionPortNumber = Integer.parseInt(generatedConnectionForDiscoveredDevice.get("portNumber").toString());
            String vendor = generatedConnectionForDiscoveredDevice.get("vendor").getValue().toString();
            externalGeneratedConnectionRelatesDevice = new ExternalGeneratedConnectionRelatesDevice();
            externalGeneratedConnectionRelatesDevice.setName(connectionName);
            externalGeneratedConnectionRelatesDevice.setUsername(username);
            externalGeneratedConnectionRelatesDevice.setPortNumber(connectionPortNumber);
            externalGeneratedConnectionRelatesDevice.setVendor(vendor);
        }

        return new ExternalDiscoveredDevice(id, name, ipAddress, connectionInfoUuid, createdAt, updatedAt, isDeleted, externalGeneratedConnectionRelatesDevice);
    }

    public Json serializeForDbapi() {
        Json result = Json.object(
                DISCOVERED_DEVICE_UUID, id,
                DEVICE_NAME, name,
                IP_ADDRESS, ipAddress,
                CONNECTION_INFO_UUID, connectionInfoUuid,
                DELETED, isDeleted,
                GENERATED_CONNECTION_FOR_DISCOVERED_DEVICE, externalGeneratedConnectionRelatesDevice.serializeForDbapi()
        );
        if (createdAt != null) {
            result.set(CREATED_AT, DbapiUtilsForCSLScan.localDateToDbapi(createdAt).toString());
        }

        if (updatedAt != null) {
            result.set(UPDATED_AT, DbapiUtilsForCSLScan.localDateToDbapi(updatedAt).toString());
        }

        return result;
    }
}