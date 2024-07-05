package com.csl.intercom.dbapi;

import com.csl.core.CSLContext;
import com.csl.intercom.cslscan.ApiHandler;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.CpeItem;
import com.csl.intercom.cslscan.models.EntityHttpConnection;
import com.csl.intercom.cslscan.models.MicrosoftKB;
import com.csl.intercom.dbapi.enums.ConnectionProtocolField;
import com.csl.intercom.dbapi.enums.DbapiEndpointForCSLScan;
import com.csl.intercom.dbapi.enums.FinishedScanStatus;
import com.csl.intercom.dbapi.models.Connection;
import com.csl.intercom.dbapi.models.ConnectionProtocol;
import com.csl.intercom.dbapi.models.Device;
import com.csl.intercom.dbapi.models.ScanEntity;
import com.csl.util.Pair;
import com.ucsl.interfaces.IAlertDescriptor;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.JsonApiResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Manage HTTP communications with DB-API. It offers a general interface to reach the db.
 */
public class DbapiHandler extends ApiHandler {
    private static final Logger logger = LoggerFactory.getLogger(DbapiHandler.class);

    public DbapiHandler() {
        this(CSLContext.instance.getConfig());
    }

    public DbapiHandler(Json config) {
        super("DB-API",
                (JsonUtil.getBooleanFromJson(config.get("global"), "use_ssl", true) ? "https://" : "http://")+
                        JsonUtil.getStringFromJson(config.get("global"), "ip_server_remote", "localhost")+
                        "/api");
        setApiKey(JsonUtil.getStringFromJson(config.get("global"), "api_key", ""));
    }

    public DbapiHandler(String moduleName) {
        this(moduleName, CSLContext.instance.getConfig());
    }

    public DbapiHandler(String moduleName, Json config) {
        super("DB-API::"+moduleName,
                (JsonUtil.getBooleanFromJson(config.get("global"), "use_ssl", true) ? "https://" : "http://")+
                        JsonUtil.getStringFromJson(config.get("global"), "ip_server_remote", "localhost")+
                        "/api");
        setApiKey(JsonUtil.getStringFromJson(config.get("global"), "api_key", ""));
    }
}
