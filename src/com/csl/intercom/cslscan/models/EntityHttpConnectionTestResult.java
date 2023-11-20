package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.enums.EntityHttpConnectionTestResultDetailsField;
import com.ucsl.json.Json;

import java.util.Map;
import java.util.stream.Collectors;

public class EntityHttpConnectionTestResult {
    private boolean success;

    private Details details = null;

    public Json serializeForDbapi() {
        Json detailsSerialized = null;
        if (details != null) {
            detailsSerialized = details.serializeForDbapi();
        }
        return Json.object(
                "success", success,
                "details", detailsSerialized
        );
    }

    public static EntityHttpConnectionTestResult fromScannerJson(Json json) {
        EntityHttpConnectionTestResult entityHttpConnectionTestResult = new EntityHttpConnectionTestResult();
        if (json.has("success") && json.get("success").isBoolean()) {
            entityHttpConnectionTestResult.success = json.get("success").asBoolean();
        }
        if (json.has("details") && json.get("details").isObject()) {
            entityHttpConnectionTestResult.details = Details.fromScannerJson(json.get("details"));
        }
        return entityHttpConnectionTestResult;
    }

    public static class Details {
        private String failedStageUuid;
        private String failedStageName;
        private Integer failedStageIndex;
        private EntityHttpConnection.Part failedPart;
        private String errorMessage;
        private Integer statusCode;
        private Map<String, HttpApiVariable> deviceVariables;
        private Map<String, HttpApiVariable> connectionVariables;
        private Map<String, HttpApiVariable> customVariables;
        private String body;
        private String cpeItems;

        public Json serializeForDbapi() {
            Json variablesSerialized = Json.object();
            if (this.deviceVariables != null) {
                Json deviceVariablesSerialized = Json.object();
                this.deviceVariables.forEach((name, variable) -> deviceVariablesSerialized.set(name, variable.serializeForDbapi()));
                variablesSerialized.set("device", deviceVariablesSerialized);
            }
            if (this.connectionVariables != null) {
                Json connectionVariablesSerialized = Json.object();
                this.connectionVariables.forEach((name, variable) -> connectionVariablesSerialized.set(name, variable.serializeForDbapi()));
                variablesSerialized.set("connection", connectionVariablesSerialized);
            }
            if (this.customVariables != null) {
                Json customVariablesSerialized = Json.object();
                this.customVariables.forEach((name, variable) -> customVariablesSerialized.set(name, variable.serializeForDbapi()));
                variablesSerialized.set("custom", customVariablesSerialized);
            }

            Json serialized = Json.object();
            if (failedStageUuid != null) {
                serialized.set(EntityHttpConnectionTestResultDetailsField.FAILED_STAGE_UUID.dbapiName(), failedStageUuid);
            }
            if (failedStageName != null) {
                serialized.set(EntityHttpConnectionTestResultDetailsField.FAILED_STAGE_NAME.dbapiName(), failedStageName);
            }
            if (failedStageIndex != null) {
                serialized.set(EntityHttpConnectionTestResultDetailsField.FAILED_STAGE_INDEX.dbapiName(), failedStageIndex);
            }
            if (failedPart != null) {
                serialized.set(EntityHttpConnectionTestResultDetailsField.FAILED_PART.dbapiName(), failedPart.toString());
            }
            if (errorMessage != null) {
                serialized.set(EntityHttpConnectionTestResultDetailsField.ERROR_MESSAGE.dbapiName(), errorMessage);
            }
            if (statusCode != null) {
                serialized.set(EntityHttpConnectionTestResultDetailsField.STATUS_CODE.dbapiName(), statusCode);
            }
            if (variablesSerialized != null) {
                serialized.set(EntityHttpConnectionTestResultDetailsField.VARIABLES.dbapiName(), variablesSerialized);
            }
            if (body != null) {
                serialized.set(EntityHttpConnectionTestResultDetailsField.BODY.dbapiName(), body);
            }
            if (cpeItems != null) {
                serialized.set(EntityHttpConnectionTestResultDetailsField.CPE_ITEMS.dbapiName(), cpeItems);
            }

            return serialized;
        }

