package com.csl.intercom.dbapi;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.exceptions.ServiceNotReadyException;
import com.csl.intercom.dbapi.enums.DbapiEndpointForCSLScan;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.util.Pair;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Manage HTTP communications with DB-API.
 * Provides an interface for retrieving the devices, connections and so on,
 * and to send information to it (CPE Items, a Scan's status, ...).
 */
public class DbapiHandlerForCSLInit extends DbapiHandler {
    private static final Logger logger = LoggerFactory.getLogger(DbapiHandlerForCSLInit.class);

    public DbapiHandlerForCSLInit() {
        this("init", CSLContext.getInstance().getConfig());
    }

    public DbapiHandlerForCSLInit(String moduleName, Config config) {
        super(moduleName, config);
    }

    public void sendCommandsList(List<ApiCommands> apiCommandsList) throws ServiceNotReadyException {
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
        logger.debug("Sending commands to DB-API: {}", requestContents.toString());
        JsonApiResponse response = sendPost(DbapiEndpointForCSLScan.JAVACOMM_SEND_COMMANDS.getEndpoint(), requestContents);
        if (!response.isSuccess()) {
            throw new ServiceNotReadyException("Error sending commands to dbapi: got unexpected status " + response.getError());
        }
    }

    public boolean isDbapiConnected() {
        String uri = "/auth/test_connection_with_apikey";
        JsonApiResponse response = sendGet(uri, Json.object());
         return response.isSuccess();
    }

    public void waitForDbapi(int timeoutSeconds, int retryIntervalSeconds) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        try {
            while (System.currentTimeMillis() - startTime < TimeUnit.SECONDS.toMillis(timeoutSeconds) && !this.isDbapiConnected()) {
                Thread.sleep(TimeUnit.SECONDS.toMillis(retryIntervalSeconds)); // Wait before retrying
            }
        } catch (InterruptedException e) {
            logger.error("Dbapi did not start within {} seconds.", timeoutSeconds);
            throw new InterruptedException("Dbapi did not start within "+timeoutSeconds+" seconds.");
        }

        if (!this.isDbapiConnected()) {
            throw new IllegalStateException("Dbapi service did not start within " + timeoutSeconds + " seconds");
        }
    }
}
