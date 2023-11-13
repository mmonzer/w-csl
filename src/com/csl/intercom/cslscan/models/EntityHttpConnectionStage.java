package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.enums.EntityHttpConnectionStageField;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.eclipse.jetty.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EntityHttpConnectionStage {
    private String name;
    private String ip_address;
    private String port;
    private String username;
    private String password;
    private String realm;
    private String token;
    private HttpProtocol protocol;
    private HttpMethod method;
    private HttpAuthenticationMethod authenticationMethod;
    private String path;
    private String contentType;
    private List<OptionalInputField<String, String>> headers;
    private List<OptionalInputField<String, String>> queryParams;
    private String body;
    private String jsParser;
    private boolean isEnabled;


    public Json serializeForScanner() {
        Json headersSerialized = this.headers.stream()
                .map(OptionalInputField::serializeForScanner)
                .collect(Json::array, Json::add, Json::add);

        Json queryParamsSerialized = this.queryParams.stream()
                .map(OptionalInputField::serializeForScanner)
                .collect(Json::array, Json::add, Json::add);

        return Json.object(
                EntityHttpConnectionStageField.NAME.scanName(), this.name,
                EntityHttpConnectionStageField.IP_ADDRESS.scanName(), this.ip_address,
                EntityHttpConnectionStageField.PORT.scanName(), this.port,
                EntityHttpConnectionStageField.USERNAME.scanName(), this.username,
                EntityHttpConnectionStageField.PASSWORD.scanName(), this.password,
                EntityHttpConnectionStageField.REALM.scanName(), this.realm,
                EntityHttpConnectionStageField.TOKEN.scanName(), this.token,
                EntityHttpConnectionStageField.PROTOCOL.scanName(), this.protocol.name(),
                EntityHttpConnectionStageField.METHOD.scanName(), this.method.name(),
                EntityHttpConnectionStageField.AUTHENTICATION_METHOD.scanName(), this.authenticationMethod.name(),
                EntityHttpConnectionStageField.PATH.scanName(), this.path,
                EntityHttpConnectionStageField.CONTENT_TYPE.scanName(), this.contentType,
                EntityHttpConnectionStageField.HEADERS.scanName(), headersSerialized,
                EntityHttpConnectionStageField.QUERY_PARAMS.scanName(), queryParamsSerialized,
                EntityHttpConnectionStageField.BODY.scanName(), this.body,
                EntityHttpConnectionStageField.JS_PARSER.scanName(), this.jsParser,
                EntityHttpConnectionStageField.ENABLED.scanName(), this.isEnabled
        );
    }

    public Json serializeForDbapi() {
        Json headersSerialized = this.headers.stream()
                .map(OptionalInputField::serializeForDbapi)
                .collect(Json::array, Json::add, Json::add);

        Json queryParamsSerialized = this.queryParams.stream()
                .map(OptionalInputField::serializeForDbapi)
                .collect(Json::array, Json::add, Json::add);

        return Json.object(
                EntityHttpConnectionStageField.NAME.dbapiName(), this.name,
                EntityHttpConnectionStageField.IP_ADDRESS.dbapiName(), this.ip_address,
                EntityHttpConnectionStageField.PORT.dbapiName(), this.port,
                EntityHttpConnectionStageField.USERNAME.dbapiName(), this.username,
                EntityHttpConnectionStageField.PASSWORD.dbapiName(), this.password,
                EntityHttpConnectionStageField.REALM.dbapiName(), this.realm,
                EntityHttpConnectionStageField.TOKEN.dbapiName(), this.token,
                EntityHttpConnectionStageField.PROTOCOL.dbapiName(), this.protocol.name(),
                EntityHttpConnectionStageField.METHOD.dbapiName(), this.method.name(),
                EntityHttpConnectionStageField.AUTHENTICATION_METHOD.dbapiName(), this.authenticationMethod.name(),
                EntityHttpConnectionStageField.PATH.dbapiName(), this.path,
                EntityHttpConnectionStageField.CONTENT_TYPE.dbapiName(), this.contentType,
                EntityHttpConnectionStageField.HEADERS.dbapiName(), headersSerialized,
                EntityHttpConnectionStageField.QUERY_PARAMS.dbapiName(), queryParamsSerialized,
                EntityHttpConnectionStageField.BODY.dbapiName(), this.body,
                EntityHttpConnectionStageField.JS_PARSER.dbapiName(), this.jsParser,
                EntityHttpConnectionStageField.ENABLED.dbapiName(), this.isEnabled
        );
    }

    /**
     * Build an EntityHttpConnectionStage from a json object as returned from DB-API
     *
     * @param json The json object as returned from DB-API
     * @return An EntityHttpConnectionStage instance
     */
    public static EntityHttpConnectionStage fromDbapiJson(Json json) {
        EntityHttpConnectionStage stage = new EntityHttpConnectionStage();
        stage.name = JsonUtil.getStringFromJson(json, EntityHttpConnectionStageField.NAME.dbapiName(), "");
        if (json.has(EntityHttpConnectionStageField.IP_ADDRESS.dbapiName()) && json.get(EntityHttpConnectionStageField.IP_ADDRESS.dbapiName()).isString()) {
            stage.ip_address = json.get(EntityHttpConnectionStageField.IP_ADDRESS.dbapiName()).asString();
        } else {
            stage.ip_address = "${device.ipAddress}";
        }

        if (json.has(EntityHttpConnectionStageField.PORT.dbapiName()) && json.get(EntityHttpConnectionStageField.PORT.dbapiName()).isString()) {
            stage.port = json.get(EntityHttpConnectionStageField.PORT.dbapiName()).asString();
        } else if (json.has(EntityHttpConnectionStageField.PORT.dbapiName()) && json.get(EntityHttpConnectionStageField.PORT.dbapiName()).isNumber()) {
            stage.port = json.get(EntityHttpConnectionStageField.PORT.dbapiName()).asInteger() + "";
        } else {
            stage.port = "${connection.port}";
        }

        if (json.has(EntityHttpConnectionStageField.USERNAME.dbapiName()) && json.get(EntityHttpConnectionStageField.USERNAME.dbapiName()).isString()) {
            stage.username = json.get(EntityHttpConnectionStageField.USERNAME.dbapiName()).asString();
        } else {
            stage.username = "${connection.username}";
        }

        if (json.has(EntityHttpConnectionStageField.PASSWORD.dbapiName()) && json.get(EntityHttpConnectionStageField.PASSWORD.dbapiName()).isString()) {
            stage.password = json.get(EntityHttpConnectionStageField.PASSWORD.dbapiName()).asString();
        } else {
            stage.password = "${connection.password}";
        }

        if (json.has(EntityHttpConnectionStageField.REALM.dbapiName()) && json.get(EntityHttpConnectionStageField.REALM.dbapiName()).isString()) {
            stage.realm = json.get(EntityHttpConnectionStageField.REALM.dbapiName()).asString();
        } else {
            stage.realm = "${connection.realm}";
        }

        if (json.has(EntityHttpConnectionStageField.TOKEN.dbapiName()) && json.get(EntityHttpConnectionStageField.TOKEN.dbapiName()).isString()) {
            stage.token = JsonUtil.getStringFromJson(json, EntityHttpConnectionStageField.TOKEN.dbapiName(), "${connection.token}");
        } else {
            stage.token = "${connection.token}";
        }

        stage.protocol = HttpProtocol.valueOf(JsonUtil.getStringFromJson(json, EntityHttpConnectionStageField.PROTOCOL.dbapiName(), "HTTP"));
        stage.method = HttpMethod.valueOf(JsonUtil.getStringFromJson(json, EntityHttpConnectionStageField.METHOD.dbapiName(), "GET"));
        stage.authenticationMethod = HttpAuthenticationMethod.valueOf(JsonUtil.getStringFromJson(json, EntityHttpConnectionStageField.AUTHENTICATION_METHOD.dbapiName(), "NONE"));
        stage.path = JsonUtil.getStringFromJson(json, EntityHttpConnectionStageField.PATH.dbapiName(), "");
        stage.contentType = JsonUtil.getStringFromJson(json, EntityHttpConnectionStageField.CONTENT_TYPE.dbapiName(), "");
        stage.body = JsonUtil.getStringFromJson(json, EntityHttpConnectionStageField.BODY.dbapiName(), "");
        stage.jsParser = JsonUtil.getStringFromJson(json, EntityHttpConnectionStageField.JS_PARSER.dbapiName(), "");
        stage.isEnabled = JsonUtil.getBooleanFromJson(json, EntityHttpConnectionStageField.ENABLED.dbapiName(), true);

        List<Json> headersJson = new ArrayList<>();
        if (json.has(EntityHttpConnectionStageField.HEADERS.dbapiName()) && json.get(EntityHttpConnectionStageField.HEADERS.dbapiName()).isArray()) {
            headersJson = json.get(EntityHttpConnectionStageField.HEADERS.dbapiName()).asJsonList();
        }
        stage.headers = headersJson.stream()
                .map(OptionalInputField::fromDbapiJson)
                .collect(Collectors.toList());

        List<Json> queryParamsJson = new ArrayList<>();
        if (json.has(EntityHttpConnectionStageField.QUERY_PARAMS.dbapiName()) && json.get(EntityHttpConnectionStageField.QUERY_PARAMS.dbapiName()).isArray()) {
            queryParamsJson = json.get(EntityHttpConnectionStageField.QUERY_PARAMS.dbapiName()).asJsonList();
        }
        stage.queryParams = queryParamsJson.stream()
                .map(OptionalInputField::fromDbapiJson)
                .collect(Collectors.toList());

        return stage;
    }

    /**
     * Build an EntityHttpConnectionStage from a json object as returned from the scanner
     *
     * @param json The json object as returned from the scanner
     * @return An EntityHttpConnectionStage instance
     */
    public static EntityHttpConnectionStage fromScannerJson(Json json) {
        EntityHttpConnectionStage stage = new EntityHttpConnectionStage();
        stage.name = json.get(EntityHttpConnectionStageField.NAME.scanName()).asString();
        stage.ip_address = json.get(EntityHttpConnectionStageField.IP_ADDRESS.scanName()).asString();
        stage.port = json.get(EntityHttpConnectionStageField.PORT.scanName()).asString();
        if (json.get(EntityHttpConnectionStageField.USERNAME.scanName()).isString() && !json.get(EntityHttpConnectionStageField.USERNAME.scanName()).asString().equals("")) {
            stage.username = json.get(EntityHttpConnectionStageField.USERNAME.scanName()).asString();
        }
        if (json.get(EntityHttpConnectionStageField.PASSWORD.scanName()).isString() && !json.get(EntityHttpConnectionStageField.PASSWORD.scanName()).asString().equals("")) {
            stage.password = json.get(EntityHttpConnectionStageField.PASSWORD.scanName()).asString();
        }
        if (json.get(EntityHttpConnectionStageField.REALM.scanName()).isString() && !json.get(EntityHttpConnectionStageField.REALM.scanName()).asString().equals("")) {
            stage.realm = json.get(EntityHttpConnectionStageField.REALM.scanName()).asString();
        }
        if (json.get(EntityHttpConnectionStageField.TOKEN.scanName()).isString() && !json.get(EntityHttpConnectionStageField.TOKEN.scanName()).asString().equals("")) {
            stage.token = json.get(EntityHttpConnectionStageField.TOKEN.scanName()).asString();
        }
        stage.protocol = HttpProtocol.valueOf(json.get(EntityHttpConnectionStageField.PROTOCOL.scanName()).asString());
        stage.method = HttpMethod.valueOf(json.get(EntityHttpConnectionStageField.METHOD.scanName()).asString());
        stage.authenticationMethod = HttpAuthenticationMethod.valueOf(json.get(EntityHttpConnectionStageField.AUTHENTICATION_METHOD.scanName()).asString());
        stage.path = json.get(EntityHttpConnectionStageField.PATH.scanName()).asString();
        stage.contentType = json.get(EntityHttpConnectionStageField.CONTENT_TYPE.scanName()).asString();
        stage.body = json.get(EntityHttpConnectionStageField.BODY.scanName()).asString();
        stage.jsParser = json.get(EntityHttpConnectionStageField.JS_PARSER.scanName()).asString();
        stage.isEnabled = json.get(EntityHttpConnectionStageField.ENABLED.scanName()).asBoolean();

        List<Json> headersJson = json.get(EntityHttpConnectionStageField.HEADERS.scanName()).asJsonList();
        stage.headers = headersJson.stream()
                .map(OptionalInputField::fromScannerJson)
                .collect(Collectors.toList());

        List<Json> queryParamsJson = json.get(EntityHttpConnectionStageField.QUERY_PARAMS.scanName()).asJsonList();
        stage.queryParams = queryParamsJson.stream()
                .map(OptionalInputField::fromScannerJson)
                .collect(Collectors.toList());

        return stage;
    }

    public enum HttpProtocol {
        HTTP,
        HTTPS
    }

    public enum HttpAuthenticationMethod {
        BASIC,
        DIGEST,
        TOKEN,
        NONE
    }

//    public enum HttpMethod {
//        GET,
//        POST,
//        PUT,
//        DELETE,
//        PATCH,
//        HEAD,
//        OPTIONS,
//        TRACE
//    }

    public static class OptionalInputField<K, V> {
        private K key;
        private V value;
        private boolean isOptional;
        private boolean isInput;
        private boolean isVisible;
        private boolean isEditable;

        public OptionalInputField(K key, V value, boolean isOptional, boolean isInput, boolean isVisible, boolean isEditable) {
            this.key = key;
            this.value = value;
            this.isOptional = isOptional;
            this.isInput = isInput;
            this.isVisible = isVisible;
            this.isEditable = isEditable;
        }

        public Json serializeForScanner() {
            return Json.object(
                    "key", key,
                    "value", value,
                    "isOptional", isOptional,
                    "isInput", isInput,
                    "isVisible", isVisible,
                    "isEditable", isEditable
            );
        }

        public Json serializeForDbapi() {
            return Json.object(
                    "key", key,
                    "value", value,
                    "isOptional", isOptional,
                    "isInput", isInput,
                    "isVisible", isVisible,
                    "isEditable", isEditable
            );
        }

        /**
         * Build an OptionalInputField from a json object as returned from DB-API
         *
         * @param json The json object as returned from DB-API
         * @return An OptionalInputField instance
         */
        public static OptionalInputField<String, String> fromDbapiJson(Json json) {
            Json keyJson = json.get("key");
            Json valueJson = json.get("value");
            Json isOptionalJson = json.get("isOptional");
            Json isInputJson = json.get("isInput");
            Json isVisibleJson = json.get("isVisible");
            Json isEditableJson = json.get("isEditable");
            return new OptionalInputField<>(
                    keyJson != null && keyJson.isString() ? keyJson.asString() : "",
                    valueJson != null && valueJson.isString() ? valueJson.asString() : "",
                    isOptionalJson != null && isOptionalJson.isBoolean() ? isOptionalJson.asBoolean() : false,
                    isInputJson != null && isInputJson.isBoolean() ? isInputJson.asBoolean() : false,
                    isVisibleJson != null && isVisibleJson.isBoolean() ? isVisibleJson.asBoolean() : true,
                    isEditableJson != null && isEditableJson.isBoolean() ? isEditableJson.asBoolean() : true
            );
        }

        /**
         * Build an OptionalInputField from a json object as returned from the scanner
         *
         * @param json The json object as returned from the scanner
         * @return An OptionalInputField instance
         */
        public static OptionalInputField<String, String> fromScannerJson(Json json) {
            return new OptionalInputField<>(
                    json.get("key").asString(),
                    json.get("value").asString(),
                    json.get("optional").asBoolean(),
                    json.get("input").asBoolean(),
                    json.get("visible").asBoolean(),
                    json.get("editable").asBoolean()
            );
        }
    }
}
