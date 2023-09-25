package com.csl.intercom.dbapi.models;

import com.csl.intercom.cslscan.models.EntityHttpConnectionStage;
import com.csl.intercom.dbapi.enums.ConnectionProtocol;
import com.csl.intercom.dbapi.enums.HttpConnectionField;
import com.mongodb.util.JSON;
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

    protected HttpConnection(int id, int port, List<String> devices, String entityHttpConnectionUuid, EntityHttpConnectionStage.HttpAuthenticationMethod authenticationMethod, String username, String password, String token, Map<String, String> headers, Map<String, String> queryParams, Map<Integer, StageConfig> stagesConfig, Boolean isSimulated) {
        super(id, devices, ConnectionProtocol.HTTP, isSimulated);
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

    public static HttpConnection fromJson(Json jsonConnection) {
        try {
            int id = jsonConnection.get("id").asInteger();
            int port = jsonConnection.get(HttpConnectionField.PORT.dbapiName()).asInteger();
            String username = JsonUtil.getStringFromJson(jsonConnection, HttpConnectionField.USERNAME.dbapiName(), null);
            String password = JsonUtil.getStringFromJson(jsonConnection, HttpConnectionField.PASSWORD.dbapiName(), null);

            Json otherData = jsonConnection.get("read_only_other_data");
            EntityHttpConnectionStage.HttpAuthenticationMethod authenticationMethod = EntityHttpConnectionStage.HttpAuthenticationMethod.valueOf(otherData.get(HttpConnectionField.AUTHENTICATION_METHOD.dbapiName()).asString());
            String entityHttpConnectionUuid = otherData.get(HttpConnectionField.ENTITY_HTTP_CONNECTION_ID.dbapiName()).asString();
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
        this.stagesConfig.entrySet().forEach(entry -> stagesConfigSerialized.set(entry.getKey().toString(), entry.getValue().serializeForScanner()));

        Json result = super.serializeForScanner();
        result.set(HttpConnectionField.ENTITY_HTTP_CONNECTION_ID.scanName(), entityHttpConnectionUuid);
        result.set(HttpConnectionField.AUTHENTICATION_METHOD.scanName(), authenticationMethod.name());
        result.set(HttpConnectionField.USERNAME.scanName(), username);
        result.set(HttpConnectionField.PASSWORD.scanName(), password);
        result.set(HttpConnectionField.TOKEN.scanName(), token);
        result.set(HttpConnectionField.HEADERS.scanName(), headers);
        result.set(HttpConnectionField.QUERY_PARAMS.scanName(), queryParams);
        result.set(HttpConnectionField.STAGES_CONFIG.scanName(), stagesConfigSerialized);

        return result;
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
            stageConfig.port = json.get(HttpConnectionField.PORT.dbapiName()).asInteger();
            stageConfig.authMethod = EntityHttpConnectionStage.HttpAuthenticationMethod.valueOf(json.get(HttpConnectionField.AUTHENTICATION_METHOD.dbapiName()).asString());
            stageConfig.username = json.get(HttpConnectionField.USERNAME.dbapiName()).asString();
            stageConfig.password = json.get(HttpConnectionField.PASSWORD.dbapiName()).asString();
            stageConfig.token = json.get(HttpConnectionField.TOKEN.dbapiName()).asString();

            stageConfig.headers = new HashMap<>();
            json.get(HttpConnectionField.HEADERS.dbapiName()).asJsonMap().forEach((key, value) -> stageConfig.headers.put(key, value.asString()));

            stageConfig.queryParams = new HashMap<>();
            json.get(HttpConnectionField.QUERY_PARAMS.dbapiName()).asJsonMap().forEach((key, value) -> stageConfig.queryParams.put(key, value.asString()));

            return stageConfig;
        }
    }
}
