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
    private int port;
    private EntityHttpConnectionStage.HttpAuthenticationMethod authenticationMethod;
    private String username;
    private String password;
    private String token;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Map<Integer, StageConfig> stagesConfig;

    public HttpConnection(int id, int port, List<String> devices, String entityHttpConnectionUuid, EntityHttpConnectionStage.HttpAuthenticationMethod authenticationMethod, String username, String password, String token, Map<String, String> headers, Map<String, String> queryParams, Map<Integer, StageConfig> stagesConfig, Boolean isSimulated) {
        super(id, devices, StaticConnectionProtocol.HTTP, isSimulated);
        this.entityHttpConnectionUuid = entityHttpConnectionUuid;
        this.port = port;
        this.authenticationMethod = authenticationMethod;
        this.username = username;
        this.password = password;
        this.token = token;
        this.headers = headers;
        this.queryParams = queryParams;
        this.stagesConfig = stagesConfig;
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
            int port = jsonConnection.get(HttpConnectionField.PORT.dbapiName()).asInteger();
            String username = JsonUtil.getStringFromJson(jsonConnection, HttpConnectionField.USERNAME.dbapiName(), null);
            String password = JsonUtil.getStringFromJson(jsonConnection, HttpConnectionField.PASSWORD.dbapiName(), null);

            Json otherData = jsonConnection.get("read_only_other_data");
            EntityHttpConnectionStage.HttpAuthenticationMethod authenticationMethod = EntityHttpConnectionStage.HttpAuthenticationMethod.valueOf(otherData.get(HttpConnectionField.AUTHENTICATION_METHOD.dbapiName()).asString());
            String entityHttpConnectionUuid = protocol.getConnectionTemplateId();
            String token = JsonUtil.getStringFromJson(otherData, HttpConnectionField.TOKEN.dbapiName(), null);
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

            return new HttpConnection(id, port, devices, entityHttpConnectionUuid, authenticationMethod, username, password, token, headers, queryParams, stagesConfig, isSimulated);
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
        if (this.token != null) {
            result.set(HttpConnectionField.TOKEN.scanName(), this.token);
        }
        if (this.headers != null) {
            result.set(HttpConnectionField.HEADERS.scanName(), this.headers);
        }
        if (this.queryParams != null) {
            result.set(HttpConnectionField.QUERY_PARAMS.scanName(), this.queryParams);
        }
        result.set(HttpConnectionField.STAGES_CONFIG.scanName(), stagesConfigSerialized);

        return result;
    }

    public String getEntityHttpConnectionUuid() {
        return entityHttpConnectionUuid;
    }

    public int getPort() {
        return port;
    }

    public HttpConnection setPort(int port) {
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
        private int port = 0;
        private EntityHttpConnectionStage.HttpAuthenticationMethod authMethod = null;
        private String username = null;
        private String password = null;
        private String token = null;
        private Map<String, String> headers = null;
        private Map<String, String> queryParams = null;

        public Json serializeForScanner() {
            Json serializedHeaders = Json.object();
            this.headers.entrySet().forEach(entry -> serializedHeaders.set(entry.getKey(), entry.getValue()));
            Json serializedQueryParams = Json.object();
            this.queryParams.entrySet().forEach(entry -> serializedQueryParams.set(entry.getKey(), entry.getValue()));

            return Json.object(
                    HttpConnectionField.PORT.scanName(), this.port,
                    HttpConnectionField.AUTHENTICATION_METHOD.scanName(), this.authMethod.name(),
                    HttpConnectionField.USERNAME.scanName(), this.username,
                    HttpConnectionField.PASSWORD.scanName(), this.password,
                    HttpConnectionField.TOKEN.scanName(), this.token,
                    HttpConnectionField.HEADERS.scanName(), serializedHeaders,
                    HttpConnectionField.QUERY_PARAMS.scanName(), serializedQueryParams
            );
        }

        public static StageConfig fromJson(Json json) {
            StageConfig stageConfig = new StageConfig();
            stageConfig.port = JsonUtil.getIntFromJson(json, HttpConnectionField.PORT.dbapiName(), 0);
            stageConfig.authMethod = EntityHttpConnectionStage.HttpAuthenticationMethod.valueOf(JsonUtil.getStringFromJson(json, HttpConnectionField.AUTHENTICATION_METHOD.dbapiName(), EntityHttpConnectionStage.HttpAuthenticationMethod.NONE.name()));
            stageConfig.username = JsonUtil.getStringFromJson(json, HttpConnectionField.USERNAME.dbapiName(), null);
            stageConfig.password = JsonUtil.getStringFromJson(json, HttpConnectionField.PASSWORD.dbapiName(), null);
            stageConfig.token = JsonUtil.getStringFromJson(json, HttpConnectionField.TOKEN.dbapiName(), null);

            stageConfig.headers = new HashMap<>();
            if (json.has(HttpConnectionField.HEADERS.dbapiName())) {
                json.get(HttpConnectionField.HEADERS.dbapiName()).asJsonMap().forEach((key, value) -> stageConfig.headers.put(key, value.asString()));
            }

            stageConfig.queryParams = new HashMap<>();
            if (json.has(HttpConnectionField.QUERY_PARAMS.dbapiName())) {
                json.get(HttpConnectionField.QUERY_PARAMS.dbapiName()).asJsonMap().forEach((key, value) -> stageConfig.queryParams.put(key, value.asString()));
            }

            return stageConfig;
        }

        public int getPort() {
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
