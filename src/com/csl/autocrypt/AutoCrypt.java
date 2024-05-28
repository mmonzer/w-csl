package com.csl.autocrypt;

import com.csl.intercom.cslscan.ApiHandler;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import org.eclipse.jetty.http.HttpMethod;

/**
 * API client of the module AutoCrypt
 */
public class AutoCrypt {
    private String ip = "localhost";
    private int port = 8989;
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
     * @param newPort new port of the API
     */
    public void setPort(int newPort) {
        port = newPort;
    }

    /**
     * Set the new API ip
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
     * @param body     boyd of the POST request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmd(String endpoint, String body) {
        return sendCmd(endpoint, Json.read(body));
    }

    /**
     * Send cmd to the module in method POST
     *
     * @param endpoint endpoint to connect
     * @param body     boyd of the POST request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmd(String endpoint, Json body) {
        return apiHandler.sendRequestToApi(HttpMethod.POST, endpoint, body);
    }
}
