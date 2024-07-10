package com.csl.intercom.dbapi;

import com.csl.core.CSLContext;
import com.csl.intercom.cslscan.ApiHandler;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage HTTP communications with DB-API. It offers a general interface to reach the db.
 */
public class DbapiHandler extends ApiHandler {
    private static final Logger logger = LoggerFactory.getLogger(DbapiHandler.class);

    public DbapiHandler() {
        this(CSLContext.instance.getConfig());
    }

    public DbapiHandler(Json config) {
        super("DB-API",
                (JsonUtil.getBooleanFromJson(config.get("global"), "use_ssl", true) ? "https://" : "http://")+
                        JsonUtil.getStringFromJson(config.get("global"), "ip_server_remote", "localhost")+
                        "/api");
        setApiKey(JsonUtil.getStringFromJson(config.get("global"), "api_key", ""));
    }

    public DbapiHandler(String moduleName) {
        this(moduleName, CSLContext.instance.getConfig());
    }

    public DbapiHandler(String moduleName, Json config) {
        super("DB-API::"+moduleName,
                (JsonUtil.getBooleanFromJson(config.get("global"), "use_ssl", true) ? "https://" : "http://")+
                        JsonUtil.getStringFromJson(config.get("global"), "ip_server_remote", "localhost")+
                        "/api");
        setApiKey(JsonUtil.getStringFromJson(config.get("global"), "api_key", ""));
    }
}
