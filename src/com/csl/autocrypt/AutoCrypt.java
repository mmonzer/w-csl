package com.csl.autocrypt;

import com.csl.core.CSLContext;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.JsonApiResponse;

/**
 * API client of the module AutoCrypt
 */
public class AutoCrypt {
    private String moduleIp;
    private int modulePort;
    private String name;
    private String dbIp;
    private String dbApikey;
    private DbapiHandlerForCSLAutoCrypt dbApiHandler = null;
    private ApiHandlerForCSLAutoCrypt moduleApiHandler = null;
    private AutoCryptLogic logic = null;

    public AutoCrypt(String name) {
        this.name = name;
        Json config = CSLContext.instance.getConfig();
        Json localConfig = config.get("auto_crypt");
        moduleIp = JsonUtil.getStringFromJson(localConfig, "ip", "host.docker.internal");
        modulePort = JsonUtil.getIntFromJson(localConfig, "port", 8002);
        Json globalConfig = config.get("global");
        dbIp = JsonUtil.getStringFromJson(globalConfig, "ip_server_remote", "host.docker.internal");
        dbApikey = JsonUtil.getStringFromJson(globalConfig, "api_key", "");
    }

    /**
     * Get the API port
     */
    public int getModulePort() {
        return modulePort;
    }

    /**
     * Get the API ip
     */
    public String getModuleIp() {
        return moduleIp;
    }

    /**
     * Set the new API port
     *
     * @param newPort new port of the API
     */
    public void setModulePort(int newPort) {
        modulePort = newPort;
    }

    /**
     * Set the new API ip
     *
     * @param newIp new ip of the API
     */
    public void setModuleIp(String newIp) {
        moduleIp = newIp;
    }

    /**
     * Set the new configuration for connecting the DB (port par default)
     *
     * @param ip     ip of the DB
     * @param apikey authentication key for DB
     */
    public void configureDbApiConnection(String ip, String apikey) {
        this.dbIp = ip;
        this.dbApikey = apikey;
    }

    /**
     * Reinit the handler point
     */
    public void reinitApiHandler() {
        moduleApiHandler = new ApiHandlerForCSLAutoCrypt(name, "http://" + moduleIp + ":" + modulePort);
//        moduleApiHandler = new ApiHandlerForCSLAutoCrypt(name, AutoCryptUtils.generateAutoCryptApiUrlFromConfig(CSLContext.instance.getConfig().get("auto_crypt")));
//        NOTE: APIKey is used to authorize the client when sending requests to DBAPI, HTTPS is used when passing by NGINX
//        FIXME: DbapiHandlerForCSLAutoCrypt should get its own configuration from the global section without passing it here
//        FIXME: I think we only use https for the connection with DBAPI
        if (dbApikey.isEmpty()) {
            dbApiHandler = new DbapiHandlerForCSLAutoCrypt("BDAPI - " + name, "http://" + dbIp + "/api");
        } else {
            dbApiHandler = new DbapiHandlerForCSLAutoCrypt("BDAPI - " + name, "https://" + dbIp + "/api");
            dbApiHandler.setApiKey(dbApikey);
        }
        moduleApiHandler.addCleaner(CSLAutocryptUtils::cleanApiResponse);
        moduleApiHandler.addCleaner(CSLAutocryptUtils::cleanApiResponse);
        logic = new AutoCryptLogic(moduleApiHandler, dbApiHandler);
    }

    /**
     * Get the module API handler
     */
    public AutoCryptLogic getMethods() {
        return logic;
    }

    /**
     * Changes the saving to Db
     *
     * @param shouldSaveToDb
     */
    public void setSaveToDb(boolean shouldSaveToDb) {
        logic.setSaveToDb(shouldSaveToDb);
    }

}
