package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.enums.EntityHttpConnectionField;
import com.ucsl.json.Json;

import java.util.List;
import java.util.stream.Collectors;

public class EntityHttpConnection {
    private String uuid;
    private String name;
    private List<EntityHttpConnectionStage> stages;

    public Json serializeForScanner() {
        Json stagesSerialized = this.stages.stream()
                .map(EntityHttpConnectionStage::serializeForScanner)
                .collect(Json::array, Json::add, Json::add);

        return Json.object(
                EntityHttpConnectionField.UUID.scanName(), uuid,
                EntityHttpConnectionField.NAME.scanName(), name,
                EntityHttpConnectionField.STAGES.scanName(), stagesSerialized
        );
    }

    public Json serializeForDbapi() {
        Json stagesSerialized = this.stages.stream()
                .map(EntityHttpConnectionStage::serializeForDbapi)
                .collect(Json::array, Json::add, Json::add);

        return Json.object(
                EntityHttpConnectionField.UUID.dbapiName(), uuid,
                EntityHttpConnectionField.NAME.dbapiName(), name,
                EntityHttpConnectionField.STAGES.dbapiName(), stagesSerialized
        );
    }

    public static EntityHttpConnection fromDbapiJson(Json json) {
        EntityHttpConnection entityHttpConnection = new EntityHttpConnection();
        try {
            entityHttpConnection.uuid = json.get(EntityHttpConnectionField.UUID.dbapiName()).asString();
            entityHttpConnection.name = json.get(EntityHttpConnectionField.NAME.dbapiName()).asString();
            entityHttpConnection.stages = json.get(EntityHttpConnectionField.STAGES.dbapiName()).asJsonList().stream()
                    .map(EntityHttpConnectionStage::fromDbapiJson)
                    .collect(Collectors.toList());
            return entityHttpConnection;
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static EntityHttpConnection fromScannerJson(Json json) {
        EntityHttpConnection entityHttpConnection = new EntityHttpConnection();
        try {
            entityHttpConnection.uuid = json.get(EntityHttpConnectionField.UUID.scanName()).asString();
            entityHttpConnection.name = json.get(EntityHttpConnectionField.NAME.scanName()).asString();
            entityHttpConnection.stages = json.get(EntityHttpConnectionField.STAGES.scanName()).asJsonList().stream()
                    .map(EntityHttpConnectionStage::fromScannerJson)
                    .collect(Collectors.toList());
            return entityHttpConnection;
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            return null;
        }
    }
}
