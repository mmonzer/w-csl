package com.csl.intercom.dbapi;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.intercom.dbapi.enums.DbapiEndpointForCSLScan;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.util.Pair;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Predicate;

/**
 * Manage HTTP communications with DB-API.
 * Provides an interface for retrieving the devices, connections and so on,
 * and to send information to it (CPE Items, a Scan's status, ...).
 */
public class DbapiHandlerForCSLInit extends DbapiHandler {
    private static final Logger logger = LoggerFactory.getLogger(DbapiHandlerForCSLInit.class);

    public DbapiHandlerForCSLInit() {
        this("init", CSLContext.instance.getConfig());
    }

    public DbapiHandlerForCSLInit(String moduleName, Config config) {
        super(moduleName, config);
    }

    public void sendCommandsList(List<ApiCommands> apiCommandsList) throws Exception {
        Json requestContents = Json.object();
        apiCommandsList.stream()
                .map(apiCommands -> new Pair<>(apiCommands.getName(), apiCommands.getListOfCommandPrivileges()))
                .filter(Predicate.not(pair -> pair.getSecond().isEmpty()))
                .map(pair -> pair.map((name, map) -> {
                    Json result = Json.object();
                    map.forEach((key, value) -> result.set(key, value.toString()));
                    return new Pair<>(name, result);
                }))
                .forEach(pair -> requestContents.set(pair.getFirst(), pair.getSecond()));
        logger.debug("Sending commands to DB-API: " + requestContents.toString());
//        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.JAVACOMM_SEND_COMMANDS)
//                .content(new StringContentProvider(requestContents.toString()), JSON_FORMAT);
//        ContentResponse response = request.send();
//        if (response.getStatus() != 200) {
//            throw new Exception("Error sending commands to dbapi: got unexpected status " + response.getStatus());
//        }
        JsonApiResponse response = sendPost(DbapiEndpointForCSLScan.JAVACOMM_SEND_COMMANDS.getEndpoint(), requestContents);
        if (!response.isSuccess()) {
            throw new Exception("Error sending commands to dbapi: got unexpected status " + response.getError());
        }
    }
}
