package com.csl.intercom.dbapi.models;

import com.csl.intercom.cslscan.models.EntityHttpConnectionStage;
import com.csl.intercom.dbapi.enums.HttpConnectionField;
import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpConnection extends Connection {
    private final String entityHttpConnectionUuid;
    @Getter
    private String port;
    @Getter
    private final EntityHttpConnectionStage.HttpAuthenticationMethod authenticationMethod;
    @Getter
    private String username;
    @Getter
    private String password;
    private final String realm;
    @Getter
    private String token;
    @Getter
    private final Map<Integer, StageConfig> stagesConfig;
    @Getter
    private Map<String, String> inputs;

    public HttpConnection(String id,
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
        this.inputs = inputs;
        this.stagesConfig = stagesConfig;
    }
    public HttpConnection(String name, String id,
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
        super(name, id, devices, StaticConnectionProtocol.HTTP, isSimulated);
        this.entityHttpConnectionUuid = entityHttpConnectionUuid;
        this.port = port;
        this.authenticationMethod = authenticationMethod;
        this.username = username;
        this.password = password;
        this.realm = realm;
        this.token = token;
        this.inputs = inputs;
        this.stagesConfig = stagesConfig;
    }

    /**
     * Parse the JSON serialization received from DB-API. This method requires the connection protocol to find the right template.
     *
     * @param connectionJson The serialized connection as handed by DB-API.
     * @param protocol The connection protocol corresponding to this connection.
     * @return An instance of {@link HttpConnection} if the parsing was successful, null otherwise.
     */
    public static HttpConnection fromJson(Json connectionJson, ConnectionProtocol protocol) {
        try {
            String uuid;
            if (connectionJson.has("uuid")) {
                uuid = connectionJson.get("uuid").asString();
            } else {
                if(connectionJson.has("mongo_entity_id") && !connectionJson.get("mongo_entity_id").isNull())
                    uuid = connectionJson.get("mongo_entity_id").asString();
                else
                    uuid = null;
            }
            String port;
            if (connectionJson.has(HttpConnectionField.PORT.dbapiName()) && connectionJson.get(HttpConnectionField.PORT.dbapiName()).isString()) {
                port = connectionJson.get(HttpConnectionField.PORT.dbapiName()).asString();
            } else if (connectionJson.has(HttpConnectionField.PORT.dbapiName()) && connectionJson.get(HttpConnectionField.PORT.dbapiName()).isNumber()) {
                port = String.valueOf(connectionJson.get(HttpConnectionField.PORT.dbapiName()).asInteger());
            } else {
                port = null;
            }
            String username = null;
            try {
                username = JsonUtil.getStringFromJson(connectionJson, HttpConnectionField.USERNAME.dbapiName(), null);
            } catch (UnsupportedOperationException ignored) {
            }
            String password = null;
            try {
                password = JsonUtil.getStringFromJson(connectionJson, HttpConnectionField.PASSWORD.dbapiName(), null);
            } catch (UnsupportedOperationException ignored) {
            }

            Json otherData = connectionJson.get("other_data");
            Json readOnlyOtherData = connectionJson.get("read_only_other_data");
            if(otherData == null && readOnlyOtherData != null) {
                otherData = readOnlyOtherData;
            }
            EntityHttpConnectionStage.HttpAuthenticationMethod authenticationMethod = null;
            assert otherData != null;
            if (otherData.has(HttpConnectionField.AUTHENTICATION_METHOD.dbapiName())) {
                authenticationMethod = EntityHttpConnectionStage.HttpAuthenticationMethod.valueOf(otherData.get(HttpConnectionField.AUTHENTICATION_METHOD.dbapiName()).asString());
            }
            String entityHttpConnectionUuid = protocol.getConnectionTemplateId();
            String token = JsonUtil.getStringFromJson(otherData, HttpConnectionField.TOKEN.dbapiName(), null);
            String realm = JsonUtil.getStringFromJson(otherData, HttpConnectionField.REALM.dbapiName(), null);
            boolean isSimulated = false;
            if (connectionJson.has("is_simulated") && !connectionJson.get("is_simulated").isNull()) {
                isSimulated = connectionJson.get("is_simulated").asBoolean();
            }
            Map<Integer, StageConfig> stagesConfig = new HashMap<>();
            otherData.get(HttpConnectionField.STAGES_CONFIG.dbapiName()).asJsonMap().forEach((key, value) -> stagesConfig.put(Integer.parseInt(key), StageConfig.fromJson(value)));

            List<String> devices = connectionJson.get("connected_devices").asJsonList().stream()
                    .map(Json::asString)
                    .collect(java.util.stream.Collectors.toList());

            Map<String, String> inputs = new HashMap<>();
            // otherData.has("inputs") is like this forexample : {"cameraUsername":{"value":"service","is_secret":false},"camerPassword":{"value":"agd-fb3-M13-aqh","is_secret":true}}
            // so we need to parse it as a map and get the value of the key then put it in the inputs map
            // check if otherData has inputs key
            if (otherData.has("inputs")) {
                for (String key : otherData.get("inputs").asJsonMap().keySet()) {
                    inputs.put(key, otherData.get("inputs").get(key).get("value").asString());
                }
            }

            String name = "";
            if (connectionJson.has("name")) {
                name = connectionJson.get("name").asString();
            }

            return new HttpConnection(name, uuid, port, devices, entityHttpConnectionUuid, authenticationMethod, username, password, realm, token, stagesConfig, isSimulated, inputs);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static HttpConnection fromScannerJson(Json connectionJson) {
        try {
            String uuid = null;
            if (connectionJson.has("uuid") && connectionJson.get("uuid").isString()) {
                uuid = connectionJson.get("uuid").asString();
            }
            String port = connectionJson.get(HttpConnectionField.PORT.scanName()).asString();
            String entityHttpConnectionUuid = connectionJson.get(HttpConnectionField.ENTITY_HTTP_CONNECTION_ID.scanName()).asString();
            EntityHttpConnectionStage.HttpAuthenticationMethod authenticationMethod = EntityHttpConnectionStage.HttpAuthenticationMethod.valueOf(connectionJson.get(HttpConnectionField.AUTHENTICATION_METHOD.scanName()).asString());
            String username = connectionJson.get(HttpConnectionField.USERNAME.scanName()).asString();
            String password = connectionJson.get(HttpConnectionField.PASSWORD.scanName()).asString();
            String realm = connectionJson.get(HttpConnectionField.REALM.scanName()).asString();
            String token = connectionJson.get(HttpConnectionField.TOKEN.scanName()).asString();

            Map<String, String> inputs = new HashMap<>();
            if (connectionJson.has("inputs")) {
                connectionJson.get("inputs").asJsonMap().forEach((key, value) -> inputs.put(key, value.asString()));
            }

            Map<Integer, StageConfig> stagesConfig = new HashMap<>();
            connectionJson.get(HttpConnectionField.STAGES_CONFIG.scanName()).asJsonMap().forEach((key, value) -> stagesConfig.put(Integer.parseInt(key), StageConfig.fromJson(value)));

            return new HttpConnection(uuid, port, null, entityHttpConnectionUuid, authenticationMethod, username, password, realm, token, stagesConfig, false, inputs);
        } catch (NullPointerException e) {
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

    public HttpConnection setPort(String port) {
        this.port = port;
        return this;
    }

    public HttpConnection setUsername(String username) {
        this.username = username;
        return this;
    }

    public HttpConnection setPassword(String password) {
        this.password = password;
        return this;
    }

    public HttpConnection setToken(String token) {
        this.token = token;
        return this;
    }

    public HttpConnection setInputs(Map<String, String> inputs) {
        this.inputs = inputs;
        return this;
    }

    public static class StageConfig {
        @Getter
        private Integer port = null;

        private Map<String, String> inputs = null;

        public Json serializeForScanner() {
            Json serialized = Json.object();

            if (this.inputs != null) {
                Json serializedInputs = Json.object();
                this.inputs.forEach(serializedInputs::set);
                serialized.set(HttpConnectionField.INPUTS.scanName(), serializedInputs);
            }

            serialized.set(HttpConnectionField.PORT.scanName(), this.port);
            return serialized;
        }

        public static StageConfig fromJson(Json json) {
            StageConfig stageConfig = new StageConfig();
            if (json.has(HttpConnectionField.PORT.dbapiName()) && json.get(HttpConnectionField.PORT.dbapiName()).isNumber()) {
                stageConfig.port = json.get(HttpConnectionField.PORT.dbapiName()).asInteger();
            }

            stageConfig.inputs = new HashMap<>();
            if (json.has(HttpConnectionField.INPUTS.dbapiName())) {
                json.get(HttpConnectionField.INPUTS.dbapiName()).asJsonMap().forEach((key, value) -> stageConfig.inputs.put(key, value.asString()));
            }

            return stageConfig;
        }

    }
}
