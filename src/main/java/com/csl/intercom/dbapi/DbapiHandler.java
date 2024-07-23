package com.csl.intercom.dbapi;

import com.csl.core.CSLContext;
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
                (JsonUtil.getBooleanFromJson(config.get("global"), "use_ssl", true) ? "https://" : "http://")+
                        JsonUtil.getStringFromJson(config.get("global"), "ip_server_remote", "localhost")+
                        "/api");
        setApiKey(JsonUtil.getStringFromJson(config.get("global"), "api_key", ""));
    }

    @Override
    protected HttpClient initClient() {
       return initSSLDbApiHandler();
    }

    /**
     * Ensures the connexion with SSL with the Dbapi.
     * Return the new client well configured
     */
    private static HttpClient initSSLDbApiHandler(){
        // Retrieve system properties
        String trustStorePath = System.getProperty("javax.net.ssl.trustStore");
        String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");

        // Ensure the properties are set
        if (trustStorePath == null || trustStorePassword == null) {
            throw new IllegalStateException("Trust store properties are not set.");
        }

        // Configure SslContextFactory with the retrieved properties
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setTrustStorePath(trustStorePath);
        sslContextFactory.setTrustStorePassword(trustStorePassword);
        sslContextFactory.setTrustAll(true);

        return new HttpClient(sslContextFactory);
    }
}
