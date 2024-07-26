package com.csl.intercom.dbapi;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.intercom.cslscan.ApiHandler;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
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
        this("DB-API", config);
    }

    public DbapiHandler(String moduleName) {
        this(moduleName, CSLContext.instance.getConfig());
    }

    public DbapiHandler(String moduleName, Json config) {
        super("DB-API::"+moduleName,
                JsonUtil.getStringFromJson(config.get("global"), "ip_server_remote", "localhost"),
                JsonUtil.getBooleanFromJson(config.get("global"), "use_ssl", true));
        addUriCommonPath("/api");
        setApiKey(JsonUtil.getStringFromJson(config.get("global"), "api_key", ""));
    }

    public DbapiHandler(Config config) {
        this("DB-API", config);
    }

    public DbapiHandler(String moduleName, Config config) {
        super("DB-API::"+moduleName,
                config.Global.getIpServerRemote(),
                config.Global.getUseSsl());
        addUriCommonPath("/api");
        setApiKey(config.Global.getApiKey());
    }
}
