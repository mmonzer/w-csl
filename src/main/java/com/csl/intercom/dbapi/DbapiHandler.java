package com.csl.intercom.dbapi;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.web.apiclient.ApiHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage HTTP communications with DB-API. It offers a general interface to reach the db.
 */
public class DbapiHandler extends ApiHandler {
    private static final Logger logger = LoggerFactory.getLogger(DbapiHandler.class);

    public DbapiHandler() {
        this(CSLContext.getInstance().getConfig());
    }

    public DbapiHandler(String moduleName) {
        this(moduleName, CSLContext.getInstance().getConfig());
    }

    public DbapiHandler(Config config) {
        this("DB-API", config);
    }

    public DbapiHandler(String moduleName, Config config) {
        super("DB-API::"+moduleName,
                config.client.getIpServerRemote(),
                config.client.getUseSsl());
        addUriCommonPath("/api");
        setApiKey(config.client.getApiKey());
    }
}