        public static Details fromScannerJson(Json json) {
            Details details = new Details();
            if (json.has(EntityHttpConnectionTestResultDetailsField.FAILED_STAGE_UUID.scanName()) && json.get(EntityHttpConnectionTestResultDetailsField.FAILED_STAGE_UUID.scanName()).isString()) {
                details.failedStageUuid = json.get(EntityHttpConnectionTestResultDetailsField.FAILED_STAGE_UUID.scanName()).asString();
            }
            if (json.has(EntityHttpConnectionTestResultDetailsField.FAILED_STAGE_NAME.scanName()) && json.get(EntityHttpConnectionTestResultDetailsField.FAILED_STAGE_NAME.scanName()).isString()) {
                details.failedStageName = json.get(EntityHttpConnectionTestResultDetailsField.FAILED_STAGE_NAME.scanName()).asString();
            }
            if (json.has(EntityHttpConnectionTestResultDetailsField.FAILED_STAGE_INDEX.scanName()) && json.get(EntityHttpConnectionTestResultDetailsField.FAILED_STAGE_INDEX.scanName()).isNumber()) {
                details.failedStageIndex = json.get(EntityHttpConnectionTestResultDetailsField.FAILED_STAGE_INDEX.scanName()).asInteger();
            }
            if (json.has(EntityHttpConnectionTestResultDetailsField.FAILED_PART.scanName()) && json.get(EntityHttpConnectionTestResultDetailsField.FAILED_PART.scanName()).isString()) {
                details.failedPart = EntityHttpConnection.Part.valueOf(json.get(EntityHttpConnectionTestResultDetailsField.FAILED_PART.scanName()).asString());
            }
            if (json.has(EntityHttpConnectionTestResultDetailsField.ERROR_MESSAGE.scanName()) && json.get(EntityHttpConnectionTestResultDetailsField.ERROR_MESSAGE.scanName()).isString()) {
                details.errorMessage = json.get(EntityHttpConnectionTestResultDetailsField.ERROR_MESSAGE.scanName()).asString();
            }
            if (json.has(EntityHttpConnectionTestResultDetailsField.STATUS_CODE.scanName()) && json.get(EntityHttpConnectionTestResultDetailsField.STATUS_CODE.scanName()).isNumber()) {
                details.statusCode = json.get(EntityHttpConnectionTestResultDetailsField.STATUS_CODE.scanName()).asInteger();
            }
            if (json.has(EntityHttpConnectionTestResultDetailsField.VARIABLES.scanName()) && json.get(EntityHttpConnectionTestResultDetailsField.VARIABLES.scanName()).isObject()) {
                Json variables = json.get(EntityHttpConnectionTestResultDetailsField.VARIABLES.scanName());
                if (variables.has("device") && variables.get("device").isObject()) {
                    details.deviceVariables = variables.get("device").asJsonMap().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> HttpApiVariable.fromScannerJson(e.getValue())));
                }
                if (variables.has("connection") && variables.get("connection").isObject()) {
                    details.connectionVariables = variables.get("connection").asJsonMap().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> HttpApiVariable.fromScannerJson(e.getValue())));
                }
                if (variables.has("customVariables") && variables.get("customVariables").isObject()) {
                    details.customVariables = variables.get("customVariables").asJsonMap().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> HttpApiVariable.fromScannerJson(e.getValue())));
                }
            }
            if (json.has(EntityHttpConnectionTestResultDetailsField.BODY.scanName()) && json.get(EntityHttpConnectionTestResultDetailsField.BODY.scanName()).isString()) {
                details.body = json.get(EntityHttpConnectionTestResultDetailsField.BODY.scanName()).asString();
            }
            if (json.has(EntityHttpConnectionTestResultDetailsField.CPE_ITEMS.scanName()) && json.get(EntityHttpConnectionTestResultDetailsField.CPE_ITEMS.scanName()).isString()) {
                details.cpeItems = json.get(EntityHttpConnectionTestResultDetailsField.CPE_ITEMS.scanName()).asString();
            }
            return details;
        }
    }
}
