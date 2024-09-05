package main.services;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.intercom.broker.CSLMqttBrokerHandler;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.cslscan.ScanWebSocketHandler;
import com.csl.intercom.cslscan.models.*;
import com.csl.intercom.cslscan.models.scans.ExternalScan;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.services.ExternalConnectionInfoTemplatesSynchronizationService;
import com.csl.intercom.services.ExternalConnectionInfoSynchronizationService;
import com.csl.intercom.services.ExternalScansService;
import com.csl.intercom.cslscan.enums.DynamicDiscoveryFrequencyOption;
import com.csl.intercom.cslscan.models.CpeItem;
import com.csl.intercom.cslscan.models.EntityHttpConnection;
import com.csl.intercom.cslscan.models.EntityHttpConnectionTestResult;
import com.csl.intercom.cslscan.services.ImportExportBsonService;
import com.csl.intercom.dbapi.models.*;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.csl.intercom.jsoncmd.JsonCmdPrivilegeFamily;
import com.csl.intercom.services.*;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.csl.intercom.status.IStatusProvider;
import com.csl.logger.CustomLogger;
import com.csl.logger.LoggerActions;
import com.csl.logger.LoggerInterfaces;
import com.csl.util.FileStorageService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.csl.web.jcmdoversocket.CSLWebSocketForJcmd.X_CORRELATION_ID;

/**
 * Service in charge of the SNMP manager microservice.
 * It should expose an API to request a scan and fetch the database.
 * It also allows to know the current status of the requested scans.
 */
public class DiscoveryServices extends Service implements IStatusProvider {
    static private final String defaultConfigFileSectionName = "discovery";
    static private final String defaultName = "discovery";

    private static final CustomLogger logger = CustomLogger.getLogger(DiscoveryServices.class);
    private static final Logger log = LoggerFactory.getLogger(DiscoveryServices.class);
    private final boolean isConcentrator;
    private ScanWebSocketHandler scanWebSocketHandler = null;
    @Getter
    @Setter
    private DbapiHandlerForCSLScan dbapiHandler = null;
    @Getter
    @Setter
    private ScanApiHandler scanApiHandler = null;
    private FileStorageService fileStorageService = null;
    private ImportExportBsonService importExportBsonService = null;
    private CSLMqttBrokerHandler mqttBroker = null;
    private ExternalScansService externalScansService = null;
    private ExternalConnectionInfoSynchronizationService externalConnectionInfoSynchronizationService = null;
    private ExternalConnectionInfoTemplatesSynchronizationService externalConnectionInfoTemplatesSynchronizationService = null;
    private ExternalDiscoveredDevicesSynchronizationService externalDiscoveredDevicesSynchronizationService = null;
    @Setter
    private DataSynchronizationService cpeItemSynchronizationService = null;
    @Setter
    private DataSynchronizationService microsoftKbSynchronizationService = null;
    @Getter
    @Setter
    private DataSynchronizationService deletedCpeItemsSynchronizationService = null;
    private DataSynchronizationService deletedMicrosoftKbsSynchronizationService = null;
    @Getter
    @Setter
    private CpeScanService cpeScanService = null;
    private ScheduledExecutorService synchronizationSchedule;

    /**
     * Generic constructor of the Discovery service.
     */
    public DiscoveryServices(String name, String configFileSectionName, boolean isConcentrator) {
        super(name,
                "Service in charge of the SNMP manager microservice.\n" +
                        "It should expose an API to request a scan and fetch the database.\n" +
                        "It also manages CSL-Scan and the scanning.",
                configFileSectionName);
        this.isConcentrator = isConcentrator;
    }

    /**
     * Constructor of the Discovery service with Concentration
     */
    public DiscoveryServices() {
        this(defaultName, defaultConfigFileSectionName, true);
    }

    /**
     * Constructor of the Discovery service with custom concentration
     */
    public DiscoveryServices(boolean isConcentrator) {
        this(defaultName, defaultConfigFileSectionName, isConcentrator);
    }

    /**
     * Initialize the service, setting the list of known managers and registering the commands.
     *
     * @return True is the initialization was successful, false otherwise
     */
    @Override
    public boolean init() {
        logger.info("Initializing SNMP service ..");

//        String scanManagerDiscoveryUrl = ScanUtils.generateScanDiscoveryUrlFromConfig(jConfig);
        String scanManagerDiscoveryUrl = ScanUtils.generateScanDiscoveryUrlFromConfig(Config.instance.Scan);

        dbapiHandler = new DbapiHandlerForCSLScan();
        scanApiHandler = new ScanApiHandler();
        fileStorageService = new FileStorageService();

//        Json globalConfig = CSLContext.instance.getConfig().get("global");

        if (isConcentrator) {
            cpeScanService = new CpeScanService();
            cpeItemSynchronizationService = new CpeItemsSynchronizationService(scanApiHandler, dbapiHandler, cpeScanService);
            microsoftKbSynchronizationService = new MicrosoftKbSynchronizationService(scanApiHandler, dbapiHandler, cpeScanService);
            deletedCpeItemsSynchronizationService = new DeletedCpeItemsSynchronizationService(scanApiHandler, dbapiHandler);
            deletedMicrosoftKbsSynchronizationService = new DeletedMicrosoftKbsSynchronizationService(scanApiHandler, dbapiHandler);
            cpeScanService.init(cpeItemSynchronizationService, microsoftKbSynchronizationService);
            importExportBsonService = ImportExportBsonService.getInstance();
            importExportBsonService.init(dbapiHandler, scanApiHandler, fileStorageService);
            externalConnectionInfoSynchronizationService = new ExternalConnectionInfoSynchronizationService(scanApiHandler, dbapiHandler);
            externalConnectionInfoSynchronizationService.synchronizeExternalConnectionInfos();
            externalConnectionInfoTemplatesSynchronizationService = new ExternalConnectionInfoTemplatesSynchronizationService(scanApiHandler, dbapiHandler, 3600);
            externalDiscoveredDevicesSynchronizationService = new ExternalDiscoveredDevicesSynchronizationService(dbapiHandler, scanApiHandler);
            externalScansService = new ExternalScansService(dbapiHandler, scanApiHandler, externalDiscoveredDevicesSynchronizationService);
            scanWebSocketHandler = new ScanWebSocketHandler(this, scanManagerDiscoveryUrl, cpeScanService, importExportBsonService, externalScansService);
            externalDiscoveredDevicesSynchronizationService.init(externalScansService);

            mqttBroker = CSLContext.instance.getMqttBroker();
            mqttBroker.subscribeToTopic(CSLMqttBrokerHandler.Topic.DEVICES, message -> {
                dbapiHandler.sendNewDevicesToScanner(scanApiHandler);
            });
            mqttBroker.subscribeToTopic(CSLMqttBrokerHandler.Topic.CPE_ITEMS, message -> {
                try {
                    deletedCpeItemsSynchronizationService.syncData();
                } catch (SynchronizationException e) {
                    logger.trace("Could not synchronize deleted CPE Items", e);
                }
            });
            mqttBroker.subscribeToTopic(CSLMqttBrokerHandler.Topic.FILE_ACTION_STATUS, message -> {
                Json jsonPayload = Json.read(message.getResults());
                if (jsonPayload.has("correlation_id")) {
                    MDC.put(X_CORRELATION_ID, jsonPayload.get("correlation_id").asString());
                }
                // TODO: change correlation_id by X-Correlation-ID
                HttpTemplateImportNotification notification = HttpTemplateImportNotification.fromMQTTMessage(jsonPayload);
                    if (notification != null) {
                    if (notification.getType() == HttpTemplateImportNotification.Type.FILE_RECEIVED) {
                        importExportBsonService.startNewImportTask(notification);
                    }
                }
            });
        }

        synchronizationSchedule = Executors.newScheduledThreadPool(1);
        synchronizationSchedule.scheduleAtFixedRate(this::syncAll, 0, 300, TimeUnit.SECONDS);

        addCmd("get_status", params -> {
                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Fetching CSL-Scan status");
                    Json response = getStatus();
                    logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"CSL-Scan status : {}", response);
                    return response;
                },
                new JsonCmdHelp()
                        .setDesc("Retrieve the status of the service.")
                        .setResult("A status notification: " +
                                "<code>" +
                                "{" +
                                "\"is_http_api_reachable\": true/false" +
                                "\"is_websocket_connected\": true/false" +
                                "}" +
                                "</code>", IJsonCmdHelp.JSON).setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_all_cpes", params -> {
                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Fetching all CPEs ...");

                    List<CpeItem> cpeItems = getAllCpes();

                    if (cpeItems == null) {
                        logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to fetch all CPEs");
                        return JsonApiResponse.error("Failed to fetch all CPEs").toJson();
                    } else {
                        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Successfully fetched all CPEs");
                        return JsonApiResponse.result(Json.array(cpeItems)).toJson();
                    }
                }
                ,
                new JsonCmdHelp().setDesc("Get the CPE Items in CSL-Scan")
                        .setResult("The list of CPE Items, in the format <code>{\"success\": true, \"result\": [...]}", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_entity_cpes", params -> {
                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Fetching entity CPE ...");
                    logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Fetching entity CPE : params={}", params);

                    String id = params.get("id").asString();
                    logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"Fetching entity CPE : id={}", id);
                    JsonApiResponse response = scanApiHandler.getEntityCpes(id);
                    logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Fetched entity CPE (id={}) : {}", id, response);

