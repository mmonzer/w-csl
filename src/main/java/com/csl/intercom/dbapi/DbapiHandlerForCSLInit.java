package com.csl.intercom.dbapi;

import com.csl.autocrypt.enums.AutocryptConstants.Common;
import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.*;
import com.csl.intercom.cslscan.models.scans.ExternalScan;
import com.csl.intercom.dbapi.enums.ConnectionProtocolField;
import com.csl.intercom.dbapi.enums.DbapiEndpointForCSLScan;
import com.csl.intercom.dbapi.enums.FileActionStatus;
import com.csl.intercom.dbapi.enums.FinishedScanStatus;
import com.csl.intercom.dbapi.exceptions.DbapiUnexpectedStatusCodeException;
import com.csl.intercom.dbapi.models.*;
import com.csl.util.FileStorageService;
import com.csl.util.Pair;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.JsonApiResponse;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.MultiPartContentProvider;
import org.eclipse.jetty.client.util.PathContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    public void sendCommandsList(List<IApiCommands> apiCommandsList) throws Exception {
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
//                .content(new StringContentProvider(requestContents.toString()), "application/json");
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
