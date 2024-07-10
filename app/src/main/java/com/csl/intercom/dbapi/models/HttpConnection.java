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

            Map<Integer, StageConfig> stagesConfig = new HashMap<>();
            otherData.get(HttpConnectionField.STAGES_CONFIG.dbapiName()).asJsonMap().forEach((key, value) -> stagesConfig.put(Integer.parseInt(key), StageConfig.fromJson(value)));

            List<String> devices = jsonConnection.get("connected_devices").asJsonList().stream()
                    .map(Json::asString)
                    .collect(java.util.stream.Collectors.toList());

            Map<String, String> inputs = new HashMap<>();
            if (otherData.has("inputs")) {
                otherData.get("inputs").asJsonMap().forEach((key, value) -> inputs.put(key, value.asString()));
            }

            return new HttpConnection(id, port, devices, entityHttpConnectionUuid, authenticationMethod, username, password, realm, token, stagesConfig, isSimulated, inputs);
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

    public Map<String, String> getInputs() {
        return inputs;
    }

    public HttpConnection setInputs(Map<String, String> inputs) {
        this.inputs = inputs;
        return this;
    }

    public Map<Integer, StageConfig> getStagesConfig() {
        return stagesConfig;
    }

    public static class StageConfig {
        private Integer port = null;
        private EntityHttpConnectionStage.HttpAuthenticationMethod authMethod = null;
        private String username = null;
        private String password = null;
        private String token = null;
        private String realm = null;
        private Map<String, String> inputs = null;

        public Json serializeForScanner() {
            Json serialized = Json.object();

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
            if (json.has(HttpConnectionField.USERNAME.dbapiName()) && json.get(HttpConnectionField.USERNAME.dbapiName()).isString()) {
                stageConfig.username = json.get(HttpConnectionField.USERNAME.dbapiName()).asString();
            }
            if (json.has(HttpConnectionField.PASSWORD.dbapiName()) && json.get(HttpConnectionField.PASSWORD.dbapiName()).isString()) {
                stageConfig.password = json.get(HttpConnectionField.PASSWORD.dbapiName()).asString();
            }
            if (json.has(HttpConnectionField.TOKEN.dbapiName()) && json.get(HttpConnectionField.TOKEN.dbapiName()).isString()) {
                stageConfig.token = json.get(HttpConnectionField.TOKEN.dbapiName()).asString();
            }
            if (json.has(HttpConnectionField.REALM.dbapiName()) && json.get(HttpConnectionField.REALM.dbapiName()).isString()) {
                stageConfig.realm = json.get(HttpConnectionField.REALM.dbapiName()).asString();
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
