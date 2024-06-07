package com.csl.autocrypt;

import com.csl.intercom.cslscan.ApiHandler;
import com.csl.autocrypt.enums.DbapiEndpointForCSLAutocrypt;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;

import static com.csl.autocrypt.CSLAutocryptUtils.reformatAutoCryptError;

public class DbapiHandlerForCSLAutocrypt {
    private ApiHandler apiHandler = null;
    private String ip;
    private String apiKey;

    public DbapiHandlerForCSLAutocrypt(String name, String ip, String apiKey) {
        this.ip = ip;
        this.apiKey = apiKey;

        apiHandler = new ApiHandler("DBAPI - "+name, "https://" + ip + "/api");
        apiHandler.setApiKey(apiKey);
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
     * Send cmd to the module in method DELETE
     *
     * @param endpoint endpoint to connect
     * @param id   identifier of the given element
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmdDelete(String endpoint, int id) {
        JsonApiResponse response =  apiHandler.sendDelete(endpoint+"/"+id, null);
        if (response.isSuccess()) {
            return response;
        } else {
            return reformatAutoCryptError(response);
        }
    }
}
