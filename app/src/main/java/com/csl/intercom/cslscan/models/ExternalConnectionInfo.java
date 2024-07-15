package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.cslscan.enums.ExternalConnectionInfoField;
import com.csl.intercom.dbapi.DbapiUtilsForCSLScan;
import com.ucsl.json.Json;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ExternalConnectionInfo {
    private String id;
    private String name;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private boolean isDeleted;
    private String queryProtocol;
    private int queryProtocolId;
    private Map<String, Json> fields;

    private ExternalConnectionInfo(String id, String name, OffsetDateTime createdAt, OffsetDateTime updatedAt, boolean isDeleted, String queryProtocol, int queryProtocolId, Map<String, Json> fields) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
        this.queryProtocol = queryProtocol;
        this.queryProtocolId = queryProtocolId;
        this.fields = fields;
    }

    public static ExternalConnectionInfo fromHMIJson(Json json) {
        if (!json.isObject()) {
            return null;
        }
        AtomicReference<String> id = new AtomicReference<>();
        AtomicReference<String> name = new AtomicReference<>();
        AtomicReference<OffsetDateTime> createdAt = new AtomicReference<>();
        AtomicReference<OffsetDateTime> updatedAt = new AtomicReference<>();
        AtomicBoolean isDeleted = new AtomicBoolean(false);
        AtomicReference<String> queryProtocol = new AtomicReference<>();
        AtomicInteger queryProtocolId = new AtomicInteger();
        Map<String, Json> fields = new HashMap<>();
        json.asJsonMap().forEach((key, value) -> {
            ExternalConnectionInfoField field = ExternalConnectionInfoField.fromScanName(key);
            if (field == null) {
                fields.put(key, value);
            } else {
                switch (field) {
                    case ID:
                        id.set(value.asString());
                        break;
                    case NAME:
                        name.set(value.asString());
                        break;
                    case CREATED_AT:
                        createdAt.set(OffsetDateTime.parse(value.asString()));
                        break;
                    case UPDATED_AT:
                        updatedAt.set(OffsetDateTime.parse(value.asString()));
                        break;
                    case IS_DELETED:
                        isDeleted.set(value.asBoolean());
                        break;
                    case QUERY_PROTOCOL:
                        queryProtocol.set(value.asString());
                        break;
                    case QUERY_PROTOCOL_ID:
                        queryProtocolId.set(value.asInteger());
                        break;
                }
            }
        });
        return new ExternalConnectionInfo(id.get(), name.get(), createdAt.get(), updatedAt.get(), isDeleted.get(), queryProtocol.get(), queryProtocolId.get(), fields);
    }

    public static ExternalConnectionInfo fromScannerJson(Json json) {
        if (!json.isObject()) {
            return null;
        }
        AtomicReference<String> id = new AtomicReference<>();
        AtomicReference<String> name = new AtomicReference<>();
        AtomicReference<OffsetDateTime> createdAt = new AtomicReference<>();
        AtomicReference<OffsetDateTime> updatedAt = new AtomicReference<>();
        AtomicBoolean isDeleted = new AtomicBoolean(false);
        AtomicReference<String> queryProtocol = new AtomicReference<>();
        AtomicInteger queryProtocolId = new AtomicInteger();
        Map<String, Json> fields = new HashMap<>();
        json.asJsonMap().forEach((key, value) -> {
            ExternalConnectionInfoField field = ExternalConnectionInfoField.fromScanName(key);
            if (field == null) {
                fields.put(key, value);
            } else {
                switch (field) {
                    case ID:
                        if (value.isString()) {
                            id.set(value.asString());
                        }
                        break;
                    case NAME:
                        if (value.isString()) {
                            name.set(value.asString());
                        }
                        break;
                    case CREATED_AT:
                        if (value.isString()) {
                            createdAt.set(ScanUtils.scanTimeToLocal(OffsetDateTime.parse(value.asString())));
                        }
                        break;
                    case UPDATED_AT:
                        if (value.isString()) {
                            updatedAt.set(ScanUtils.scanTimeToLocal(OffsetDateTime.parse(value.asString())));
                        }
                        break;
                    case IS_DELETED:
                        if (value.isBoolean()) {
                            isDeleted.set(value.asBoolean());
                        }
                        break;
                    case QUERY_PROTOCOL:
                        if (value.isString()) {
                            queryProtocol.set(value.asString());
                        }
                        break;
                    case QUERY_PROTOCOL_ID:
                        if (value.isNumber()) {
                            queryProtocolId.set(value.asInteger());
                        }
                        break;
                }
            }
        });
        return new ExternalConnectionInfo(id.get(), name.get(), createdAt.get(), updatedAt.get(), isDeleted.get(), queryProtocol.get(), queryProtocolId.get(), fields);
    }

    public Json serializeForDbapi() {
        Json result = Json.object(
                ExternalConnectionInfoField.ID.getDbapiName(), id,
                ExternalConnectionInfoField.NAME.getDbapiName(), name,
                ExternalConnectionInfoField.IS_DELETED.getDbapiName(), isDeleted,
                ExternalConnectionInfoField.QUERY_PROTOCOL.getDbapiName(), queryProtocol,
                ExternalConnectionInfoField.QUERY_PROTOCOL_ID.getDbapiName(), queryProtocolId
        );
        if (this.createdAt != null) {
            result.set(ExternalConnectionInfoField.CREATED_AT.getDbapiName(), DbapiUtilsForCSLScan.localDateToDbapi(this.createdAt).toString());
        }
        if (this.updatedAt != null) {
            result.set(ExternalConnectionInfoField.UPDATED_AT.getDbapiName(), DbapiUtilsForCSLScan.localDateToDbapi(this.updatedAt).toString());
        }
        Json otherData = Json.object();
        this.fields.forEach(otherData::set);
        result.set("other_data", otherData);
        return result;
    }

    public Json serializeForScanner() {
        Json result = Json.object(
                ExternalConnectionInfoField.NAME.getScanName(), name,
                ExternalConnectionInfoField.QUERY_PROTOCOL.getScanName(), queryProtocol,
                ExternalConnectionInfoField.QUERY_PROTOCOL_ID.getScanName(), queryProtocolId
        );
        if (this.id != null) {
            result.set(ExternalConnectionInfoField.ID.getScanName(), this.id);
        }

        if (this.createdAt != null) {
            result.set(ExternalConnectionInfoField.CREATED_AT.getScanName(), this.createdAt.toString());
        }

        if (this.updatedAt != null) {
            result.set(ExternalConnectionInfoField.UPDATED_AT.getScanName(), this.updatedAt.toString());
        }

        if (this.isDeleted) {
            result.set(ExternalConnectionInfoField.IS_DELETED.getScanName(), this.isDeleted);
        }

        this.fields.forEach(result::set);
        return result;
    }

    public String getId() {
        return id;
    }

    public boolean isDeleted() {
        return isDeleted;
    }
}
