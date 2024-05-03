package com.csl.ids;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import org.eclipse.jetty.http.HttpMethod;

import java.util.List;

public class Tap {
    private String name;
    private String id;
    private String ip;
    private int port;
    private List<Json> includes;
    private ScanApiHandler apiHandler;

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
        this.id=id;
        this.ip=ip;
        this.port=port;
        this.includes=includes;
        apiHandler = new ScanApiHandler("http://"+ip+":"+port);
    }

    /**
     * Send cmd to the TAP
     * @param endpoint endpoint to connect
     * @param body boyd of the POST request
     * @return the {@link JsonApiResponse} returned by the manager
     */
    public JsonApiResponse sendCmd(String endpoint, Json body) {
        return apiHandler.sendRequestToScanManager(HttpMethod.POST, endpoint, body);
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
        return id;
    }

    /**
     * Gives the ip of the TAP
     * @return the ip of the TAP
     */
    public String getIp() {
        return ip;
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
}
