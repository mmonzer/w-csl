package com.csl.autocrypt;

import com.csl.intercom.cslscan.ApiHandler;

/**
 * API client of the module AutoCrypt
 */
public class AutoCrypt {
    private String moduleIp;
    private int modulePort;
    private String name;
    private String dbIp;
    private String dbApikey;
    private ApiHandler dbApiHandler = null;
    private ApiHandler moduleApiHandler = null;
    private AutoCryptLogic logic = null;

    public AutoCrypt(String name) {
        this.name = name;
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
     * @param ip ip of the DB
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
        moduleApiHandler = new ApiHandler(name, "http://" + moduleIp + ":" + modulePort);
        dbApiHandler = new ApiHandler("BDAPI - "+name, "https://" + dbIp + "/api");
        dbApiHandler.setApiKey(dbApikey);
        logic = new AutoCryptLogic(moduleApiHandler, dbApiHandler);
    }

    /**
     * Get the module API handler
     */
    public AutoCryptLogic getMethods() {
        return logic;
    }
}
