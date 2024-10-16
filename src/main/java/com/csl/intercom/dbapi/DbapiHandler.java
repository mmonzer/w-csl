package com.csl.intercom.dbapi;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.web.ApiHandler;
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

    public DbapiHandler(String moduleName) {
        this(moduleName, CSLContext.instance.getConfig());
    }

    public DbapiHandler(Config config) {
        this("DB-API", config);
    }

    public DbapiHandler(String moduleName, Config config) {
        super("DB-API::"+moduleName,
                config.Client.getIpServerRemote(),
                config.Client.getUseSsl());
        addUriCommonPath("/api");
        setApiKey(config.Client.getApiKey());
    }
}
