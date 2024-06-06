package com.csl.autocrypt;

import com.csl.intercom.cslscan.ApiHandler;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import main.services.endpoints.AutoCryptEndpoints;
import org.jetbrains.annotations.NotNull;

/**
 * API client of the module AutoCrypt
 */
public class AutoCrypt {
    private String moduleIp;
    private int modulePort;
    private ApiHandler moduleApiHandler = null;
    private String dbIp;
    private int dbPort;
    private String dbApikey;
    private ApiHandler dbApiHandler = null;

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
        moduleApiHandler = new ApiHandler("CSL-AutoCrypt", "http://" + moduleIp + ":" + modulePort);
        dbApiHandler = new ApiHandler("DB-AutoCrypt", "https://" + dbIp + "/api");
        dbApiHandler.setApiKey(this.dbApikey);
    }

    /**
     * Send cmd to the module in method POST
     *
     * @param endpoint endpoint to connect
     * @param body     body of the POST request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmdPost(String endpoint, String body) {
        JsonApiResponse response =  sendCmdPost(endpoint, Json.read(body));
        if (response.isSuccess()) {
            return response;
        } else {
            return reformatAutoCryptError(response);
        }
    }

    /**
     * Send cmd to the module in method POST
     *
     * @param endpoint endpoint to connect
     * @param body     body of the POST request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmdPost(String endpoint, Json body) {
        JsonApiResponse response =  moduleApiHandler.sendPost(endpoint, body);
        if (response.isSuccess()) {
            return response;
        } else {
            return reformatAutoCryptError(response);
        }
    }

    /**
     * Send cmd to the module in method POST
     *
     * @param endpoint endpoint to connect
     * @param body     body of the POST request
     * @param params   params of the POST request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmdPost(String endpoint, Json body, Json params) {
        JsonApiResponse response =  moduleApiHandler.sendPost(endpoint, params, body);
        if (response.isSuccess()) {
            return response;
        } else {
            return reformatAutoCryptError(response);
        }
    }

    /**
     * Send cmd to the module in method POST
     *
     * @param endpoint endpoint to connect
     * @param body     body of the POST request
     * @param params   params of the POST request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmdPostFile(String endpoint, Json body, Json params) {
        JsonApiResponse response =  moduleApiHandler.sendPostFile(endpoint, params, body);
        if (response.isSuccess()) {
            return response;
        } else {
            return reformatAutoCryptError(response);
        }
    }

    /**
     * Send cmd to the module in method GET
     *
     * @param endpoint endpoint to connect
     * @param params   params of the GET request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmdGet(String endpoint, String params) {
        return sendCmdGet(endpoint, Json.read(params));
    }

    /**
     * Send cmd to the module in method GET
     *
     * @param endpoint endpoint to connect
     * @param params   params of the GET request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmdGet(String endpoint, Json params) {
        JsonApiResponse response = moduleApiHandler.sendGet(endpoint, params);
        if (response.isSuccess()) {
            return response;
        } else {
            return reformatAutoCryptError(response);
        }
    }

    /**
     * Send cmd to the module in method DELETE
     *
     * @param endpoint endpoint to connect
     * @param params   params of the DELETE request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmdDelete(String endpoint, String params) {
        return sendCmdDelete(endpoint, Json.read(params));
    }

    /**
     * Send cmd to the module in method DELETE
     *
     * @param endpoint endpoint to connect
     * @param params   params of the DELETE request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmdDelete(String endpoint, Json params) {
        JsonApiResponse response =  moduleApiHandler.sendDelete(endpoint, params);
        if (response.isSuccess()) {
            return response;
        } else {
            return reformatAutoCryptError(response);
        }
    }

    /**
     * Send cmd to the module in method DELETE
     *
     * @param endpoint endpoint to connect
     * @param params   params of the DELETE request
     * @param body     body of the DELETE request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmdDelete(String endpoint, Json body, Json params) {
        JsonApiResponse response =  moduleApiHandler.sendDelete(endpoint, params, body);
        if (response.isSuccess()) {
            return response;
        } else {
            return reformatAutoCryptError(response);
        }
    }

    /**
     * Send cmd to the module in method PUT
     *
     * @param endpoint endpoint to connect
     * @param body     body of the PUT request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmdPut(String endpoint, String body) {
        return sendCmdPut(endpoint, Json.read(body));
    }

    /**
     * Send cmd to the module in method PUT
     *
     * @param endpoint endpoint to connect
     * @param body     body of the PUT request
     * @param params   params of the PUT request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmdPut(String endpoint, Json body, Json params) {
        JsonApiResponse response =  moduleApiHandler.sendPut(endpoint, params, body);
        if (response.isSuccess()) {
            return response;
        } else {
            return reformatAutoCryptError(response);
        }
    }

    /**
     * Send cmd to the module in method PUT
     *
     * @param endpoint endpoint to connect
     * @param body     body of the PUT request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmdPut(String endpoint, Json body) {
        JsonApiResponse response =  moduleApiHandler.sendPut(endpoint, body);
        if (response.isSuccess()) {
            return response;
        } else {
            return reformatAutoCryptError(response);
        }
    }

    /**
     * Verifies whether the http api is reachable or not
     *
     * @return if the http api is reachable
     */
    public boolean getStatus() {
        return moduleApiHandler.sendGet(
                AutoCryptEndpoints.MISC_URI_IS_ALIVE,
                Json.object()
        ).isSuccess();
    }

    /**
     * Verifies whether the http api is reachable or not
     *
     * @return if the http api is reachable
     */
    public boolean saveRoleToDb() {
        return moduleApiHandler.sendGet(
                AutoCryptEndpoints.MISC_URI_IS_ALIVE,
                Json.object()
        ).isSuccess();
    }

    /**
     * Reformats the AutoCrypt error to resend only the user-friendly error
     *
     * @param response raw response
     * @return reformated response
     */
    private static @NotNull JsonApiResponse reformatAutoCryptError(JsonApiResponse response) {
        String errorMessage = Json.read(response.getError().getDetails().asJsonMap().get("content").asString()).get("message").asString();
        return JsonApiResponse.error(errorMessage);
    }
}
