package com.csl.intercom.dbapi.models;

import com.csl.intercom.cslscan.models.EntityHttpConnectionStage;
import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.csl.intercom.dbapi.enums.HttpConnectionField;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpConnection extends Connection {
    private String entityHttpConnectionUuid;
    private String port;
    private EntityHttpConnectionStage.HttpAuthenticationMethod authenticationMethod;
    private String username;
    private String password;
    private String realm;
    private String token;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Map<Integer, StageConfig> stagesConfig;
    private Map<String, String> inputs;

    public HttpConnection(int id,
                          String port,
                          List<String> devices,
                          String entityHttpConnectionUuid,
                          EntityHttpConnectionStage.HttpAuthenticationMethod authenticationMethod,
                          String username,
                          String password,
                          String realm,
                          String token,
                          Map<String, String> headers,
                          Map<String, String> queryParams,
                          Map<Integer, StageConfig> stagesConfig,
                          Boolean isSimulated,
                          Map<String, String> inputs) {
        super(id, devices, StaticConnectionProtocol.HTTP, isSimulated);
        this.entityHttpConnectionUuid = entityHttpConnectionUuid;
        this.port = port;
        this.authenticationMethod = authenticationMethod;
        this.username = username;
        this.password = password;
        this.realm = realm;
        this.token = token;
        this.headers = headers;
        this.queryParams = queryParams;
        this.stagesConfig = stagesConfig;
        this.inputs = inputs;
    }

    /**
     * Parse the JSON serialization received from DB-API. This method requires the connection protocol to find the right template.
     *
     * @param jsonConnection The serialized connection as handed by DB-API.
     * @param protocol The connection protocol corresponding to this connection.
     * @return An instance of {@link HttpConnection} if the parsing was successful, null otherwise.
     */
    public static HttpConnection fromJson(Json jsonConnection, ConnectionProtocol protocol) {
        try {
            int id = jsonConnection.get("id").asInteger();
            String port;
            if (jsonConnection.has(HttpConnectionField.PORT.dbapiName()) && jsonConnection.get(HttpConnectionField.PORT.dbapiName()).isString()) {
                port = jsonConnection.get(HttpConnectionField.PORT.dbapiName()).asString();
            } else if (jsonConnection.has(HttpConnectionField.PORT.dbapiName()) && jsonConnection.get(HttpConnectionField.PORT.dbapiName()).isNumber()) {
                port = String.valueOf(jsonConnection.get(HttpConnectionField.PORT.dbapiName()).asInteger());
            } else {
                port = null;
            }
            String username = null;
            try {
                username = JsonUtil.getStringFromJson(jsonConnection, HttpConnectionField.USERNAME.dbapiName(), null);
            } catch (UnsupportedOperationException ignored) {
            }
            String password = null;
            try {
                password = JsonUtil.getStringFromJson(jsonConnection, HttpConnectionField.PASSWORD.dbapiName(), null);
            } catch (UnsupportedOperationException ignored) {
            }

            Json otherData = jsonConnection.get("read_only_other_data");
            EntityHttpConnectionStage.HttpAuthenticationMethod authenticationMethod = null;
            if (otherData.has(HttpConnectionField.AUTHENTICATION_METHOD.dbapiName())) {
                authenticationMethod = EntityHttpConnectionStage.HttpAuthenticationMethod.valueOf(otherData.get(HttpConnectionField.AUTHENTICATION_METHOD.dbapiName()).asString());
            }
            String entityHttpConnectionUuid = protocol.getConnectionTemplateId();
            String token = JsonUtil.getStringFromJson(otherData, HttpConnectionField.TOKEN.dbapiName(), null);
            String realm = JsonUtil.getStringFromJson(otherData, HttpConnectionField.REALM.dbapiName(), null);
            Boolean isSimulated = jsonConnection.get("is_simulated").asBoolean();

            Map<String, String> headers = new HashMap<>();
            otherData.get(HttpConnectionField.HEADERS.dbapiName()).asJsonMap().forEach((key, value) -> headers.put(key, value.asString()));

            Map<String, String> queryParams = new HashMap<>();
            otherData.get(HttpConnectionField.QUERY_PARAMS.dbapiName()).asJsonMap().forEach((key, value) -> queryParams.put(key, value.asString()));

            Map<Integer, StageConfig> stagesConfig = new HashMap<>();
            otherData.get(HttpConnectionField.STAGES_CONFIG.dbapiName()).asJsonMap().forEach((key, value) -> stagesConfig.put(Integer.parseInt(key), StageConfig.fromJson(value)));

            List<String> devices = jsonConnection.get("connected_devices").asJsonList().stream()
                    .map(Json::asString)
                    .collect(java.util.stream.Collectors.toList());

            Map<String, String> inputs = new HashMap<>();
            if (otherData.has("inputs")) {
                otherData.get("inputs").asJsonMap().forEach((key, value) -> inputs.put(key, value.asString()));
            }

            return new HttpConnection(id, port, devices, entityHttpConnectionUuid, authenticationMethod, username, password, realm, token, headers, queryParams, stagesConfig, isSimulated, inputs);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    @Override
    public Json serializeForScanner() {
        Json stagesConfigSerialized = Json.object();
        if (this.stagesConfig != null) {
            this.stagesConfig.entrySet().forEach(entry -> stagesConfigSerialized.set(entry.getKey().toString(), entry.getValue().serializeForScanner()));
        }

        Json result = super.serializeForScanner();
        result.set("uuid", this.getId());
        result.set(HttpConnectionField.PORT.scanName(), this.port);
        if (this.entityHttpConnectionUuid != null) {
            result.set(HttpConnectionField.ENTITY_HTTP_CONNECTION_ID.scanName(), this.entityHttpConnectionUuid);
        }
        if (this.authenticationMethod != null) {
            result.set(HttpConnectionField.AUTHENTICATION_METHOD.scanName(), this.authenticationMethod.name());
        }
        if (this.username != null) {
            result.set(HttpConnectionField.USERNAME.scanName(), this.username);
        }
        if (this.password != null) {
            result.set(HttpConnectionField.PASSWORD.scanName(), this.password);
        }
        if (this.realm != null) {
            result.set(HttpConnectionField.REALM.scanName(), this.realm);
        }
        if (this.token != null) {
            result.set(HttpConnectionField.TOKEN.scanName(), this.token);
        }
        if (this.headers != null) {
            result.set(HttpConnectionField.HEADERS.scanName(), this.headers);
        }
        if (this.queryParams != null) {
            result.set(HttpConnectionField.QUERY_PARAMS.scanName(), this.queryParams);
        }
        if (this.inputs != null) {
            result.set("inputs", this.inputs);
        }
        result.set(HttpConnectionField.STAGES_CONFIG.scanName(), stagesConfigSerialized);

        return result;
    }

    public String getEntityHttpConnectionUuid() {
        return entityHttpConnectionUuid;
    }

    public String getPort() {
        return port;
    }

    public HttpConnection setPort(String port) {
        this.port = port;
        return this;
    }

    public EntityHttpConnectionStage.HttpAuthenticationMethod getAuthenticationMethod() {
        return authenticationMethod;
    }

    public String getUsername() {
        return username;
    }

    public HttpConnection setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public HttpConnection setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getToken() {
        return token;
    }

    public HttpConnection setToken(String token) {
        this.token = token;
        return this;
    }

    public Map<Integer, StageConfig> getStagesConfig() {
        return stagesConfig;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public HttpConnection setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public HttpConnection setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
        return this;
    }

    public static class StageConfig {
        private Integer port = null;
        private EntityHttpConnectionStage.HttpAuthenticationMethod authMethod = null;
        private String username = null;
        private String password = null;
        private String token = null;
        private String realm = null;
        private Map<String, String> headers = null;
        private Map<String, String> queryParams = null;
        private Map<String, String> inputs = null;

        public Json serializeForScanner() {
            Json serialized = Json.object();

            if (this.headers != null) {
                Json serializedHeaders = Json.object();
                this.headers.forEach(serializedHeaders::set);
                serialized.set(HttpConnectionField.HEADERS.scanName(), serializedHeaders);
            }
            if (this.queryParams != null) {
                Json serializedQueryParams = Json.object();
                this.queryParams.forEach(serializedQueryParams::set);
                serialized.set(HttpConnectionField.QUERY_PARAMS.scanName(), serializedQueryParams);
            }
            if (this.inputs != null) {
                Json serializedInputs = Json.object();
                this.inputs.forEach(serializedInputs::set);
                serialized.set(HttpConnectionField.INPUTS.scanName(), serializedInputs);
            }

            serialized.set(HttpConnectionField.PORT.scanName(), this.port);
            serialized.set(HttpConnectionField.AUTHENTICATION_METHOD.scanName(), this.authMethod.name());
            serialized.set(HttpConnectionField.USERNAME.scanName(), this.username);
            serialized.set(HttpConnectionField.PASSWORD.scanName(), this.password);
            serialized.set(HttpConnectionField.TOKEN.scanName(), this.token);
            serialized.set(HttpConnectionField.REALM.scanName(), this.realm);

            return serialized;
        }

        public static StageConfig fromJson(Json json) {
            StageConfig stageConfig = new StageConfig();
            if (json.has(HttpConnectionField.PORT.dbapiName()) && json.get(HttpConnectionField.PORT.dbapiName()).isNumber()) {
                stageConfig.port = json.get(HttpConnectionField.PORT.dbapiName()).asInteger();
            }
            stageConfig.authMethod = EntityHttpConnectionStage.HttpAuthenticationMethod.valueOf(JsonUtil.getStringFromJson(json, HttpConnectionField.AUTHENTICATION_METHOD.dbapiName(), EntityHttpConnectionStage.HttpAuthenticationMethod.NONE.name()));
            stageConfig.username = JsonUtil.getStringFromJson(json, HttpConnectionField.USERNAME.dbapiName(), null);
            stageConfig.password = JsonUtil.getStringFromJson(json, HttpConnectionField.PASSWORD.dbapiName(), null);
            stageConfig.token = JsonUtil.getStringFromJson(json, HttpConnectionField.TOKEN.dbapiName(), null);
            stageConfig.realm = JsonUtil.getStringFromJson(json, HttpConnectionField.REALM.dbapiName(), null);

            stageConfig.headers = new HashMap<>();
            if (json.has(HttpConnectionField.HEADERS.dbapiName())) {
                json.get(HttpConnectionField.HEADERS.dbapiName()).asJsonMap().forEach((key, value) -> stageConfig.headers.put(key, value.asString()));
            }

            stageConfig.queryParams = new HashMap<>();
            if (json.has(HttpConnectionField.QUERY_PARAMS.dbapiName())) {
                json.get(HttpConnectionField.QUERY_PARAMS.dbapiName()).asJsonMap().forEach((key, value) -> stageConfig.queryParams.put(key, value.asString()));
            }

            stageConfig.inputs = new HashMap<>();
            if (json.has(HttpConnectionField.INPUTS.dbapiName())) {
                json.get(HttpConnectionField.INPUTS.dbapiName()).asJsonMap().forEach((key, value) -> stageConfig.inputs.put(key, value.asString()));
            }

            return stageConfig;
        }

        public Integer getPort() {
            return port;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getToken() {
            return token;
        }
    }
}