                    if (!response.isSuccess()) {
                        logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to fetch entity CPE ({}) : {}", id, response.getError());
                    } else {
                        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Successfully fetched entity CPE ({})", id);
                    }
                    return response.toJson();
                },
                new JsonCmdHelp().setDesc("Get an entity's CPE Items")
                        .setParam("id", "The entity's uuid", IJsonCmdHelp.STR)
                        .setResult("The list of CPE Items of the entity, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_cpes_since", params -> {
                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Fetching CPEs since date");
                    logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Fetching CPEs since : params={}", params);

                    String date = JsonUtil.getStringFromJson(params, "date", null);
                    logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"Fetching CPEs since {}", date);
                    List<CpeItem> cpeItems = scanApiHandler.getCpeItemChangesSince(OffsetDateTime.parse(date));
                    logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Fetched CPEs since {} : {}", date, cpeItems);

                    if (cpeItems == null) {
                        logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to fetch CPEs since {}", date);
                        return JsonApiResponse.error("Failed to fetch CPEs since " + date).toJson();
                    } else {
                        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Successfully fetched CPEs since {}", date);
                        return JsonApiResponse.result(Json.array(cpeItems)).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Retrieve CPE Items that change strictly after a specified date")
                        .setParam("date", "in ISO format, example: 2023-04-13T13:56:56.66 (local date format)", IJsonCmdHelp.STR)
                        .setResult("The list of CPE Items that changed strictly after <code>date</code>, in the format" +
                                "<code>{\"success\": true, \"result\": [...]</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("scan_status", params -> {
                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Fetching scan status ...");
                    logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Fetching scan status : params={}", params);

                    String id = params.get("id").asString();
                    logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"Fetching scan status with id {}", id);
                    JsonApiResponse response = scanApiHandler.getScanStatus(id);
                    logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Fetched scan status with id {}: {}", id, response);

                    if (response.isSuccess()) {
                        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Successfully fetched scan status {}", id);
                        return response.toJson();
                    } else {
                        logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to fetch scan status ({}): {}", id, response.getError());
                        return JsonApiResponse.error("Could not drop collections",
                                Json.object("exception", response.getError())).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Get the status of a specific scan")
                        .setParam("id", "The uuid of the scan to inquire", JsonCmdHelp.STR)
                        .setResult("The status of the scan, in the format <code>{ \"success\": true, \"result\": { ... } }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("start_scan", params -> {
                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Starting the scan ...");
                    logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Starting the scan : params= {}", params);

                    List<String> entities = new ArrayList<>();
                    if (params.has("entities")) {
                        for (Json entity : params.get("entities").asJsonList()) {
                            if (entity.isString()) {
                                entities.add(entity.asString());
                            }
                        }
                    }
                    logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_WS,"Starting the scan : entities={}", entities);
                    JsonApiResponse response = startScan(entities);
                    logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_WS,"Started the scan : entities={} : {}", entities, response );

                    if (response.isSuccess()) {
                        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Successfully started scan");
                        return response.toJson();
                    } else {
                        logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to start the scan : {}", response.getError());
                        return JsonApiResponse.error("Could not drop collections",
                                Json.object("exception", response.getError())).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Start a scan from CSL-Scan")
                        .setParam("entities", "An array of strings with the uuids of the entities to scan. May be omitted or null, resulting in scanning all entities.", IJsonCmdHelp.JSON)
                        .setResult("<code>{ \"success\": true }</code> if the scan was started successfully", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.START_CPE_SCAN
        );
        addCmd("stop_scan", params -> {
                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Stopping the scan ...");

                    try {
                        // TODO :  add debug logs, but this method calls DBAPI and SCAN...
                        this.cpeScanService.cancelScan();

                        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Successfully stopped scan");
                        return JsonApiResponse.success().toJson();
                    } catch (Exception e) {
                        logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to stop the scan");
                        return JsonApiResponse.error("Could not stop the scan", Json.object("exception", e.getMessage())).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Stop a scan in CSL-Scan")
                        .setParam("id", "The uuid of the scan to stop", IJsonCmdHelp.STR)
                        .setResult("<code>{ \"success\": true }</code> if the scan was stopped successfully", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.START_CPE_SCAN
        );
        addCmd("synchronize_devices", params -> {
                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Synchronizing all devices ...");

                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_DBAPI_API,"Synchronizing all devices ...");
                    JsonApiResponse response = dbapiHandler.sendNewDevicesToScanner(scanApiHandler);
                    logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_DBAPI_API,"Synchronized all devices : {}", response);

                    if (response.isSuccess()) {
                        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Successfully synchronized all devices");
                        return response.toJson();
                    } else {
                        logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to synchronize all devices : {}", response.getError());
                        return JsonApiResponse.error("Could not drop collections",
                                Json.object("exception", response.getError())).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Synchronize devices between DB-API and CSL-Scan.")
                        .setResult("<code>{\"success\": true }</code> if the synchronisation went without error," +
                                "<code>{\"success\": false, \"error\", {\"reason\": \"...\", \"failed_devices\": [...]}}</code> otherwise. The failed_devices field is present if devices were actually fetched from DB-API.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.MANAGE_SCAN_DB
        );
        addCmd("drop_all_collections", params -> {
                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Dropping all collections ...");
                    try {
                        logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"Dropping all collections from dbapi ...");
                        scanApiHandler.dropAllCollections();
                        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Dropped all collections from dbapi");

                        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Successfully dropped all collections");
                        return JsonApiResponse.success().toJson();
                    } catch (Exception e) {
                        logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Could not drop all collections", e);
                        return JsonApiResponse.error("Could not drop collections",
                                Json.object("exception", e.getMessage())
                        ).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Drop all collections in DB-API")
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.MANAGE_SCAN_DB
        );
        addCmd("get_entity_http_connections", params -> {
                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Getting list of entities to HTTP connections ...");

                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"Getting list of entities to HTTP connections from Scan API ...");
                    List<EntityHttpConnection> entityHttpConnections = scanApiHandler.getAllEntityHttpConnections(true);
                    logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Getting list of entities to HTTP connections  from Scan API : {}", entityHttpConnections);

                    if (entityHttpConnections == null) {
                        logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to get list of entities to HTTP connections : entityHttpConnections={}", entityHttpConnections);
                        return JsonApiResponse.error("Could not fetch entity HTTP connections from CSL-Scan",
                                Json.object("exception", "Could not fetch entity HTTP connections from CSL-Scan")
                        ).toJson();
                    } else {
                        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Successfully got the list of entities to HTTP connections (only visible)");
                        return JsonApiResponse.result(Json.array(entityHttpConnections.stream().map(EntityHttpConnection::serializeForDbapi).toArray())).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Get all entity HTTP connections from CSL-Scan")
                        .setResult("The list of entity HTTP connections, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_entity_http_connections_full", params -> {
                    logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Getting the list of entities to HTTP connections full ...");

                    logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Getting the list of entities to HTTP connections full from Scan API ...");
                    List<EntityHttpConnection> entityHttpConnections = scanApiHandler.getAllEntityHttpConnections(false);
                    logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Got the list of entities to HTTP connections full from Scan API: {}", entityHttpConnections);

                    if (entityHttpConnections == null) {
                        logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to get the list of entities to HTTP connections full : entityHttpConnections={}", entityHttpConnections);
                        return JsonApiResponse.error("Could not fetch entity HTTP connections from CSL-Scan",
                                Json.object("exception", "Could not fetch entity HTTP connections from CSL-Scan")
                        ).toJson();
                    } else {
                        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Successfully got the list of entities to HTTP connections full (visible and hidden)");
                        return JsonApiResponse.result(Json.array(entityHttpConnections.stream().map(EntityHttpConnection::serializeForDbapi).toArray())).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Get all entity HTTP connections from CSL-Scan, also showing non-visible stages")
                        .setResult("The list of entity HTTP connections, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.MANAGE_HTTP_TEMPLATES
        );
        addCmd("get_entity_http_connection", params -> {
                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Getting entity of HTTP connection ...");
                    logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Getting entity of HTTP connection with params={} ...", params);

                    Json uuidJson = params.get("uuid");
                    String uuid;
                    if (uuidJson == null) {
                        uuid = null;
                    } else if (uuidJson.isString()) {
                        uuid = uuidJson.asString();
                    } else if (uuidJson.isNumber()) {
                        uuid = String.valueOf(uuidJson.asInteger());
                    } else {
                        uuid = null;
                    }

                    if (uuid == null) {
                        logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to get entity to HTTP connection: uuid={}", uuid);
                        return JsonApiResponse.error("Missing required parameter uuid",
                                Json.object("exception", "Missing parameter uuid, of type string or integer")
                        ).toJson();
                    }

                    logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"Getting entity of HTTP connection with uuid={} ...", uuid);
                    EntityHttpConnection entityHttpConnection = scanApiHandler.getEntityHttpConnection(uuid, true);
                    logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Got entity of HTTP connection with uuid={} : ", entityHttpConnection);

                    if (entityHttpConnection == null) {
                        logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to get entity to HTTP connection: uuid={}", uuid);
                        return JsonApiResponse.error("Could not fetch entity HTTP connection from CSL-Scan",
                                Json.object("exception", "Could not fetch entity HTTP connection from CSL-Scan")
                        ).toJson();
                    } else {
                        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Successfully got entity to HTTP connection: uuid={}", uuid);
                        return JsonApiResponse.result(entityHttpConnection.serializeForDbapi()).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Get a specific entity HTTP connection from CSL-Scan")
                        .setParam("uuid", "The uuid of the entity HTTP connection to retrieve", IJsonCmdHelp.STR)
                        .setResult("The entity HTTP connection, in the format <code>{ \"success\": true, \"result\": { ... } }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_entity_http_connection_full", params -> {
                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Getting entity to HTTP connection full ...");
                    logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Getting entity to HTTP connection full: params={}", params);

                    Json uuidJson = params.get("uuid");
                    String uuid;
                    if (uuidJson == null) {
                        uuid = null;
                    } else if (uuidJson.isString()) {
                        uuid = uuidJson.asString();
                    } else if (uuidJson.isNumber()) {
                        uuid = String.valueOf(uuidJson.asInteger());
                    } else {
                        uuid = null;
                    }

                    if (uuid == null) {
                        logger.error(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Failed to get entity to HTTP connection full: uuid={}", uuid);
                        return JsonApiResponse.error("Missing required parameter uuid",
                                Json.object("exception", "Missing parameter uuid, of type string or integer")
                        ).toJson();
                    }
                    logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"Getting entity to HTTP connection full from Scan API with uuid={} ...", uuid);
                    EntityHttpConnection entityHttpConnection = scanApiHandler.getEntityHttpConnection(uuid, false);
                    logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Getting entity to HTTP connection full from Scan API with uuid={}: {}", uuid, entityHttpConnection);
                    if (entityHttpConnection == null) {
                        logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to get entity to HTTP connection full: uuid={}", uuid);
                        return JsonApiResponse.error("Could not fetch entity HTTP connection from CSL-Scan",
                                Json.object("exception", "Could not fetch entity HTTP connection from CSL-Scan")
                        ).toJson();
                    } else {
                        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Successfully got entity to HTTP connection full (visible and hidden): uuid={}", uuid);
                        return JsonApiResponse.result(entityHttpConnection.serializeForDbapi()).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Get a specific entity HTTP connection from CSL-Scan, also showing non-visible stages")
                        .setParam("uuid", "The uuid of the entity HTTP connection to retrieve", IJsonCmdHelp.STR)
                        .setResult("The entity HTTP connection, in the format <code>{ \"success\": true, \"result\": { ... } }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.MANAGE_HTTP_TEMPLATES
        );
        addCmd("delete_entity_http_connection", params -> {
                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Delete entity to HTTP connection ...");
                    logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Delete entity to HTTP connection with params={} ...", params);

                    Json uuidJson = params.get("uuid");
                    String uuid;
                    if (uuidJson == null) {
                        uuid = null;
                    } else if (uuidJson.isString()) {
                        uuid = uuidJson.asString();
                    } else if (uuidJson.isNumber()) {
                        uuid = String.valueOf(uuidJson.asInteger());
                    } else {
                        uuid = null;
                    }

                    logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"Fetching entity to HTTP connection to delete from Scan API uuid={} ...", uuid);
                    EntityHttpConnection entityHttpConnection = scanApiHandler.getEntityHttpConnection(uuid, false);
                    logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Fetched entity to HTTP connection to delete from Scan API uuid={} : {}", uuid, entityHttpConnection);

                    try {
                        logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"Deleting entity to HTTP connection from Scan API with uuid={} ...", uuid);
                        JsonApiResponse response = scanApiHandler.deleteEntityHttpConnection(uuid);
                        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Deleted entity to HTTP connection from Scan API with uuid={} : {}", uuid, response);

                        if (response.isSuccess()) {
                            logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_DBAPI_API,"Deleting entity to HTTP connection from Scan API with uuid={} succeeded, deleting in Dbapi ...", uuid);
                            dbapiHandler.deleteDiscoveryProtocol(uuid);
                            logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_DBAPI_API,"Deleting entity to HTTP connection from Scan API with uuid={} succeeded, deleted in Dbapi", uuid);

                            logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Fetched entity to HTTP connection to delete: uuid={} uuid={}", uuid, entityHttpConnection);
                            return JsonApiResponse.success().toJson();
                        } else {
                            logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"Deleting entity to HTTP connection from Scan API with uuid={} failed, rolling back ...", uuid);
                            JsonApiResponse responseRollingBack = scanApiHandler.createEntityHttpConnection(entityHttpConnection);
                            logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Deleting entity to HTTP connection from Scan API with uuid={} failed, rolled back : {}", uuid, responseRollingBack);

                            logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Fetched entity to HTTP connection to delete: uuid={} uuid={}", uuid, entityHttpConnection);
                            return response.toJson();
                        }
                    } catch (Exception e) {
                        logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Could not delete entity HTTP connection from CSL-Scan", e);
                        return JsonApiResponse.error("Could not delete entity HTTP connection from CSL-Scan",
                                Json.object("exception", e.getMessage())
                        ).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Delete an EntityHttpConnection from CSL-Scan")
                        .setParam("uuid", "The uuid of the EntityHttpConnection to delete", IJsonCmdHelp.STR)
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.MANAGE_HTTP_TEMPLATES
        );
        addCmd("add_entity_http_connection", params -> {
                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Adding entity to HTTP connection ...");
                    logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Adding entity to HTTP connection with params={} ...", params);

                    Json entityHttpConnectionJson = params.get("entity_http_connection");
                    EntityHttpConnection entityHttpConnection = EntityHttpConnection.fromDbapiJson(entityHttpConnectionJson);
                    logger.trace(LoggerActions.NULL, LoggerInterfaces.NULL,"Adding entity to HTTP connection : entityHttpConnection={}", entityHttpConnection);
                    if (entityHttpConnection == null) {
                        logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to add entity to HTTP connection : could not parse entity_http_connection : {}", entityHttpConnectionJson);
                        return JsonApiResponse.error("Could not parse entity_http_connection",
                                Json.object("exception", "Could not parse entity_http_connection")
                        ).toJson();
                    }
                    JsonApiResponse response;
                    if (entityHttpConnection.getUuid() == null) {
                        logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"HTTP connection not found, creating entity HTTP connection in CSL-Sca ...");
                        response = scanApiHandler.createEntityHttpConnection(entityHttpConnection);
                        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"HTTP connection not found, created entity HTTP connection in CSL-Scan : {}", response);

                        if (response.isSuccess()) {
                            EntityHttpConnection createdEntityHttpConnection = EntityHttpConnection.fromDbapiJson(response.getResult());
                            logger.trace(LoggerActions.NULL, LoggerInterfaces.NULL,"Parsed entity HTTP connection to DBapi format: createdEntityHttpConnection={}", createdEntityHttpConnection);
                            try {
                                if (createdEntityHttpConnection == null) {
                                    logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Could not parse the created entity_http_connection");
                                    throw new Exception("Could not parse the created entity_http_connection");
                                }

                                logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_DBAPI_API,"Creating discovery protocol for entity HTTP connection in CSL-Dbapi ...");
                                dbapiHandler.createDiscoveryProtocol(createdEntityHttpConnection);
                                logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_DBAPI_API,"Created discovery protocol for entity HTTP connection in CSL-Dbapi : entityHttpConnection={}", entityHttpConnection);

                                logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Created discovery protocol for entity HTTP connection in CSL-Dbapi.");
                            } catch (Exception e) {
                                logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"Could not create discovery protocol in CSL-Dbapi ({}), rolling back ...", entityHttpConnection);
                                JsonApiResponse responseFromDeletion = scanApiHandler.deleteEntityHttpConnection(createdEntityHttpConnection.getUuid());
                                logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Could not create discovery protocol in CSL-Dbapi ({}), rolled back : {}", entityHttpConnection, responseFromDeletion);

                                logger.warn(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Could not create discovery protocol in CSL-Dbapi (entityHttpConnection={}), compensated in CSL-Scan", entityHttpConnection, e);
                                response = JsonApiResponse.error("Could not create discovery protocol",
                                        Json.object("exception", e.getMessage())
                                );
                            }
                        }
                    } else {
                        logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"Fetching HTTP connection in CSL-Scan ...");
                        EntityHttpConnection previousEntityHttpConnection = scanApiHandler.getEntityHttpConnection(entityHttpConnection.getUuid(), false);
                        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Fetched HTTP connection in CSL-Scan : {}", previousEntityHttpConnection);

                        logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"Updating entity HTTP connection information in CSL-Scan with previousEntityHttpConnection={} ...", previousEntityHttpConnection);
                        response = scanApiHandler.updateEntityHttpConnection(entityHttpConnection);
                        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Updated entity HTTP connection in CSL-Scan with previousEntityHttpConnection={} : {}", previousEntityHttpConnection, response);

                        try {
                            logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_DBAPI_API,"Updating entity HTTP connection information in CSL-Dbapi with entityHttpConnection={} ...", entityHttpConnection);
                            dbapiHandler.updateDiscoveryProtocol(entityHttpConnection);
                            logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_DBAPI_API,"Updated entity HTTP connection information in CSL-Dbapi with entityHttpConnection={}", entityHttpConnection);

                            logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Updated entity HTTP connection in CSL-Dbapi");
                        } catch (Exception e) {
                            logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"Updating entity HTTP connection information in CSL-Dbapi with previousEntityHttpConnection={} failed, rolling back ...", previousEntityHttpConnection);
                            scanApiHandler.updateEntityHttpConnection(previousEntityHttpConnection);
                            logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Updating entity HTTP connection information in CSL-Scan with previousEntityHttpConnection={} failed, rolled back", previousEntityHttpConnection);

                            logger.warn(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Could not update discovery protocol in CSL-Dbapi (entityHttpConnection={}), compensated in CSL-Scan", entityHttpConnection, e);
                            response = JsonApiResponse.error("Could not update discovery protocol",
                                    Json.object("exception", e.getMessage())
                            );
                        }
                    }
                    return response.toJson();
                },
                new JsonCmdHelp().setDesc("Add an EntityHttpConnection to CSL-Scan")
                        .setParam("entity_http_connection", "The EntityHttpConnection to add", IJsonCmdHelp.JSON)
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.MANAGE_HTTP_TEMPLATES
        );
        addCmd("test_connection", params -> {
                    logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Testing HTTP connection ...");

                    String deviceUuid = JsonUtil.getStringFromJson(params, "device_uuid", null);
                    logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Testing HTTP connection : deviceUuid={}", deviceUuid);

                    String connectionId;
                    Json connectionIdJson = params.get("connection_id");
                    logger.trace(LoggerActions.NULL, LoggerInterfaces.NULL,"Testing HTTP connection : deviceUuid={}  connectionId={}", deviceUuid, connectionIdJson);
                    if (connectionIdJson != null && connectionIdJson.isString()) {
                        connectionId = connectionIdJson.asString();
                    } else if (connectionIdJson != null && connectionIdJson.isNumber()) {
                        connectionId = String.valueOf(connectionIdJson.asInteger());
                    } else {
                        connectionId = null;
                    }
                    logger.trace(LoggerActions.NULL, LoggerInterfaces.NULL,"Testing HTTP connection : parsed connection id : connectionId={}", connectionId);

                    if (deviceUuid == null || connectionId == null) {
                        logger.warn(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Testing HTTP connection failed: required not null deviceUuid={}  connectionId={}", deviceUuid, connectionIdJson);
                        return JsonApiResponse.error("Missing required parameter device_uuid or connection_id",
                                Json.object("exception", "Missing parameter device_uuid or connection_uuid, of type string")
                        ).toJson();
                    } else {
                        logger.debug(LoggerActions.REQUEST, LoggerInterfaces.CSL_SCAN_API,"Testing HTTP connection with deviceUuid={}, connectionId={} ...", deviceUuid, connectionId);
                        JsonApiResponse response = scanApiHandler.testConnection(deviceUuid, connectionId);
                        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SCAN_API,"Tested HTTP connection with deviceUuid={}, connectionId={} : {}", deviceUuid, connectionId, response);

                        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Tested HTTP connection (deviceUuid={}  connectionId={})", deviceUuid, connectionIdJson);
                        return response.toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Test if an existing connection is valid")
                        .setParam("device_uuid", "The uuid of the device to test the connection on", IJsonCmdHelp.STR)
                        .setParam("connection_id", "The id of the connection to test", IJsonCmdHelp.STR)
                        .setResult("<code>{ \"success\": true, \"result\": { \"value\": \"true/false\" }</code> if the operation went without error, " +
                                "where result contains \"true\" (as a String) if the connection is valid," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
//        addCmd("test_new_connection", params -> {
//                    String ipAddress = JsonUtil.getStringFromJson(params, "ip_address", null);
//                    Json connectionJson = params.get("connection");
//                    Json baseConnectionIdJson = params.get("base_connection_id");
//
//                    connectionJson.set("id", 0);
//                    connectionJson.set("connected_devices", Json.array(0));
//                    List<ConnectionProtocol> protocols;
//                    try {
//                        protocols = dbapiHandler.fetchDiscoveryProtocols();
//                    } catch (ExecutionException | InterruptedException | TimeoutException e) {
//                        logger.error("Could not fetch discovery protocols", e);
//                        throw new RuntimeException(e);
//                    }
//
//                    // Fetch the password from the base connection if needed
//                    if (baseConnectionIdJson != null && baseConnectionIdJson.isNumber()) {
//                        try {
//                            Connection baseConnection = dbapiHandler.fetchConnections(List.of(baseConnectionIdJson.asInteger()), protocols).get(0);
//                            if (!connectionJson.has("read_only_connection_data")) {
//                                connectionJson.set("read_only_connection_data", Json.object());
//                            }
//                            Json otherDataJson = connectionJson.get("read_only_other_data");
//                            switch (baseConnection.getProtocol()) {
//                                case SNMPv3:
//                                    if (!connectionJson.has(SNMPv3ConnectionField.PASSWORD.dbapiName())) {
//                                        connectionJson.set(SNMPv3ConnectionField.PASSWORD.dbapiName(), ((SNMPv3Connection) baseConnection).getPassword());
//                                    }
//                                    if (!otherDataJson.has(SNMPv3ConnectionField.PASSPHRASE.dbapiName())) {
//                                        otherDataJson.set(SNMPv3ConnectionField.PASSPHRASE.dbapiName(), ((SNMPv3Connection) baseConnection).getPassphrase());
//                                    }
//                                    break;
//                                case RemotePowershell:
//                                    if (!connectionJson.has(RemotePowershellConnectionField.PASSWORD.dbapiName())) {
//                                        connectionJson.set(RemotePowershellConnectionField.PASSWORD.dbapiName(), ((RemotePowershellConnection) baseConnection).getPassword());
//                                    }
//                                    break;
//                                case SSH:
//                                    if (!connectionJson.has(SshConnectionField.PASSWORD.dbapiName())) {
//                                        connectionJson.set(SshConnectionField.PASSWORD.dbapiName(), ((SshConnection) baseConnection).getPassword());
//                                    }
//                                    if (!otherDataJson.has(SshConnectionField.PASSPHRASE.dbapiName())) {
//                                        otherDataJson.set(SshConnectionField.PASSPHRASE.dbapiName(), ((SshConnection) baseConnection).getPassphrase());
//                                    }
//                                    if (!otherDataJson.has(SshConnectionField.PRIVATE_KEY.dbapiName())) {
//                                        otherDataJson.set(SshConnectionField.PRIVATE_KEY.dbapiName(), ((SshConnection) baseConnection).getPrivateKey());
//                                    }
//                                    break;
//                                case HTTP:
//                                    if (!connectionJson.has(HttpConnectionField.PASSWORD.dbapiName())) {
//                                        connectionJson.set(HttpConnectionField.PASSWORD.dbapiName(), ((HttpConnection) baseConnection).getPassword());
//                                    }
//                                    // Add the password of the base connection to the stages config
//                                    Map<Integer, HttpConnection.StageConfig> baseStagesConfig = ((HttpConnection) baseConnection).getStagesConfig();
//                                    for (Map.Entry<String, Json> stageConfig : otherDataJson.get(HttpConnectionField.STAGES_CONFIG.dbapiName()).asJsonMap().entrySet()) {
//                                        try {
//                                            String stagePassword = baseStagesConfig.get(Integer.parseInt(stageConfig.getKey())).getPassword();
//                                            if (stagePassword != null) {
//                                                if (!stageConfig.getValue().has(SNMPv3ConnectionField.PASSWORD.dbapiName())) {
//                                                    stageConfig.getValue().set(SNMPv3ConnectionField.PASSWORD.dbapiName(), stagePassword);
//                                                }
//                                            }
//                                        } catch (NullPointerException e) {
//                                            continue;
//                                        }
//                                    }
//                                    // Add the inputs from the base connection
//                                    Map<String, String> baseInputs = ((HttpConnection) baseConnection).getInputs();
//                                    if (!otherDataJson.has(HttpConnectionField.INPUTS.dbapiName())) {
//                                        otherDataJson.set(HttpConnectionField.INPUTS.dbapiName(), Json.object());
//                                    }
//                                    Json inputsJson = otherDataJson.get(HttpConnectionField.INPUTS.dbapiName());
//                                    baseInputs.entrySet().stream()
//                                            .filter(input -> !inputsJson.has(input.getKey()) || !inputsJson.get(input.getKey()).isString() || !inputsJson.get(input.getKey()).asString().isEmpty())
//                                            .forEach(input -> inputsJson.set(input.getKey(), input.getValue()));
//                                    break;
//                                default:
//                                    break;
//
//                            }
//                        } catch (ExecutionException | InterruptedException | TimeoutException | IndexOutOfBoundsException |
//                                 NullPointerException e) {
//                            logger.error("Could not fetch base connection", e);
//                            return JsonApiResponse.error("Could not fetch base connection",
//                                    Json.object("exception", e.getMessage())
//                            ).toJson();
//                        }
//                    }
//
//                    Connection connection = Connection.fromDbapiJson(connectionJson, protocols);
//                    if (ipAddress == null || connection == null) {
//                        return JsonApiResponse.error("Missing required parameter device or connection",
//                                Json.object("exception", "Missing parameter device or connection, of type object")
//                        ).toJson();
//                    } else {
//                        Device device = Device.fromIpAddress(ipAddress);
//                        device.setConnections(List.of(connection));
//                        return scanApiHandler.testConnection(device).toJson();
//                    }
//                },
//                new JsonCmdHelp().setDesc("Test if a new connection is valid")
//                        .setParam("ip_address", "The IP address to test the connection on", IJsonCmdHelp.STR)
//                        .setParam("connection", "The connection to test", IJsonCmdHelp.JSON)
//                        .setParam("base_connection_id", "The id of the base connection to fetch the password from", IJsonCmdHelp.INT)
//                        .setResult("<code>{ \"success\": true, \"result\": { \"value\": \"true/false\" }</code> if the operation went without error, " +
//                                "where result contains \"true\" (as a String) if the connection is valid," +
//                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
//                        .setStatus(IJsonCmdHelp.STATUS_OK)
//        );
        addCmd("fetch_http_connection_stage", params -> {
                    logger.infoReq(LoggerInterfaces.CSL_SERVER,"Starting fetching HTTP connection stage ..." );

                    String ipAddress = JsonUtil.getStringFromJson(params, "ip_address", null);
                    String port = JsonUtil.getStringFromJson(params, "port", null);
                    String username = JsonUtil.getStringFromJson(params, "username", null);
                    String password = JsonUtil.getStringFromJson(params, "password", null);
                    String realm = JsonUtil.getStringFromJson(params, "realm", null);
                    String token = JsonUtil.getStringFromJson(params, "token", null);
                    logger.traceReq(LoggerInterfaces.CSL_SERVER,"Starting fetching HTTP connection stage: ipAddress={} port={} username={} password={} realm={} token={}", ipAddress, port, username, password.substring(0, 3) + "*****", realm, token);

                    Json templateJson = params.get("entity_http_connection");
                    // Use the default values for the headers and query params, thus mark them as usable for CSL-Scan
                    if (templateJson != null && templateJson.has("stages") && templateJson.get("stages").isArray()) {
                        for (Json stage : templateJson.get("stages").asJsonList()) {
                            if (stage.has("headers") && stage.get("headers").isArray()) {
                                stage.get("headers").asJsonList().forEach(header -> header.set("isInput", false));
                            }
                            if (stage.has("queryParams") && stage.get("queryParams").isArray()) {
                                stage.get("queryParams").asJsonList().forEach(header -> header.set("isInput", false));
                            }
                        }
                    }
                    EntityHttpConnection entityHttpConnection = EntityHttpConnection.fromDbapiJson(templateJson);
                    logger.trace(LoggerActions.NULL, LoggerInterfaces.NULL,"Parsing entity HTTP connection : {}", entityHttpConnection);

                    Integer stageIndex;
                    if (params.has("stageIndex")) {
                        Json stageIndexJson = params.get("stageIndex");
                        if (stageIndexJson.isNumber()) {
                            stageIndex = params.get("stageIndex").asInteger();
                        } else if (stageIndexJson.isString()) {
                            stageIndex = Integer.parseInt(stageIndexJson.asString());
                        } else {
                            stageIndex = null;
                        }
                    } else {
                        stageIndex = null;
                    }
                    logger.trace(LoggerActions.NULL, LoggerInterfaces.NULL,"Calculated stage index for entity HTTP connection stage : {}", stageIndex);

                    if (ipAddress == null || port == null || entityHttpConnection == null) {
                        logger.errorResp(LoggerInterfaces.CSL_SERVER,"Failed to fetch entity HTTP connection stage : ip_address, port or stage are needed");
                        return JsonApiResponse.error("Missing required parameter ip_address, port or stage",
                                Json.object("exception", "Missing parameter ip_address, port or stage, of type string, int or object")
                        ).toJson();
                    } else {
                        logger.debugReq(LoggerInterfaces.CSL_SCAN_API, "Fetching HTTP connection stage from ip:{} port:{} user:{} pwd:{} token:{} entityHttpConnection:{} stageIndex:{} ...", ipAddress, port, username, password.substring(0, 3) + "*****", realm, token, stageIndex);
                        JsonApiResponse response = scanApiHandler.fetchHttpConnectionStage(ipAddress, port, username, password, realm, token, entityHttpConnection, stageIndex);
                        logger.debugResp(LoggerInterfaces.CSL_SCAN_API, "Fetched HTTP connection stage from ip:{} port:{} user:{} pwd:{} token:{} entityHttpConnection:{} stageIndex:{} : {}", ipAddress, port, username, password.substring(0, 3) + "*****", realm, token, stageIndex, response);

                        if (response.isSuccess()) {
                            logger.infoResp(LoggerInterfaces.CSL_SERVER,"Successfully fetched entity HTTP connection stage");
                        } else {
                            logger.errorResp(LoggerInterfaces.CSL_SERVER,"Failed to fetch entity HTTP connection stage");
                        }
                        return response.toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Try to fetch the contents of a stage in the Http Connection API")
                        .setParam("stage", "The stage to fetch", IJsonCmdHelp.JSON)
                        .setParam("ip_address", "The IP address to test", IJsonCmdHelp.STR)
                        .setParam("port", "The port to test", IJsonCmdHelp.INT)
                        .setParam("username", "The username to test. Optional.", IJsonCmdHelp.STR)
                        .setParam("password", "The password to test. Optional.", IJsonCmdHelp.STR)
                        .setResult("<code>{ \"success\": true, \"result\": { \"value\": { \"page\": \"...\", \"status\": int }</code> if the operation went without error, " +
                                "where result contains \"true\" (as a String) if the connection is valid," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON),
                JsonCmdPrivilegeFamily.MANAGE_HTTP_TEMPLATES
        );
        addCmd("get_predefined_http_variables", params -> {
            logger.infoReq(LoggerInterfaces.CSL_SERVER, "Getting predefined http variables ...");

            logger.debugReq(LoggerInterfaces.CSL_SCAN_API, "Getting predefined http variables from CSL-Scan API ...");
            JsonApiResponse response = scanApiHandler.getPredefinedHttpVariables();
            logger.debugResp(LoggerInterfaces.CSL_SCAN_API, "Got get predefined http variables from CSL-Scan API");

            if (response.isSuccess()) {
                logger.infoResp(LoggerInterfaces.CSL_SERVER, "Got get predefined http variables");
            } else {
                logger.errorResp(LoggerInterfaces.CSL_SERVER, "Failed to get predefined http variables : {}", response.getError());
            }

            return response.toJson();
                },
                new JsonCmdHelp().setDesc("Get the list of predefined HTTP variables")
                        .setResult("The list of predefined HTTP variables, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.MANAGE_HTTP_TEMPLATES
        );
        addCmd("get_list_of_allowed_libraries_in_http_templates", params -> {
                    logger.infoReq(LoggerInterfaces.CSL_SERVER, "Getting the list of installed NPM packages ...");

                    logger.debugReq(LoggerInterfaces.CSL_SCAN_API, "Getting the list of installed NPM packages from CSL-Scan API ...");
                    JsonApiResponse response = scanApiHandler.getInstalledNpmPackages();
                    logger.debugResp(LoggerInterfaces.CSL_SCAN_API, "Got get the list of installed NPM packages from CSL-Scan API");

                    if (response.isSuccess()) {
                        logger.infoResp(LoggerInterfaces.CSL_SERVER, "Got get the list of installed NPM packages");
                    } else {
                        logger.errorResp(LoggerInterfaces.CSL_SERVER, "Failed to get the list of installed NPM packages : {}", response.getError());
                    }

                    return response.toJson();
                },
                new JsonCmdHelp().setDesc("Get the list of installed NPM packages")
                        .setResult("The list of installed NPM packages, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.MANAGE_HTTP_TEMPLATES
        );
        addCmd("test_http_template", params -> {
            logger.infoReq(LoggerInterfaces.CSL_SERVER, "Testing HTTP template ...");
//                region -- Get Body params from the request
                    String deviceId = JsonUtil.getStringFromJson(params, "device_uuid", null);
                    Integer connectionId = null;
                    if (params.has("connection_id")) {
                        Json connectionIdJson = params.get("connection_id");
                        if (connectionIdJson.isNumber()) {
                            connectionId = connectionIdJson.asInteger();
                        } else if (connectionIdJson.isString()) {
                            connectionId = Integer.parseInt(connectionIdJson.asString());
                        }
                    }
                    String templateId = JsonUtil.getStringFromJson(params, "template_uuid", null);
                    String ipAddress = JsonUtil.getStringFromJson(params, "ip_address", null);
                    Json connectionJson = params.get("connection");
                    Json templateJson = params.get("template");
                    logger.trace(LoggerActions.NULL, LoggerInterfaces.NULL, templateId, ipAddress, connectionJson, templateJson);
//                endregion -- Get Body params

                    // region -- Get Connection Template & Connection Obj (create from data or get by id)

                    // region -- Fetch All discovery protocols (containing the HttpTemplate details from dbapi)
                    EntityHttpConnection entityHttpConnection = null;   // Connection Template
                    Device device = null;
                    Connection connection = null;           // the connection instance from the EntityHttpConnection
                    List<ConnectionProtocol> dbapiDiscoveryProtocols;
                    try {
                        logger.debugReq(LoggerInterfaces.CSL_DBAPI_API, "Fetching discovery protocols from dbapi for testing entity HTTP connection ...");
                        dbapiDiscoveryProtocols = dbapiHandler.fetchDiscoveryProtocols();
                        logger.debugResp(LoggerInterfaces.CSL_DBAPI_API, "Fetched discovery protocols from dbapi for testing entity HTTP connection : {}", dbapiDiscoveryProtocols);
                    } catch (Exception e) {
                        logger.errorResp(LoggerInterfaces.CSL_SERVER, "Could not fetch discovery protocols", e);
                        return JsonApiResponse.error("Could not fetch discovery protocols from DB-API",
                                Json.object("exception", e.getMessage())
                        ).toJson();
                    }
                    // endregion -- Fetch All discovery protocols (containing the HttpTemplate details from dbapi)

                    // region -- Get Connection by id (from dbapi) Or create Connection from jsonData
                    if (connectionJson != null) {
                        connectionJson.set("id", connectionId != null ? connectionId : 0);
                        connectionJson.set("connected_devices", Json.array(deviceId != null ? deviceId : 0));
                        // If the template is provided, use it instead of existing discovery protocols
                        if (templateJson != null) {
                            // Template Does not necessarily exist in dbapi, create a list of fake discovery protocols (len = 1 in this case)
                            dbapiDiscoveryProtocols = List.of(ConnectionProtocol.createFakeConnectionProtocol(templateJson.has("id") ? templateJson.get("id").asString() : null));
                            connectionJson.set("discovery_protocol", dbapiDiscoveryProtocols.get(0).getId());
                        }
                        connection = Connection.fromDbapiJson(connectionJson, dbapiDiscoveryProtocols);
                        logger.debug(LoggerActions.NULL, LoggerInterfaces.NULL,"Parsed connection for testing entity HTTP connection : {}", connection);
                    } else if (connectionId != null) {
                        try {
                            logger.debugReq(LoggerInterfaces.NULL,"Fetching connections for testing entity HTTP connection ...");
                            List<Connection> connections = dbapiHandler.fetchConnections(List.of(connectionId), dbapiDiscoveryProtocols);
                            logger.debugResp(LoggerInterfaces.NULL,"Fetched connections for testing entity HTTP connection : {}", connections);

                            if (!connections.isEmpty()) {
                                connection = connections.get(0);
                            }
                        } catch (Exception e) {
                            logger.errorResp(LoggerInterfaces.CSL_SERVER,"Could not fetch connection from DB-API", e);
                            return JsonApiResponse.error("Could not fetch connection from DB-API",
                                    Json.object("exception", e.getMessage())
                            ).toJson();
                        }
                    } else {
                        logger.warnResp(LoggerInterfaces.CSL_SERVER,"Failed to test http template because connection or connection_id is missing from parameters");
                        return JsonApiResponse.error("Missing required parameter connection or connection_id",
                                Json.object("exception", "Missing parameter connection or connection_id, of type object or int")
                        ).toJson();
                    }
                    // endregion -- Get Connection by id Or create Connection from jsonData

                    // region -- assign connections to the device
                    if (ipAddress != null && connection != null) {
                        device = Device.fromIpAddress(ipAddress).setConnectionsIds(List.of(connectionId != null ? connectionId : 0));
                        device.setConnections(List.of(connection));
                        logger.trace(LoggerActions.NULL, LoggerInterfaces.NULL,"Fetched connections for device ({}) testing HTTP connection : {}", device, connection);
                    } else if (deviceId != null && connection != null) {
                        try {
                            logger.debugReq(LoggerInterfaces.CSL_DBAPI_API,"Fetching devices for entity HTTP connection ...");
                            List<Device> devices = dbapiHandler.fetchDevices(List.of(deviceId));
                            logger.debugResp(LoggerInterfaces.CSL_DBAPI_API,"Fetched devices for entity HTTP connection : {}", devices);

                            if (!devices.isEmpty()) {
                                device = devices.get(0).setConnectionsIds(List.of(connectionId != null ? connectionId : 0));
                                device.setConnections(List.of(connection));
                            }
                        } catch (Exception e) {
                            logger.errorResp(LoggerInterfaces.CSL_SERVER,"Could not fetch device from DB-API", e);
                            return JsonApiResponse.error("Could not fetch device from DB-API",
                                    Json.object("exception", e.getMessage())
                            ).toJson();
                        }
                    }
                    // endregion -- assign connections to the device

                    if (templateJson != null) {
                        entityHttpConnection = EntityHttpConnection.fromDbapiJson(templateJson);
                        logger.trace(LoggerActions.NULL, LoggerInterfaces.NULL,"Parsed entity HTTP connection : {}", entityHttpConnection);
                    }
                    //  endregion -- Get Connection Template & Connection Obj (create from data or get by id)

                    EntityHttpConnectionTestResult result = null;
                    try {
                        logger.debugResp(LoggerInterfaces.CSL_SCAN_API,"Testing entity HTTP connection ...");
                        result = scanApiHandler.testEntityHttpConnection(templateId, entityHttpConnection, deviceId, device, connectionId);
                        logger.debugResp(LoggerInterfaces.CSL_SCAN_API,"Tested entity HTTP connection : {}", result);
                    } catch (Exception e) {
                        logger.errorResp(LoggerInterfaces.CSL_SERVER,"Could not test entity HTTP connection", e);
                        return JsonApiResponse.error("Could not test entity HTTP connection",
                                Json.object("exception", e.getMessage())
                        ).toJson();
                    }
                    if (result == null) {
                        logger.errorResp(LoggerInterfaces.CSL_SERVER, "Could not test entity HTTP connection");
                        return JsonApiResponse.error("Could not test entity HTTP connection",
                                Json.object("exception", "Could not test entity HTTP connection")
                        ).toJson();
                    } else {
                        logger.infoResp(LoggerInterfaces.CSL_SERVER,"Tested entity HTTP connection successfully");
                        return JsonApiResponse.result(result.serializeForDbapi()).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Test an HTTP template")
                        .setParam("ip_address", "The IP address to test the connection on - optional", IJsonCmdHelp.STR)
                        .setParam("device_uuid", "The uuid of the device to test the connection on - optional", IJsonCmdHelp.STR)
                        .setParam("connection_id", "The id of the connection to test - optional", IJsonCmdHelp.INT)
                        .setParam("connection", "The connection to test - optional", IJsonCmdHelp.JSON)
                        .setParam("template_uuid", "The uuid of the template to test", IJsonCmdHelp.STR)
                        .setParam("template", "The template to test - optional", IJsonCmdHelp.JSON)
                        .setResult("<code>{ \"success\": true, \"result\": { \"success\": \"true/false\" }</code> if the operation went without error, " +
                                "where result contains <code>{ \"success\": true }</code> if the template is valid," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON),
                JsonCmdPrivilegeFamily.MANAGE_HTTP_TEMPLATES
        );
        addCmd("get_discovery_cron", params -> {
            logger.infoReq(LoggerInterfaces.CSL_SERVER, "Getting discovery cron ...");
                    try {
                        logger.debugReq(LoggerInterfaces.CSL_SCAN_API, "Getting discovery cron from CSL-Scan ...");
                        Json cron = scanApiHandler.getDiscoveryCron();
                        logger.debugResp(LoggerInterfaces.CSL_SCAN_API, "Got discovery cron from CSL-Scan : {}", cron);

                        if (cron == null) {
                            logger.errorResp(LoggerInterfaces.CSL_SERVER,"Failed to get discovery cron");
                            throw new Exception("Could not fetch discovery cron");
                        }
                        logger.infoResp(LoggerInterfaces.CSL_SERVER,"Got discovery cron successfully");
                        return JsonApiResponse.result(cron).toJson();
                    } catch (Exception e) {
                        logger.errorResp(LoggerInterfaces.CSL_SERVER,"Could not fetch discovery cron", e);
                        return JsonApiResponse.error("Could not fetch discovery cron",
                                Json.object("exception", e.getMessage())
                        ).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Get the discovery cron")
                        .setResult("The discovery cron, in the format <code>{ \"success\": true, \"result\": { \"cron\": \"...\" } }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("set_discovery_cron", params -> {
                    logger.infoReq(LoggerInterfaces.CSL_SERVER, "Setting discovery cron ...");
                    String cron = null;
                    if (params.has("cron") && params.get("cron").isString()) {
                        cron = params.get("cron").asString();
                    }
                    if (cron == null) {
                        logger.errorResp(LoggerInterfaces.CSL_SERVER,"Could not set discovery cron status because cron is missing from params");
                        return JsonApiResponse.error("Missing required parameter cron",
                                Json.object("exception", "Missing parameter cron, of type string")
                        ).toJson();
                    }
                    DynamicDiscoveryFrequencyOption frequencyOption = null;
                    if (params.has("frequencyOption") && params.get("frequencyOption").isString()) {
                        logger.trace("Parsing frequency option {}...", params.get("frequencyOption").asString());
                        frequencyOption = DynamicDiscoveryFrequencyOption.fromDbapiName(params.get("frequencyOption").asString());
                        logger.trace("Parsed frequency option {}=>{}", params.get("frequencyOption").asString(), frequencyOption);
                    }
                    try {
                        logger.debugReq(LoggerInterfaces.CSL_SCAN_API,"Setting Discovery cron with: cron={} and frequencyOptions={} ...", cron, frequencyOption);
                        scanApiHandler.setDiscoveryCron(cron, frequencyOption);
                        logger.debugResp(LoggerInterfaces.CSL_SCAN_API,"Set Discovery cron with cron={} and frequencyOptions={}", cron, frequencyOption);

                        logger.infoResp(LoggerInterfaces.CSL_SERVER,"Discovery cron set : cron={} and frequencyOptions={}", cron, frequencyOption);
                        return JsonApiResponse.success().toJson();
                    } catch (Exception e) {
                        logger.errorResp(LoggerInterfaces.CSL_SERVER,"Could not set discovery cron", e);
                        return JsonApiResponse.error("Could not set discovery cron",
                                Json.object("exception", e.getMessage())
                        ).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Set the discovery cron")
                        .setParam("cron", "The cron to set", IJsonCmdHelp.STR)
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.START_CPE_SCAN
        );
        addCmd("is_discovery_cron_active", params -> {
                    logger.infoReq(LoggerInterfaces.CSL_SERVER, "Checking discovery cron status ...");
                    try {
                        boolean isActive = scanApiHandler.isDiscoveryCronActive();
                        logger.infoResp(LoggerInterfaces.CSL_SERVER,"Checking discovery cron status : {}", isActive);
                        return JsonApiResponse.result(Json.object("isActive", isActive)).toJson();
                    } catch (Exception e) {
                        logger.errorResp(LoggerInterfaces.CSL_SERVER,"Could not fetch discovery cron status", e);
                        return JsonApiResponse.error("Could not fetch discovery cron status",
                                Json.object("exception", e.getMessage())
                        ).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Get the status of the discovery cron")
                        .setResult("The status of the discovery cron, in the format <code>{ \"success\": true, \"result\": { \"active\": \"true/false\" } }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("set_discovery_cron_active", params -> {
                    logger.infoReq(LoggerInterfaces.CSL_SERVER, "Setting discovery cron status ...");
                    Boolean isActive = null;
                    if (params.has("isActive") && params.get("isActive").isBoolean()) {
                        isActive = params.get("isActive").asBoolean();
                    }
                    if (isActive == null) {
                        logger.warnResp(LoggerInterfaces.CSL_SERVER,"Failed to set the discovery cron because the required parameter isActive is missing");
                        return JsonApiResponse.error("Missing required parameter isActive",
                                Json.object("exception", "Missing parameter isActive, of type boolean")
                        ).toJson();
                    }
                    try {
                        logger.infoReq(LoggerInterfaces.CSL_SCAN_API,"Setting discovery cron active in CSL_Scan with status {}", isActive);
                        scanApiHandler.setDiscoveryCronActive(isActive);
                        logger.infoResp(LoggerInterfaces.CSL_SCAN_API,"Set discovery cron active in CSL_Scan with status {}", isActive);

                        logger.infoResp(LoggerInterfaces.CSL_SERVER,"Set discovery cron active : {}", isActive);
                        return JsonApiResponse.success().toJson();
                    } catch (Exception e) {
                        logger.errorResp(LoggerInterfaces.CSL_SERVER,"Could not set discovery cron status", e);
                        return JsonApiResponse.error("Could not set discovery cron status",
                                Json.object("exception", e.getMessage())
                        ).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Set the status of the discovery cron")
                        .setParam("isActive", "The status to set", IJsonCmdHelp.BOOL)
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.START_CPE_SCAN
        );
        addCmd("import_http_templates_bson", params -> {
                    logger.infoReq(LoggerInterfaces.CSL_SERVER, "Importing HTTP templates from bson file ...");

                    HttpTemplateImportNotification query = HttpTemplateImportNotification.fromHMIJson(params);
                    logger.debug(LoggerActions.NULL, LoggerInterfaces.NULL, "Parsed ImportBson task from HMI : {}", query);
                    if (query == null) {
                        logger.warnResp(LoggerInterfaces.CSL_SERVER,"Failed to start new ImportBson task : could not parse BSON file");
                        return JsonApiResponse.error("Could not parse BSON file",
                                Json.object("exception", "Could not parse BSON file")
                        ).toJson();
                    } else {
                        logger.debugReq(LoggerInterfaces.CSL_SCAN_API,"Starting new ImportBson task in CSL-Scan with query={} ...", query);
                        this.importExportBsonService.startNewImportTask(query);
                        logger.debugResp(LoggerInterfaces.CSL_SCAN_API,"Started new ImportBson task in CSL-Scan with query={}", query);

                        logger.infoResp(LoggerInterfaces.CSL_SERVER,"Started new ImportBson task");
                        return JsonApiResponse.success().toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Import HTTP templates from a BSON file")
                        .setParam("file", "The BSON file to import", IJsonCmdHelp.STR)
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error, " +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
        );
        addCmd("export_http_templates_bson", params -> {
                    logger.infoReq(LoggerInterfaces.CSL_SERVER, "Exporting HTTP templates from bson file ...");

                    try {
                        logger.debugReq(LoggerInterfaces.CSL_SCAN_API,"Starting new ExportBson task in CSL-Scan ...");
                        int id = this.importExportBsonService.startNewExportTask();
                        logger.debugResp(LoggerInterfaces.CSL_SCAN_API,"Started new ExportBson task in CSL-Scan");

                        logger.infoResp(LoggerInterfaces.CSL_SERVER,"Started new ExportBson task with id {}", id);
                        return JsonApiResponse.result(Json.object("id", id)).toJson();
                    } catch (Exception e) {
                        logger.warnResp(LoggerInterfaces.CSL_SERVER,"Failed to start new ExportBson task");
                        return JsonApiResponse.error(e.getMessage()).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Request to export HTTP templates to a BSON file")
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error, " +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
        );
        addCmd("get_external_connection_info_templates", params -> {
                    logger.infoReq(LoggerInterfaces.CSL_SERVER, "Getting external connection info templates ...");

                    logger.debugReq(LoggerInterfaces.CSL_SCAN_API,"Getting external connection information templates from CSLScan ...");
                    List<ExternalConnectionInfoTemplate> templates = scanApiHandler.getExternalConnectionInfoTemplates();
                    logger.debugResp(LoggerInterfaces.CSL_SCAN_API,"Got external connection information templates from CSLScan : {}", templates);

                    Json serializedTemplates = Json.array(templates.stream().map(ExternalConnectionInfoTemplate::serializeForDbapi).toArray());
                    logger.trace(LoggerActions.NULL, LoggerInterfaces.NULL, "Serialized external connection information templates : {}", serializedTemplates);

                    logger.infoResp(LoggerInterfaces.CSL_SERVER,"Got external connection information templates from CSLScan");
                    return JsonApiResponse.result(serializedTemplates).toJson();
                },
                new JsonCmdHelp().setDesc("Get the list of device discovery fetcher templates")
                        .setResult("The list of device discovery fetcher templates, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("create_external_connection_info", params -> {
                    logger.infoReq(LoggerInterfaces.CSL_SERVER, "Creating external connection info ...");

                    if (!params.has("connection_info")) {
                        logger.warnResp(LoggerInterfaces.CSL_SERVER, "Missing required parameter connection_info_uuid for creating external connection info");
                        return JsonApiResponse.error("Missing required parameter connection_info",
                                Json.object("exception", "Missing parameter connection_info")
                        ).toJson();
                    }
                    ExternalConnectionInfo connectionInfo = ExternalConnectionInfo.fromHMIJson(params.get("connection_info"));
                    logger.trace(LoggerActions.NULL, LoggerInterfaces.NULL, "Serialized external connection information with connexion info {} for creating : {}", params.get("connection_info"), connectionInfo);

                    if (connectionInfo == null) {
                        logger.warnResp(LoggerInterfaces.CSL_SERVER, "Failed to serialize external connection information with connexion info {} for creating", params.get("connection_info"));
                        return JsonApiResponse.error("Could not parse connection_info",
                                Json.object("exception", "Could not parse connection_info")
                        ).toJson();
                    } else {
                        logger.debugResp(LoggerInterfaces.CSL_SCAN_API, "Creating external connection information with connexion info {} ...", params.get("connection_info"));
                        JsonApiResponse response = scanApiHandler.createExternalConnectionInfo(connectionInfo);
                        logger.debugResp(LoggerInterfaces.CSL_SCAN_API, "Created external connection information with connexion info {} : {}", params.get("connection_info"), response);

                        if (response.isSuccess()) {
                            logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API, "External connection information synchronizing after creating connexion info {} ...", params.get("connection_info"));
                            externalConnectionInfoSynchronizationService.synchronizeExternalConnectionInfos();
                            logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API,  "External connection information synchronized after creating connexion info {}", params.get("connection_info"));

                            logger.infoResp(LoggerInterfaces.CSL_SERVER, "External connection information synchronization after creating connexion info {}", params.get("connection_info"));
                        } else {
                            logger.warnResp(LoggerInterfaces.CSL_SERVER, "Failed to create external connection information with connexion info {}", params.get("connection_info"));
                        }
                        return response.toJson();
                    }
                }, new JsonCmdHelp().setDesc("Create a device discovery connection info")
                        .setParam("connection_info", "The connection info to create", IJsonCmdHelp.JSON)
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.CREATE_EXTERNAL_CONNECTION_INFO
        );
        addCmd("update_external_connection_info", params -> {
                    logger.infoReq(LoggerInterfaces.CSL_SERVER, "Updating external connection info ...");

                    if (!params.has("connection_info")) {
                        logger.warnResp(LoggerInterfaces.CSL_SERVER,"Missing required parameter connection_info_uuid for updating external connection info");
                        return JsonApiResponse.error("Missing required parameter connection_info",
                                Json.object("exception", "Missing parameter connection_info")
                        ).toJson();
                    }
                    ExternalConnectionInfo connectionInfo = ExternalConnectionInfo.fromHMIJson(params.get("connection_info"));
                    logger.trace(LoggerActions.NULL, LoggerInterfaces.NULL, "Serialized external connection information with connexion info {} for updating: {}", params.get("connection_info"), connectionInfo);
                    if (connectionInfo == null) {
                        logger.warnResp(LoggerInterfaces.CSL_SERVER,"Failed to serialize external connection information with connexion info {} for updating", params.get("connection_info"));
                        return JsonApiResponse.error("Could not parse connection_info",
                                Json.object("exception", "Could not parse connection_info")
                        ).toJson();
                    }

                    logger.debugReq(LoggerInterfaces.CSL_SCAN_API,"Updating external connection information with connexion info {} from CSL-Scan ...", params.get("connection_info"));
                    JsonApiResponse response = scanApiHandler.updateExternalConnectionInfo(connectionInfo);
                    logger.debugResp(LoggerInterfaces.CSL_SCAN_API,"Updated external connection information with connexion info {} : {}", params.get("connection_info"), response);

                    if (response.isSuccess()) {
                        logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API, "External connection information synchronizing after updating connexion info {} ...", params.get("connection_info"));
                        externalConnectionInfoSynchronizationService.synchronizeExternalConnectionInfos();
                        logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API,  "External connection information synchronized after updating connexion info {}", params.get("connection_info"));

                        logger.infoResp(LoggerInterfaces.CSL_SERVER,"External connection information synchronization after updating connexion info {}", params.get("connection_info"));
                    } else {
                        logger.warnResp(LoggerInterfaces.CSL_SERVER,"Failed to update external connection information with connexion info {}", params.get("connection_info"));
                    }
                    return response.toJson();
                },
                new JsonCmdHelp().setDesc("Update a device discovery connection info")
                        .setParam("connection_info", "The connection info to update", IJsonCmdHelp.JSON)
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.UPDATE_EXTERNAL_CONNECTION_INFO
        );
        addCmd("delete_external_connection_info", params -> {
                    logger.infoReq(LoggerInterfaces.CSL_SERVER, "Deleting external connection info ...");

                    if (!params.has("connection_info_uuid") || !params.get("connection_info_uuid").isString()) {
                        logger.warnResp(LoggerInterfaces.CSL_SERVER,"Missing required parameter connection_info_uuid for deleting external connection info");
                        return JsonApiResponse.error("Missing required parameter connection_info_uuid",
                                Json.object("exception", "Missing parameter connection_info_uuid")
                        ).toJson();
                    }
                    String connectionInfoId = params.get("connection_info_uuid").asString();

                    logger.debugReq(LoggerInterfaces.CSL_SCAN_API,"Deleting external connection information with connexion info id {} from CSL-Scan ...", connectionInfoId);
                    JsonApiResponse response = scanApiHandler.deleteExternalConnectionInfo(connectionInfoId, false);
                    logger.debugResp(LoggerInterfaces.CSL_SCAN_API,"Deleted external connection information with connexion info id {}  from CSL-Scan: {}", connectionInfoId, response);

                    if (response.isSuccess()) {
                        logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API, "External connection information synchronizing after deleting connexion info {} ...", connectionInfoId);
                        externalConnectionInfoSynchronizationService.synchronizeExternalConnectionInfos();
                        logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API,  "External connection information synchronized after deleting connexion info {}", connectionInfoId);

                        logger.infoResp(LoggerInterfaces.CSL_SERVER,"External connection information synchronization after deleting connexion info id {}", connectionInfoId);
                    } else {
                        logger.warnResp(LoggerInterfaces.CSL_SERVER,"Failed to delete external connection information with connexion info id {}", connectionInfoId);
                    }
                    return response.toJson();
                }, new JsonCmdHelp().setDesc("Remove a device discovery connection info")
                        .setParam("connection_info_uuid", "The id of the connection info to remove", IJsonCmdHelp.STR)
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.DELETE_EXTERNAL_CONNECTION_INFO
        );
        addCmd("clear_external_connection_infos", params -> {
                    logger.infoReq(LoggerInterfaces.CSL_SERVER, "Clearing external connection info ...");
                    try {
                        logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API, "Synchronizing after clearing external connexion info ...");
                        externalConnectionInfoSynchronizationService.clear();
                        logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API,  "Synchronized after clearing external connexion info");

                        logger.infoResp(LoggerInterfaces.CSL_SERVER,"Cleared the collection of external discovery connection infos");
                        return JsonApiResponse.success().toJson();
                    } catch (SynchronizationException e) {
                        logger.warnResp(LoggerInterfaces.CSL_SERVER,"Could not clear the collection of external discovery connection infos");
                        return JsonApiResponse.error("Could not clear the collection of device discovery connection infos",
                                Json.object("exception", e.getMessage())
                        ).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Clear the collection of device discovery connection infos")
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.DELETE_EXTERNAL_CONNECTION_INFO
        );
        addCmd("clear_external_discovered_devices", params -> {
                    logger.infoReq(LoggerInterfaces.CSL_SERVER, "Clearing external discovered devices ...");

                    try {
                        logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API, "Synchronizing after clearing external discovered devices ...");
                        externalDiscoveredDevicesSynchronizationService.clear();
                        logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API,  "Synchronized after clearing external discovered devices");

                        logger.infoResp(LoggerInterfaces.CSL_SERVER,"Cleared the collection of external discovered devices");
                        return JsonApiResponse.success().toJson();
                    } catch (SynchronizationException e) {
                        logger.warnResp(LoggerInterfaces.CSL_SERVER,"Could not clear the collection of external discovered devices");
                        return JsonApiResponse.error("Could not clear the collection of discovered devices",
                                Json.object("exception", e.getMessage())
                        ).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Clear the collection of discovered devices")
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.DELETE_EXTERNAL_DISCOVERED_DEVICE
        );
        addCmd("start_external_scan", params -> {
                    logger.infoReq(LoggerInterfaces.CSL_SERVER, "Starting external scan ...");

                    if (!params.has("connection_info_uuid") || !params.get("connection_info_uuid").isString()) {
                        logger.warn("Missing required parameter connection_info_uuid for device discovery (external) scan");
                        return JsonApiResponse.error("Missing required parameter connection_info_uuid",
                                Json.object("exception", "Missing parameter connection_info_uuid")
                        ).toJson();
                    }
                    String connectionInfoId = params.get("connection_info_uuid").asString();

                    logger.debugReq(LoggerInterfaces.CSL_SCAN_API,"Starting device discovery (external) scan with info uuid {} in CSL-Scan ...", connectionInfoId);
                    ExternalScan scan = externalScansService.startExternalDiscoveryScan(connectionInfoId);
                    logger.debugResp(LoggerInterfaces.CSL_SCAN_API,"Started device discovery (external) scan with info uuid {} in CSL-Scan", connectionInfoId);

                    if (scan == null) {
                        logger.warnResp(LoggerInterfaces.CSL_SERVER,"Could not start device discovery (external) scan");
                        return JsonApiResponse.error("Could not start device discovery scan",
                                Json.object("exception", "Could not start device discovery scan")
                        ).toJson();
                    } else {
                        logger.infoResp(LoggerInterfaces.CSL_SERVER,"Started device discovery (external) scan with uuid {}", scan.getUuid());
                        return JsonApiResponse.result(Json.object("scan_uuid", scan.getUuid())).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Start a device discovery scan")
                        .setParam("connection_info_uuid", "The id of the connection info to use", IJsonCmdHelp.INT)
                        .setResult("<code>{ \"success\": true, \"result\": { \"scan_id\": \"...\" } }</code> if the operation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.START_DEVICE_SCAN
        );

        CSLContext.instance.getStatusNotifier().registerStatusProvider(name, this);

        logger.info("SNMP service operational");
        return true;
    }

    /**
     * Stop the service.
     *
     * @return False.
     */
    @Override
    public boolean terminate() {
        try {
            if (isConcentrator) {
                scanWebSocketHandler.stop();
                dbapiHandler.close();
            }
            scanApiHandler.close();
        } catch (Exception e) {
            logger.warn("Could not stop the service", e);
            return false;
        }
        return false;
    }

    /**
     * Register an API command.
     *
     * @param name            The name of the command.
     * @param cmd             The callback to be executed when the command is invoked.
     * @param privilegeFamily The privilege family of the command.
     * @return A {@link String}
     */
    public String addCmd(String name, IJsonCmd cmd, JsonCmdPrivilegeFamily privilegeFamily) {
        return apiCommands.registerCmd(name, cmd, privilegeFamily);
    }

    /**
     * Register an API command.
     *
     * @param name            The name of the command.
     * @param cmd             The callback to be executed when the command is invoked.
     * @param help            The helper to display in the '/apihelp' page.
     * @param privilegeFamily The privilege family of the command.
     * @return A {@link String}
     */
    public String addCmd(String name, IJsonCmd cmd, IJsonCmdHelp help, JsonCmdPrivilegeFamily privilegeFamily) {
        return apiCommands.registerCmd(name, cmd, help, privilegeFamily);
    }

    public List<CpeItem> getCpeItemChangesSince(OffsetDateTime date) {
        return scanApiHandler.getCpeItemChangesSince(date);
    }

    /**
     * Extract an entity's UUID
     *
     * @param entity The entity of which we will extract the UUID, in {@link Json} format
     * @return The UUID of the entity passed in parameter
     */
    public static String getEntityUuid(Json entity) {
        return JsonUtil.getStringFromJson(entity, "uuid", "");
    }

    /**
     * Get all the detected SNMP objects found.
     *
     * @return A {@link Json} array containing all the SNMP objects discovered so far by the scanner.
     */
    public List<CpeItem> getAllCpes() {
        return scanApiHandler.getCpeItemChangesSince(null);
    }

    /**
     * Get the service of the status.
     *
     * @return A {@link Json} object containing information about the status of sub services.
     */
    public Json getStatus() {
        Json status = Json.object();

        if (scanApiHandler.getStatus().isSuccess()) {
            status.set("is_http_api_reachable", true);
        } else {
            status.set("is_http_api_reachable", false);
        }
        if (isConcentrator) {
            Json websocketStatus = scanWebSocketHandler.getStatus();
            ///logger.debug("Scan websocket check status : {}", websocketStatus);
            boolean requests_ws_status = JsonUtil.getBooleanFromJson(websocketStatus, "is_requests_websocket_connected", false);
            boolean notifications_ws_status = JsonUtil.getBooleanFromJson(websocketStatus, "is_notifications_websocket_connected", false);
            /// logger.debug("Scan websocket check status : {}", websocketStatus);
            status.set("is_websocket_connected", requests_ws_status && notifications_ws_status);
        }

        logger.trace("CSL-Scan status : {}", status);

        return status;
    }

    /**
     * Synchronise the models :
     * - Devices
     * - CPE Items
     */
    public void syncAll() {
        if (isConcentrator) {
            logger.info(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API, "Starting Discovery synchronization");
            dbapiHandler.sendNewDevicesToScanner(scanApiHandler);
            try {
                cpeItemSynchronizationService.syncData();
                logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API, "CPE items synchronization finished");
                microsoftKbSynchronizationService.syncData();
                logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API, "Microsoft KB synchronization finished");
                deletedCpeItemsSynchronizationService.syncData();
                logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API, "Deleted CPE items synchronization finished");
                deletedMicrosoftKbsSynchronizationService.syncData();
                logger.info(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API, "Discovery synchronization finished : CPE items, microsoft KB, deleted CPE items and deleted microsoft KB");
            } catch (SynchronizationException e) {
                logger.warn(LoggerActions.SYNC, LoggerInterfaces.CSL_SCAN_API, "Could not synchronize CPE Items", e);
            }
        }
    }

    /**
     * Synchronize devices between DB-API and CSL-Scan, and then request a new scan from DB-API.
     *
     * @param entities The entities' uuids to scan. May be null, in which case all entities are scanned.
     * @return The result of the scan request, in a {@link JsonApiResponse}
     */
    public JsonApiResponse startScan(List<String> entities) {
        logger.trace("Starting scan fro entities : {}", entities);
        // Synchronize devices between DB-API and CSL-Scan
        JsonApiResponse syncResult = dbapiHandler.sendNewDevicesToScanner(scanApiHandler);
        if (!syncResult.isSuccess()) {
            logger.error("Could not retrieve devices from DB-API, scan aborted.");
            return JsonApiResponse.error(
                    "Could not retrieve devices from DB-API",
                    Json.object("failed_devices", syncResult.getError().getReason())
            );
        }
        logger.debug("Retrieved devices from DB-API for starting scan.");

        // Get deleted CPE Items from DB-API and delete them from CSL-Scan
        JsonApiResponse cpeDeletionResult = JsonApiResponse.success();
        try {
            deletedCpeItemsSynchronizationService.syncData();
            logger.debug("Synchronization of deleted CPE items before scan finished");
        } catch (SynchronizationException e) {
            logger.error("Could not synchronize deleted CPE Items", e);
            cpeDeletionResult = JsonApiResponse.error(
                    "Could not synchronize deleted CPE Items",
                    Json.object("exception", e.getMessage())
            );
        }

        if (!cpeDeletionResult.isSuccess()) {
            return JsonApiResponse.error(
                    "Could not delete CPE Items in CSL-Scan",
                    Json.object("exception", cpeDeletionResult.getError().getDetails().get("exception"))
            );
        }

        if (isConcentrator) {
            return scanWebSocketHandler.requestScan(entities);
        } else {
            return JsonApiResponse.error("Scan WebSocket not in use");
        }
    }
}
