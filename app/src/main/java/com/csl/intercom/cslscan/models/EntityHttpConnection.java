package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.enums.EntityHttpConnectionField;
import com.csl.interfaces.models.IDbapiSerializable;
import com.csl.interfaces.models.IScannerSerializable;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EntityHttpConnection implements IScannerSerializable, IDbapiSerializable {
    private static final Logger logger = LoggerFactory.getLogger(EntityHttpConnection.class);

    @Getter
    private String uuid;

    @Getter
    private String name;
    private Map<String, HttpApiVariable> variables;

    @Getter
    private List<EntityHttpConnectionStage> stages;
    private List<EntityHttpConnectionInput> inputs;

    public Json serializeForScanner() {
        Json stagesSerialized = this.stages.stream()
                .map(EntityHttpConnectionStage::serializeForScanner)
                .collect(Json::array, Json::add, Json::add);

        Json variablesSerialized = Json.object();
        this.variables.forEach((name, variable) -> variablesSerialized.set(name, variable.serializeForScanner()));

        Json inputsSerialized = Json.array();
        if (this.inputs != null) {
            this.inputs.forEach(input -> inputsSerialized.add(input.serializeForScanner()));
        }

        return Json.object(
                EntityHttpConnectionField.UUID.scanName(), uuid,
                EntityHttpConnectionField.NAME.scanName(), name,
                EntityHttpConnectionField.VARIABLES.scanName(), variablesSerialized,
                EntityHttpConnectionField.INPUTS.scanName(), inputsSerialized,
                EntityHttpConnectionField.STAGES.scanName(), stagesSerialized
        );
    }

    public Json serializeForDbapi() {
        Json stagesSerialized = this.stages.stream()
                .map(EntityHttpConnectionStage::serializeForDbapi)
                .collect(Json::array, Json::add, Json::add);

        Json variablesSerialized = Json.object();
        this.variables.forEach((name, variable) -> variablesSerialized.set(name, variable.serializeForDbapi()));

        Json inputsSerialized = Json.array();
        if (this.inputs != null) {
            this.inputs.forEach(input -> inputsSerialized.add(input.serializeForDbapi()));
        }

        return Json.object(
                EntityHttpConnectionField.UUID.dbapiName(), uuid,
                EntityHttpConnectionField.NAME.dbapiName(), name,
                EntityHttpConnectionField.VARIABLES.dbapiName(), variablesSerialized,
                EntityHttpConnectionField.INPUTS.dbapiName(), inputsSerialized,
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
            if (json.has(EntityHttpConnectionField.INPUTS.dbapiName()) && json.get(EntityHttpConnectionField.INPUTS.dbapiName()).isArray()) {
                entityHttpConnection.inputs = json.get(EntityHttpConnectionField.INPUTS.dbapiName()).asJsonList().stream()
                        .map(EntityHttpConnectionInput::fromDbapiJson)
                        .collect(Collectors.toList());
            }
            if (json.has(EntityHttpConnectionField.VARIABLES.dbapiName()) && json.get(EntityHttpConnectionField.VARIABLES.dbapiName()).isObject()) {
                entityHttpConnection.variables = json.get(EntityHttpConnectionField.VARIABLES.dbapiName()).asJsonMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, jsonEntry -> HttpApiVariable.fromDbapiJson(jsonEntry.getValue())));
            } else {
                entityHttpConnection.variables = new HashMap<>();
            }
            return entityHttpConnection;
        } catch (Throwable e) {
            logger.warn("Failed to parse EntityHttpConnection from dbapi json", e);
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
            if (json.has(EntityHttpConnectionField.INPUTS.scanName()) && json.get(EntityHttpConnectionField.INPUTS.scanName()).isArray()) {
                entityHttpConnection.inputs = json.get(EntityHttpConnectionField.INPUTS.scanName()).asJsonList().stream()
                        .map(EntityHttpConnectionInput::fromScannerJson)
                        .collect(Collectors.toList());
            }
            if (json.has(EntityHttpConnectionField.VARIABLES.scanName()) && json.get(EntityHttpConnectionField.VARIABLES.scanName()).isObject()) {
                entityHttpConnection.variables = json.get(EntityHttpConnectionField.VARIABLES.scanName()).asJsonMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, jsonEntry -> HttpApiVariable.fromScannerJson(jsonEntry.getValue())));
            } else {
                entityHttpConnection.variables = new HashMap<>();
            }
            return entityHttpConnection;
        } catch (Throwable e) {
            logger.warn("Failed to parse EntityHttpConnection from scanner json", e);
            return null;
        }
    }

    /**
     * Add a stage to the connection.
     *
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

    public enum Part {
        CONTEXT_INITIALIZATION,
        QUERY,
        JS_PARSER,
        VARIABLES_EXTRACTION,
        CPE_PARSING,
    }


}
