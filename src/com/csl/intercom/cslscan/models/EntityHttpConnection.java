package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.enums.EntityHttpConnectionField;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EntityHttpConnection {
    private String uuid;
    private String name;
    private Map<String, HttpApiVariable> variables;
    private List<EntityHttpConnectionStage> stages;

    public Json serializeForScanner() {
        Json stagesSerialized = this.stages.stream()
                .map(EntityHttpConnectionStage::serializeForScanner)
                .collect(Json::array, Json::add, Json::add);

       Json variablesSerialized = Json.object();
       this.variables.forEach((name, variable) -> variablesSerialized.set(name, variable.serializeForScanner()));

        return Json.object(
                EntityHttpConnectionField.UUID.scanName(), uuid,
                EntityHttpConnectionField.NAME.scanName(), name,
                EntityHttpConnectionField.VARIABLES.scanName(), variablesSerialized,
                EntityHttpConnectionField.STAGES.scanName(), stagesSerialized
        );
    }

    public Json serializeForDbapi() {
        Json stagesSerialized = this.stages.stream()
                .map(EntityHttpConnectionStage::serializeForDbapi)
                .collect(Json::array, Json::add, Json::add);

        Json variablesSerialized = Json.object();
        this.variables.forEach((name, variable) -> variablesSerialized.set(name, variable.serializeForDbapi()));

        return Json.object(
                EntityHttpConnectionField.UUID.dbapiName(), uuid,
                EntityHttpConnectionField.NAME.dbapiName(), name,
                EntityHttpConnectionField.VARIABLES.dbapiName(), variablesSerialized,
                EntityHttpConnectionField.STAGES.dbapiName(), stagesSerialized
        );
    }

    public static EntityHttpConnection fromDbapiJson(Json json) {
        EntityHttpConnection entityHttpConnection = new EntityHttpConnection();
        try {
            entityHttpConnection.uuid = JsonUtil.getStringFromJson(json, EntityHttpConnectionField.UUID.dbapiName(), null);
            entityHttpConnection.name = JsonUtil.getStringFromJson(json, EntityHttpConnectionField.NAME.dbapiName(), null);
            entityHttpConnection.stages = json.get(EntityHttpConnectionField.STAGES.dbapiName()).asJsonList().stream()
                    .map(EntityHttpConnectionStage::fromDbapiJson)
                    .collect(Collectors.toList());
            if (json.has(EntityHttpConnectionField.VARIABLES.dbapiName()) && json.get(EntityHttpConnectionField.VARIABLES.dbapiName()).isObject()) {
                entityHttpConnection.variables = json.get(EntityHttpConnectionField.VARIABLES.dbapiName()).asJsonMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, jsonEntry -> HttpApiVariable.fromDbapiJson(jsonEntry.getValue())));
            } else {
                entityHttpConnection.variables = new HashMap<>();
            }
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
            if (json.has(EntityHttpConnectionField.VARIABLES.scanName()) && json.get(EntityHttpConnectionField.VARIABLES.scanName()).isObject()) {
                entityHttpConnection.variables = json.get(EntityHttpConnectionField.VARIABLES.scanName()).asJsonMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, jsonEntry -> HttpApiVariable.fromScannerJson(jsonEntry.getValue())));
            } else {
                entityHttpConnection.variables = new HashMap<>();
            }
            return entityHttpConnection;
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    /**
     * Add a stage to the connection.
     * @param stage The stage to add.
     * @return The connection itself.
     */
    public EntityHttpConnection addStage(EntityHttpConnectionStage stage) {
        if (this.stages == null) {
            this.stages = new ArrayList<>();
        }
        this.stages.add(stage);
        return this;
    }

    public List<EntityHttpConnectionStage> getStages() {
        return stages;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public enum Part {
        CONTEXT_INITIALIZATION,
        QUERY,
        JS_PARSER,
        VARIABLES_EXTRACTION,
        CPE_PARSING,
    }
}
