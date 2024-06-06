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
    private String ip;
    private int port;
    private ApiHandler apiHandler = null;

    /**
     * Get the API port
     */
    public int getPort() {
        return port;
    }

    /**
     * Get the API ip
     */
    public String getIp() {
        return ip;
    }

    /**
     * Set the new API port
     *
     * @param newPort new port of the API
     */
    public void setPort(int newPort) {
        port = newPort;
    }

    /**
     * Set the new API ip
     *
     * @param newIp new ip of the API
     */
    public void setIp(String newIp) {
        ip = newIp;
    }

    /**
     * Reinit the handler point
     */
    public void reinitApiHandler() {
        apiHandler = new ApiHandler("CSL-AutoCrypt", "http://" + ip + ":" + port);
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
        JsonApiResponse response =  apiHandler.sendPost(endpoint, body);
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
        JsonApiResponse response =  apiHandler.sendPost(endpoint, params, body);
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
        JsonApiResponse response =  apiHandler.sendPostFile(endpoint, params, body);
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
        JsonApiResponse response = apiHandler.sendGet(endpoint, params);
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
        JsonApiResponse response =  apiHandler.sendDelete(endpoint, params);
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
        JsonApiResponse response =  apiHandler.sendDelete(endpoint, params, body);
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
        JsonApiResponse response =  apiHandler.sendPut(endpoint, params, body);
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
        JsonApiResponse response =  apiHandler.sendPut(endpoint, body);
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
        return apiHandler.sendGet(
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
