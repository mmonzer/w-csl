package com.csl.ids;

import com.csl.intercom.cslscan.ApiHandler;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Tap {
    private String name;
    private int id;
    private String ip = "localhost";
    private int port = 8888;
    private List<Json> includes;
    private ApiHandler apiHandler;

    /**
     * Canonical constructor of a TAP.
     * @param name name of the TAP
     * @param id id of the TAP
     * @param ip ip of the TAP
     * @param port port of API of the TAP
     * @param includes ???
     */
    public Tap(String name, String id, String ip, int port, List<Json> includes) {
        this.name=name;
        this.ip=ip;
        this.port=port;
        this.includes=includes;
        updateApiHandler();
    }
    public Tap(String name, int id, String ip, int port, List<Json> includes) {
        this.name=name;
        this.id=id;
        this.ip=ip;
        this.port=port;
        this.includes=includes;
        updateApiHandler();
    }

    /**
     * Send cmd to the TAP
     * @param endpoint endpoint to connect
     * @param body boyd of the POST request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public Json getFile(String endpoint, Json body) {
        try {
            return apiHandler.downloadFilePost(endpoint, body).toJson();
        } catch (Exception ignored) {
            return JsonApiResponse.error("Could not fetch the file in module").toJson();
        }
    }

    /**
     * Send cmd to the TAP
     * @param endpoint endpoint to connect
     * @param body boyd of the POST request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmd(String endpoint, Json body) {
        return apiHandler.sendPost( endpoint, body);
    }

    /**
     * Send quiet cmd to the TAP
     * @param endpoint endpoint to connect
     * @param body boyd of the POST request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendQuietCmd(String endpoint, Json body) {
        return apiHandler.sendRequestToApiQuiet(HttpMethod.POST, endpoint, body);
    }

    /**
     * Send cmd to the TAP
     * @param endpoint endpoint to connect
     * @param body boyd of the POST request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmd(String endpoint, String body) {
        return  sendCmd(endpoint, Json.read(body));
    }

    /**
     * Send cmd to the TAP
     * @param endpoint endpoint to connect
     * @param body boyd of the POST request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendQuietCmd(String endpoint, String body) {
        return  sendQuietCmd(endpoint, Json.read(body));
    }

    /**
     * Changes the name of the TAP
     * @param name the name of the TAP
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gives the name of the TAP
     * @return the name of the TAP
     */
    public String getName() {
        return name;
    }

    /**
     * Gives the id of the TAP
     * @return the id of the TAP
     */
    public String getId() {
        // return String.valueOf(id);
        return String.valueOf(name);
    }

    /**
     * Changes the ip of the TAP
     * @param ip the new ip of the TAP
     */
    public void setIp(String ip) {
        this.ip=ip;
        updateApiHandler();
    }

    /**
     * Gives the ip of the TAP
     * @return the ip of the TAP
     */
    public String getIp() {
        return ip;
    }

    /**
     * Changes the port of the API of the TAP
     * @param port the port of the API of the TAP
     */
    public void setPort(int port) {
        this.port=port;
        updateApiHandler();
    }

    private void updateApiHandler() {
        apiHandler = new ApiHandler("CSL-Tap", ip, port, false);
    }

    /**
     * Gives the port of the API of the TAP
     * @return the port of the API of the TAP
     */
    public int getPort() {
        return port;
    }

    /**
     * Gives the list of includes of the TAP
     * @return the list of includes of the TAP
     */
    public List<Json> getIncludes() {
        return includes;
    }

    /**
     * Transforms the TAP en json format
     * @return the tap in Json format
     */
    public Json toJson() {
        Json res = Json.object();
        res.at("idname", name);
        res.at("id", id);
        res.at("ip", ip);
        res.at("port", port);
        res.at("includes", includes);
        return res;
    }
}
