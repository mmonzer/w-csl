package main.services;
import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.intercom.broker.CSLMqttBrokerHandler;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.cslscan.ScanWebSocketHandler;
import com.csl.intercom.cslscan.enums.DynamicDiscoveryFrequencyOption;
import com.csl.intercom.cslscan.models.*;
import com.csl.intercom.cslscan.models.scans.ExternalScan;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.services.ExternalConnectionInfoTemplatesSynchronizationService;
import com.csl.intercom.services.ExternalConnectionInfoSynchronizationService;
import com.csl.intercom.services.ExternalScansService;
import com.csl.intercom.cslscan.models.CpeItem;
import com.csl.intercom.cslscan.models.EntityHttpConnection;
import com.csl.intercom.cslscan.models.EntityHttpConnectionTestResult;
import com.csl.intercom.cslscan.services.ImportExportBsonService;
import com.csl.intercom.dbapi.models.Connection;
import com.csl.intercom.dbapi.models.ConnectionProtocol;
import com.csl.intercom.dbapi.models.Device;
import com.csl.intercom.dbapi.models.HttpTemplateImportNotification;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.csl.intercom.jsoncmd.JsonCmdPrivilegeFamily;
import com.csl.intercom.services.*;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.csl.intercom.status.IStatusProvider;
import com.csl.util.FileStorageService;
import com.csl.util.FileUtils;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.interfaces.IJsonCmdWithFiles;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;
import lombok.Setter;
import main.services.endpoints.DiscoveryEndpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import static com.csl.intercom.cslscan.enums.ScanApiEndpoint.CREATE_CONNECTIONS_DRAFT;

/**
 * Service in charge of the SNMP manager microservice.
 * It should expose an API to request a scan and fetch the database.
 * It also allows to know the current status of the requested scans.
 */
public class DiscoveryServices extends Service implements IStatusProvider {
    static private final String defaultConfigFileSectionName = "discovery";
    static private final String defaultName = "discovery";

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryServices.class);
    private final boolean isRemote;
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
    private DataSynchronizationService connectionInfoSynchronizationService = null;
    @Getter
    @Setter
    private CpeScanService cpeScanService = null;
    private ScheduledExecutorService synchronizationSchedule;

    /**
     * Generic constructor of the Discovery service.
     */
    public DiscoveryServices(String name, String configFileSectionName, boolean isRemote) {
        super(name,
                "Service in charge of the SNMP manager microservice.\n" +
                        "It should expose an API to request a scan and fetch the database.\n" +
                        "It also manages CSL-Scan and the scanning.",
                configFileSectionName);
        this.isRemote = isRemote;
    }

    /**
     * Constructor of the Discovery service with Concentration
     */
    public DiscoveryServices() {
        this(defaultName, defaultConfigFileSectionName, false);
    }

    /**
     * Constructor of the Discovery service with custom concentration
     */
    public DiscoveryServices(boolean isRemote) {
        this(defaultName, defaultConfigFileSectionName, isRemote);
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

        if (!isRemote) {
            cpeScanService = new CpeScanService();
            cpeItemSynchronizationService = new CpeItemsSynchronizationService(scanApiHandler, dbapiHandler, cpeScanService);
            microsoftKbSynchronizationService = new MicrosoftKbSynchronizationService(scanApiHandler, dbapiHandler, cpeScanService);
            deletedCpeItemsSynchronizationService = new DeletedCpeItemsSynchronizationService(scanApiHandler, dbapiHandler);
            deletedMicrosoftKbsSynchronizationService = new DeletedMicrosoftKbsSynchronizationService(scanApiHandler, dbapiHandler);
            connectionInfoSynchronizationService = new ConnectionInfoSynchronizationService();
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
                HttpTemplateImportNotification notification = HttpTemplateImportNotification.fromMQTTMessage(Json.read(message.getResults()));
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
                    logger.trace("Fetching CSL-Scan status");
                    Json response = getStatus();
                    logger.info("CSL-Scan status : {}", response);
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
                    logger.trace("Fetching all CPEs : params={}", params);
                    List<CpeItem> cpeItems = getAllCpes();
                    if (cpeItems == null) {
                        logger.error("Failed to fetch all CPEs");
                        return JsonApiResponse.error("Failed to fetch all CPEs").toJson();
                    } else {
                        logger.info("Successfully fetched all CPEs");
                        return JsonApiResponse.result(Json.array(cpeItems)).toJson();
                    }
                }
                ,
                new JsonCmdHelp().setDesc("Get the CPE Items in CSL-Scan")
                        .setResult("The list of CPE Items, in the format <code>{\"success\": true, \"result\": [...]}", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_entity_cpes", params -> {
                    logger.trace("Fetching entity CPE : params={}", params);
                    String id = params.get("id").asString();
                    logger.debug("Fetching entity CPE : id={}", id);
                    JsonApiResponse response = scanApiHandler.getEntityCpes(id);

                    if (!response.isSuccess()) {
                        logger.error("Failed to fetch entity CPE ({}) : {}", id, response.getError());
                    } else {
                        logger.info("Successfully fetched entity CPE ({})", id);
                    }
                    return response.toJson();
                },
                new JsonCmdHelp().setDesc("Get an entity's CPE Items")
                        .setParam("id", "The entity's uuid", IJsonCmdHelp.STR)
                        .setResult("The list of CPE Items of the entity, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_cpes_since", params -> {
                    logger.trace("Fetching CPEs since : params={}", params);
                    String date = JsonUtil.getStringFromJson(params, "date", null);
                    logger.debug("Fetching CPEs since : date={}", date);
                    List<CpeItem> cpeItems = scanApiHandler.getCpeItemChangesSince(OffsetDateTime.parse(date));

                    if (cpeItems == null) {
                        logger.error("Failed to fetch CPEs since {}", date);
                        return JsonApiResponse.error("Failed to fetch CPEs since " + date).toJson();
                    } else {
                        logger.info("Successfully fetched CPEs since {}", date);
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
                    logger.trace("Fetching scan status : params={}", params);
                    String id = params.get("id").asString();
                    logger.debug("Fetching scan status : id={}", id);
                    JsonApiResponse response = scanApiHandler.getScanStatus(id);
                    if (response.isSuccess()) {
                        logger.info("Successfully fetched scan status {}", id);
                        return response.toJson();
                    } else {
                        logger.error("Failed to fetch scan status ({}): {}", id, response.getError());
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
                    logger.trace("Starting the scan : params= {}", params);
                    List<String> entities = new ArrayList<>();
                    if (params.has("entities")) {
                        for (Json entity : params.get("entities").asJsonList()) {
                            if (entity.isString()) {
                                entities.add(entity.asString());
                            }
                        }
                    }
                    logger.debug("Starting the scan : entities={}", entities);
                    JsonApiResponse response = startScan(entities);
                    if (response.isSuccess()) {
                        logger.info("Successfully started scan");
                        return response.toJson();
                    } else {
                        logger.error("Failed to start the scan : {}", response.getError());
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
                    logger.trace("Stopping the scan");
                    try {
                        this.cpeScanService.cancelScan();
                        logger.info("Successfully stopped scan");
                        return JsonApiResponse.success().toJson();
                    } catch (Exception e) {
                        logger.error("Failed to stop the scan");
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
                    logger.trace("Synchronizing all devices");
                    JsonApiResponse response = dbapiHandler.sendNewDevicesToScanner(scanApiHandler);
                    if (response.isSuccess()) {
                        logger.info("Successfully synchronized all devices");
                        return response.toJson();
                    } else {
                        logger.error("Failed to synchronize all devices : {}", response.getError());
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
                    logger.trace("Dropping all collections");
                    try {
                        scanApiHandler.dropAllCollections();
                        logger.info("Successfully dropped all collections");
                        return JsonApiResponse.success().toJson();
                    } catch (Exception e) {
                        logger.error("Could not drop all collections", e);
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
                    logger.trace("Getting list of entities to HTTP connections : params={}", params);
                    List<EntityHttpConnection> entityHttpConnections = scanApiHandler.getAllEntityHttpConnections(true);
                    if (entityHttpConnections == null) {
                        logger.error("Failed to get list of entities to HTTP connections : entityHttpConnections={}", entityHttpConnections);
                        return JsonApiResponse.error("Could not fetch entity HTTP connections from CSL-Scan",
                                Json.object("exception", "Could not fetch entity HTTP connections from CSL-Scan")
                        ).toJson();
                    } else {
                        logger.info("Successfully got the list of entities to HTTP connections (only visible)");
                        return JsonApiResponse.result(Json.array(entityHttpConnections.stream().map(EntityHttpConnection::serializeForDbapi).toArray())).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Get all entity HTTP connections from CSL-Scan")
                        .setResult("The list of entity HTTP connections, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_entity_http_connections_full", params -> {
                    logger.trace("Getting the list of entities to HTTP connections full : params={}", params);
                    List<EntityHttpConnection> entityHttpConnections = scanApiHandler.getAllEntityHttpConnections(false);
                    if (entityHttpConnections == null) {
                        logger.error("Failed to get the list of entities to HTTP connections full : entityHttpConnections={}", entityHttpConnections);
                        return JsonApiResponse.error("Could not fetch entity HTTP connections from CSL-Scan",
                                Json.object("exception", "Could not fetch entity HTTP connections from CSL-Scan")
                        ).toJson();
                    } else {
                        logger.info("Successfully got the list of entities to HTTP connections full (visible and hidden)");
                        return JsonApiResponse.result(Json.array(entityHttpConnections.stream().map(EntityHttpConnection::serializeForDbapi).toArray())).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Get all entity HTTP connections from CSL-Scan, also showing non-visible stages")
                        .setResult("The list of entity HTTP connections, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.MANAGE_HTTP_TEMPLATES
        );
        addCmd("get_entity_http_connection", params -> {
                    logger.trace("Getting entity of HTTP connection: params={}", params);
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
                    logger.debug("Getting entity of HTTP connection: uuid={}", uuid);

                    if (uuid == null) {
                        logger.error("Failed to get entity to HTTP connection: uuid={}", uuid);
                        return JsonApiResponse.error("Missing required parameter uuid",
                                Json.object("exception", "Missing parameter uuid, of type string or integer")
                        ).toJson();
                    }
                    EntityHttpConnection entityHttpConnection = scanApiHandler.getEntityHttpConnection(uuid, true);
                    if (entityHttpConnection == null) {
                        logger.error("Failed to get entity to HTTP connection: uuid={}", uuid);
                        return JsonApiResponse.error("Could not fetch entity HTTP connection from CSL-Scan",
                                Json.object("exception", "Could not fetch entity HTTP connection from CSL-Scan")
                        ).toJson();
                    } else {
                        logger.info("Successfully got entity to HTTP connection: uuid={}", uuid);
                        return JsonApiResponse.result(entityHttpConnection.serializeForDbapi()).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Get a specific entity HTTP connection from CSL-Scan")
                        .setParam("uuid", "The uuid of the entity HTTP connection to retrieve", IJsonCmdHelp.STR)
                        .setResult("The entity HTTP connection, in the format <code>{ \"success\": true, \"result\": { ... } }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );

        addCmd("get_entity_http_connection_full", params -> {
                    logger.trace("Getting entity to HTTP connection full: params={}", params);
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
                    logger.debug("Getting entity to HTTP connection full: uuid={}", uuid);

                    if (uuid == null) {
                        logger.error("Failed to get entity to HTTP connection full: uuid={}", uuid);
                        return JsonApiResponse.error("Missing required parameter uuid",
                                Json.object("exception", "Missing parameter uuid, of type string or integer")
                        ).toJson();
                    }
                    EntityHttpConnection entityHttpConnection = scanApiHandler.getEntityHttpConnection(uuid, false);
                    if (entityHttpConnection == null) {
                        logger.error("Failed to get entity to HTTP connection full: uuid={}", uuid);
                        return JsonApiResponse.error("Could not fetch entity HTTP connection from CSL-Scan",
                                Json.object("exception", "Could not fetch entity HTTP connection from CSL-Scan")
                        ).toJson();
                    } else {
                        logger.info("Successfully got entity to HTTP connection full (visible and hidden): uuid={}", uuid);
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
                    logger.trace("Delete entity to HTTP connection : params={}", params);
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

                    logger.debug("Fetching entity to HTTP connection to delete: uuid={}", uuid);
                    EntityHttpConnection entityHttpConnection = scanApiHandler.getEntityHttpConnection(uuid, false);
                    logger.debug("Fetched entity to HTTP connection to delete: uuid={} entityHttpConnection={}", uuid, entityHttpConnection);
                    try {
                        JsonApiResponse response = scanApiHandler.deleteEntityHttpConnection(uuid);
                        logger.debug("Fetched entity to HTTP connection to delete: uuid={} uuid={}", uuid, entityHttpConnection);
                        if (response.isSuccess()) {
                            dbapiHandler.deleteDiscoveryProtocol(uuid);
                            logger.debug("Fetched entity to HTTP connection to delete: uuid={} uuid={}", uuid, entityHttpConnection);
                        } else {
                            scanApiHandler.createEntityHttpConnection(entityHttpConnection);
                            logger.debug("Fetched entity to HTTP connection to delete: uuid={} uuid={}", uuid, entityHttpConnection);
                            return response.toJson();
                        }
                    } catch (Exception e) {
                        logger.error("Could not delete entity HTTP connection from CSL-Scan", e);
                        return JsonApiResponse.error("Could not delete entity HTTP connection from CSL-Scan",
                                Json.object("exception", e.getMessage())
                        ).toJson();
                    }
                    return JsonApiResponse.success().toJson();
                },
                new JsonCmdHelp().setDesc("Delete an EntityHttpConnection from CSL-Scan")
                        .setParam("uuid", "The uuid of the EntityHttpConnection to delete", IJsonCmdHelp.STR)
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.MANAGE_HTTP_TEMPLATES
        );
        addCmd("add_entity_http_connection", params -> {
                    logger.trace("Adding entity to HTTP connection : params={}", params);
                    Json entityHttpConnectionJson = params.get("entity_http_connection");
                    EntityHttpConnection entityHttpConnection = EntityHttpConnection.fromDbapiJson(entityHttpConnectionJson);
                    logger.debug("Adding entity to HTTP connection : entityHttpConnection={}", entityHttpConnection);
                    if (entityHttpConnection == null) {
                        logger.error("Failed to add entity to HTTP connection : could not parse entity_http_connection : {}", entityHttpConnectionJson);
                        return JsonApiResponse.error("Could not parse entity_http_connection",
                                Json.object("exception", "Could not parse entity_http_connection")
                        ).toJson();
                    }
                    JsonApiResponse response;
                    if (entityHttpConnection.getUuid() == null) {
                        response = scanApiHandler.createEntityHttpConnection(entityHttpConnection);
                        logger.debug("HTTP connection not found, created entity HTTP connection in CSL-Scan: success={}", response.isSuccess());
                        if (response.isSuccess()) {
                            EntityHttpConnection createdEntityHttpConnection = EntityHttpConnection.fromDbapiJson(response.getResult());
                            logger.trace("Parsed entity HTTP connection to DBapi format: createdEntityHttpConnection={}", createdEntityHttpConnection);
                            try {
                                if (createdEntityHttpConnection == null) {
                                    throw new Exception("Could not parse the created entity_http_connection");
                                }
                                dbapiHandler.createDiscoveryProtocol(createdEntityHttpConnection);
                                logger.info("Created discovery protocol for entity HTTP connection in CSL-Dbapi : entityHttpConnection={}", entityHttpConnection);
                            } catch (Exception e) {
                                scanApiHandler.deleteEntityHttpConnection(createdEntityHttpConnection.getUuid());
                                logger.warn("Could not create discovery protocol in CSL-Dbapi (entityHttpConnection={}), compensated in CSL-Scan", entityHttpConnection, e);
                                response = JsonApiResponse.error("Could not create discovery protocol",
                                        Json.object("exception", e.getMessage())
                                );
                            }
                        }
                    } else {
                        EntityHttpConnection previousEntityHttpConnection = scanApiHandler.getEntityHttpConnection(entityHttpConnection.getUuid(), false);
                        logger.trace("HTTP connection found,fetching entity HTTP connection information in CSL-Scan: previousEntityHttpConnection={}", previousEntityHttpConnection);
                        response = scanApiHandler.updateEntityHttpConnection(entityHttpConnection);
                        logger.debug("HTTP connection found, updating entity HTTP connection in CSL-Scan: success={}", response.isSuccess());
                        try {
                            dbapiHandler.updateDiscoveryProtocol(entityHttpConnection);
                            logger.info("Updated entity HTTP connection in CSL-Dbapi : entityHttpConnection={}", entityHttpConnection);
                        } catch (Exception e) {
                            scanApiHandler.updateEntityHttpConnection(previousEntityHttpConnection);
                            logger.warn("Could not update discovery protocol in CSL-Dbapi (entityHttpConnection={}), compensated in CSL-Scan", entityHttpConnection, e);
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
        addCmd("upload_entity_http_connection_file", (params, files) -> {
                    List<Json> listOfConnections = new ArrayList<>();
                    for (Json file : files) {
                        listOfConnections.addAll(FileUtils.parseConnexionsFromCSV(file.get("content").asString()));
                    }

                    JsonApiResponse response = scanApiHandler.sendPost(CREATE_CONNECTIONS_DRAFT.endpoint(), Json.read(listOfConnections.toString()));
                    if (response.isSuccess()) {
                        externalConnectionInfoSynchronizationService.synchronizeExternalConnectionInfos();
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
                    String deviceUuid = JsonUtil.getStringFromJson(params, "device_uuid", null);
                    logger.trace("Testing HTTP connection : deviceUuid={}", deviceUuid);
                    String connectionId;
                    String connectionUuid;
                    Json connectionUuidJson = params.get("connection_uuid");
                    Json connectionIdJson = params.get("connection_id");
                    logger.debug("Testing HTTP connection : deviceUuid={}  connectionId={}", deviceUuid, connectionIdJson);
                    if (connectionIdJson != null && connectionIdJson.isString()) {
                        connectionId = connectionIdJson.asString();
                    } else if (connectionIdJson != null && connectionIdJson.isNumber()) {
                        connectionId = String.valueOf(connectionIdJson.asInteger());
                    } else {
                        connectionId = null;
                    }
                    logger.trace("Testing HTTP connection : parsed connection id : connectionId={}", connectionId);
                    if (deviceUuid == null || connectionId == null) {
                        logger.warn("Testing HTTP connection failed: required not null deviceUuid={}  connectionId={}", deviceUuid, connectionIdJson);
                        return JsonApiResponse.error("Missing required parameter device_uuid or connection_id",
                                Json.object("exception", "Missing parameter device_uuid or connection_uuid, of type string")
                        ).toJson();
                    }
                    if (connectionUuidJson != null && connectionUuidJson.isString()) {

                        connectionUuid = connectionUuidJson.asString();
                    } else {
                        connectionUuid = null;
                    }
                    JsonApiResponse response = scanApiHandler.testConnection(deviceUuid, connectionId);
                    logger.trace("Tested HTTP connection (deviceUuid={}  connectionId={}) : {}", deviceUuid, connectionIdJson, response);
                    return response.toJson();
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
//                            Connection baseConnection = dbapiHandler.fetchConnections(List.of(baseConnectionIdJson.asString()), protocols).get(0);
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
                    String ipAddress = JsonUtil.getStringFromJson(params, "ip_address", null);
                    String port = JsonUtil.getStringFromJson(params, "port", null);
                    String username = JsonUtil.getStringFromJson(params, "username", null);
                    String password = JsonUtil.getStringFromJson(params, "password", null);
                    String realm = JsonUtil.getStringFromJson(params, "realm", null);
                    String token = JsonUtil.getStringFromJson(params, "token", null);
                    logger.trace("Starting fetching HTTP connection stage: ipAddress={} port={} username={} password={} realm={} token={}", ipAddress, port, username, password.substring(0, 3) + "*****", realm, token);

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
                    logger.debug("Parsing entity HTTP connection : {}", entityHttpConnection);

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
                    logger.debug("Calculated stage index for entity HTTP connection stage : {}", stageIndex);

                    if (ipAddress == null || port == null || entityHttpConnection == null) {
                        logger.error("Failed to fetch entity HTTP connection stage : ip_address, port or stage are needed");
                        return JsonApiResponse.error("Missing required parameter ip_address, port or stage",
                                Json.object("exception", "Missing parameter ip_address, port or stage, of type string, int or object")
                        ).toJson();
                    } else {
                        JsonApiResponse response = scanApiHandler.fetchHttpConnectionStage(ipAddress, port, username, password, realm, token, entityHttpConnection, stageIndex);
                        if (response.isSuccess()) {
                            logger.info("Successfully fetched entity HTTP connection stage");
                        } else {
                            logger.error("Failed to fetch entity HTTP connection stage");
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
        addCmd("get_predefined_http_variables", params -> scanApiHandler.getPredefinedHttpVariables().toJson(),
                new JsonCmdHelp().setDesc("Get the list of predefined HTTP variables")
                        .setResult("The list of predefined HTTP variables, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.MANAGE_HTTP_TEMPLATES
        );
        addCmd("get_list_of_allowed_libraries_in_http_templates", params -> scanApiHandler.getInstalledNpmPackages().toJson(),
                new JsonCmdHelp().setDesc("Get the list of installed NPM packages")
                        .setResult("The list of installed NPM packages, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK),
                JsonCmdPrivilegeFamily.MANAGE_HTTP_TEMPLATES
        );
        addCmd("test_http_template", params -> {
//                region -- Get Body params from the request
                    String deviceId = JsonUtil.getStringFromJson(params, "device_uuid", null);
                    String connectionId = null;
                    if (params.has("connection_id")) {
                        Json connectionIdJson = params.get("connection_id");
                        if (connectionIdJson.isNumber()) {
                            connectionId = String.valueOf(connectionIdJson.asInteger());
                        } else if (connectionIdJson.isString()) {
                            connectionId = connectionIdJson.asString();
                        }
                    }
                    String templateId = JsonUtil.getStringFromJson(params, "template_uuid", null);
                    String ipAddress = JsonUtil.getStringFromJson(params, "ip_address", null);
                    Json connectionJson = params.get("connection");
                    Json templateJson = params.get("template");
                    logger.debug("Starting testing entity HTTP connection : templateId={} ipAddress={} connectionJson={} templateJson={}", templateId, ipAddress, connectionJson, templateJson);
//                endregion -- Get Body params

                    // region -- Get Connection Template & Connection Obj (create from data or get by id)

                    // region -- Fetch All discovery protocols (containing the HttpTemplate details from dbapi)
                    EntityHttpConnection entityHttpConnection = null;   // Connection Template
                    Device device = null;
                    Connection connection = null;           // the connection instance from the EntityHttpConnection
                    List<ConnectionProtocol> dbapiDiscoveryProtocols;
                    try {
                        dbapiDiscoveryProtocols = dbapiHandler.fetchDiscoveryProtocols();
                        logger.debug("Fetched discovery protocols from dbapi for testing entity HTTP connection : {}", dbapiDiscoveryProtocols);
                    } catch (Exception e) {
                        logger.error("Could not fetch discovery protocols", e);
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
                        logger.debug("Parsed connection for testing entity HTTP connection : {}", connection);
                    } else if (connectionId != null) {
                        try {
                            List<Connection> connections = dbapiHandler.fetchConnections(List.of(connectionId), dbapiDiscoveryProtocols);
                            if (!connections.isEmpty()) {
                                connection = connections.get(0);
                            }
                            logger.debug("Fetched connections for testing entity HTTP connection : {}", connections);
                        } catch (Exception e) {
                            logger.error("Could not fetch connection from DB-API", e);
                            return JsonApiResponse.error("Could not fetch connection from DB-API",
                                    Json.object("exception", e.getMessage())
                            ).toJson();
                        }
                    } else {
                        logger.warn("Failed to test http template because connection or connection_id is missing from parameters");
                        return JsonApiResponse.error("Missing required parameter connection or connection_id",
                                Json.object("exception", "Missing parameter connection or connection_id, of type object or int")
                        ).toJson();
                    }
                    // endregion -- Get Connection by id Or create Connection from jsonData

                    // region -- assign connections to the device
                    if (ipAddress != null && connection != null) {
                        device = Device.fromIpAddress(ipAddress).setConnectionsIds(List.of(connectionId != null ? connectionId : "0"));
                        device.setConnections(List.of(connection));
                        logger.debug("Fetched connections for device ({}) testing HTTP connection : {}", device, connection);
                    } else if (deviceId != null && connection != null) {
                        try {
                            List<Device> devices = dbapiHandler.fetchDevices(List.of(deviceId));
                            if (!devices.isEmpty()) {
                                device = devices.get(0).setConnectionsIds(List.of(connectionId != null ? connectionId : "0"));
                                device.setConnections(List.of(connection));
                            }
                            logger.debug("Fetched devices for entity HTTP connection : {}", devices);
                        } catch (Exception e) {
                            logger.error("Could not fetch device from DB-API", e);
                            return JsonApiResponse.error("Could not fetch device from DB-API",
                                    Json.object("exception", e.getMessage())
                            ).toJson();
                        }
                    }
                    // endregion -- assign connections to the device

                    if (templateJson != null) {
                        entityHttpConnection = EntityHttpConnection.fromDbapiJson(templateJson);
                        logger.debug("Parsed entity HTTP connection : {}", entityHttpConnection);
                    }
                    //  endregion -- Get Connection Template & Connection Obj (create from data or get by id)

                    EntityHttpConnectionTestResult result = null;
                    try {
                        result = scanApiHandler.testEntityHttpConnection(templateId, entityHttpConnection, deviceId, device, connectionId);
                        logger.debug("Tested entity HTTP connection : {}", result);
                    } catch (Exception e) {
                        logger.error("Could not test entity HTTP connection", e);
                        return JsonApiResponse.error("Could not test entity HTTP connection",
                                Json.object("exception", e.getMessage())
                        ).toJson();
                    }
                    if (result == null) {
                        logger.error("Could not test entity HTTP connection");
                        return JsonApiResponse.error("Could not test entity HTTP connection",
                                Json.object("exception", "Could not test entity HTTP connection")
                        ).toJson();
                    } else {
                        logger.info("Tested entity HTTP connection successfully");
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
                    try {
                        Json cron = scanApiHandler.getDiscoveryCron();
                        if (cron == null) {
                            logger.error("Failed to get discovery cron");
                            throw new Exception("Could not fetch discovery cron");
                        }
                        logger.info("Got discovery cron : {}", cron);
                        return JsonApiResponse.result(cron).toJson();
                    } catch (Exception e) {
                        logger.error("Could not fetch discovery cron", e);
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
                    String cron = null;
                    if (params.has("cron") && params.get("cron").isString()) {
                        cron = params.get("cron").asString();
                    }
                    if (cron == null) {
                        logger.error("Could not set discovery cron status because cron is missing from params");
                        return JsonApiResponse.error("Missing required parameter cron",
                                Json.object("exception", "Missing parameter cron, of type string")
                        ).toJson();
                    }
                    DynamicDiscoveryFrequencyOption frequencyOption = null;
                    if (params.has("frequencyOption") && params.get("frequencyOption").isString()) {
                        logger.trace("Parsing frequency option {}...", params.get("frequencyOption").asString());
                        frequencyOption = DynamicDiscoveryFrequencyOption.fromDbapiName(params.get("frequencyOption").asString());
                        logger.debug("Parsed frequency option {}=>{}", params.get("frequencyOption").asString(), frequencyOption);
                    }
                    try {
                        scanApiHandler.setDiscoveryCron(cron, frequencyOption);
                        logger.info("Discovery cron set : cron={} and frequencyOptions={}", cron, frequencyOption);
                        return JsonApiResponse.success().toJson();
                    } catch (Exception e) {
                        logger.error("Could not set discovery cron", e);
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
                    try {
                        boolean isActive = scanApiHandler.isDiscoveryCronActive();
                        logger.info("Discovery cron status : {}", isActive);
                        return JsonApiResponse.result(Json.object("isActive", isActive)).toJson();
                    } catch (Exception e) {
                        logger.error("Could not fetch discovery cron status", e);
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
                    Boolean isActive = null;
                    if (params.has("isActive") && params.get("isActive").isBoolean()) {
                        isActive = params.get("isActive").asBoolean();
                    }
                    if (isActive == null) {
                        logger.warn("Failed to set the discovery cron because the required parameter isActive is missing");
                        return JsonApiResponse.error("Missing required parameter isActive",
                                Json.object("exception", "Missing parameter isActive, of type boolean")
                        ).toJson();
                    }
                    try {
                        scanApiHandler.setDiscoveryCronActive(isActive);
                        logger.info("Set discovery cron active : {}", isActive);
                        return JsonApiResponse.success().toJson();
                    } catch (Exception e) {
                        logger.error("Could not set discovery cron status", e);
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
                    HttpTemplateImportNotification query = HttpTemplateImportNotification.fromHMIJson(params);
                    logger.debug("Parsed ImportBson task from HMI : {}", query);
                    if (query == null) {
                        logger.warn("Failed to start new ImportBson task : could not parse BSON file");
                        return JsonApiResponse.error("Could not parse BSON file",
                                Json.object("exception", "Could not parse BSON file")
                        ).toJson();
                    } else {
                        this.importExportBsonService.startNewImportTask(query);
                        logger.info("Started new ImportBson task");
                        return JsonApiResponse.success().toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Import HTTP templates from a BSON file")
                        .setParam("file", "The BSON file to import", IJsonCmdHelp.STR)
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error, " +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
        );
        addCmd("export_http_templates_bson", params -> {
                    try {
                        int id = this.importExportBsonService.startNewExportTask();
                        logger.info("Started new ExportBson task with id {}", id);
                        return JsonApiResponse.result(Json.object("id", id)).toJson();
                    } catch (Exception e) {
                        logger.warn("Failed to start new ExportBson task");
                        return JsonApiResponse.error(e.getMessage()).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Request to export HTTP templates to a BSON file")
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error, " +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
        );
        addCmd("get_external_connection_info_templates", params -> {
                    List<ExternalConnectionInfoTemplate> templates = scanApiHandler.getExternalConnectionInfoTemplates();
                    logger.debug("Got external connection information templates from CSLScan : {}", templates);
                    Json serializedTemplates = Json.array(templates.stream().map(ExternalConnectionInfoTemplate::serializeForDbapi).toArray());
                    logger.trace("Serialized external connection information templates : {}", serializedTemplates);
                    logger.info("Got external connection information templates from CSLScan");
                    return JsonApiResponse.result(serializedTemplates).toJson();
                },
                new JsonCmdHelp().setDesc("Get the list of device discovery fetcher templates")
                        .setResult("The list of device discovery fetcher templates, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("create_external_connection_info", params -> {
                    if (!params.has("connection_info")) {
                        logger.warn("Missing required parameter connection_info_uuid for creating external connection info");
                        return JsonApiResponse.error("Missing required parameter connection_info",
                                Json.object("exception", "Missing parameter connection_info")
                        ).toJson();
                    }
                    ExternalConnectionInfo connectionInfo = ExternalConnectionInfo.fromHMIJson(params.get("connection_info"));
                    logger.debug("Serialized external connection information with connexion info {} for creating : {}", params.get("connection_info"), connectionInfo);
                    if (connectionInfo == null) {
                        logger.warn("Failed to serialize external connection information with connexion info {} for creating", params.get("connection_info"));
                        return JsonApiResponse.error("Could not parse connection_info",
                                Json.object("exception", "Could not parse connection_info")
                        ).toJson();
                    } else {
                        JsonApiResponse response = scanApiHandler.createExternalConnectionInfo(connectionInfo);
                        if (response.isSuccess()) {
                            externalConnectionInfoSynchronizationService.synchronizeExternalConnectionInfos();
                            logger.info("External connection information synchronization after creating connexion info {}", params.get("connection_info"));
                        } else {
                            logger.warn("Failed to create external connection information with connexion info {}", params.get("connection_info"));
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
                    if (!params.has("connection_info")) {
                        logger.warn("Missing required parameter connection_info_uuid for updating external connection info");
                        return JsonApiResponse.error("Missing required parameter connection_info",
                                Json.object("exception", "Missing parameter connection_info")
                        ).toJson();
                    }
                    ExternalConnectionInfo connectionInfo = ExternalConnectionInfo.fromHMIJson(params.get("connection_info"));
                    logger.debug("Serialized external connection information with connexion info {} for updating: {}", params.get("connection_info"), connectionInfo);
                    if (connectionInfo == null) {
                        logger.warn("Failed to serialize external connection information with connexion info {} for updating", params.get("connection_info"));
                        return JsonApiResponse.error("Could not parse connection_info",
                                Json.object("exception", "Could not parse connection_info")
                        ).toJson();
                    }
                    JsonApiResponse response = scanApiHandler.updateExternalConnectionInfo(connectionInfo);
                    logger.debug("Updated external connection information with connexion info {} : {}", params.get("connection_info"), response);
                    if (response.isSuccess()) {
                        externalConnectionInfoSynchronizationService.synchronizeExternalConnectionInfos();
                        logger.info("External connection information synchronization after updating connexion info {}", params.get("connection_info"));
                    } else {
                        logger.warn("Failed to update external connection information with connexion info {}", params.get("connection_info"));
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
                    if (!params.has("connection_info_uuid") || !params.get("connection_info_uuid").isString()) {
                        logger.warn("Missing required parameter connection_info_uuid for deleting external connection info");
                        return JsonApiResponse.error("Missing required parameter connection_info_uuid",
                                Json.object("exception", "Missing parameter connection_info_uuid")
                        ).toJson();
                    }
                    String connectionInfoId = params.get("connection_info_uuid").asString();
                    JsonApiResponse response = scanApiHandler.deleteExternalConnectionInfo(connectionInfoId, false);
                    logger.debug("Deleted external connection information with connexion info id {} : {}", connectionInfoId, response);
                    if (response.isSuccess()) {
                        externalConnectionInfoSynchronizationService.synchronizeExternalConnectionInfos();
                        logger.info("External connection information synchronization after deleting connexion info id {}", connectionInfoId);
                    } else {
                        logger.warn("Failed to delete external connection information with connexion info id {}", connectionInfoId);
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
                    try {
                        externalConnectionInfoSynchronizationService.clear();
                        logger.info("Cleared the collection of external discovery connection infos");
                        return JsonApiResponse.success().toJson();
                    } catch (SynchronizationException e) {
                        logger.warn("Could not clear the collection of external discovery connection infos");
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
                    try {
                        externalDiscoveredDevicesSynchronizationService.clear();
                        logger.info("Cleared the collection of external discovered devices");
                        return JsonApiResponse.success().toJson();
                    } catch (SynchronizationException e) {
                        logger.warn("Could not clear the collection of external discovered devices");
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
                    if (!params.has("connection_info_uuid") || !params.get("connection_info_uuid").isString()) {
                        logger.warn("issing required parameter connection_info_uuid for device discovery (external) scan");
                        return JsonApiResponse.error("Missing required parameter connection_info_uuid",
                                Json.object("exception", "Missing parameter connection_info_uuid")
                        ).toJson();
                    }
                    String connectionInfoId = params.get("connection_info_uuid").asString();
                    logger.debug("Starting device discovery (external) scan with info uuid {}", connectionInfoId);
                    ExternalScan scan = externalScansService.startExternalDiscoveryScan(connectionInfoId);
                    if (scan == null) {
                        logger.warn("Could not start device discovery (external) scan");
                        return JsonApiResponse.error("Could not start device discovery scan",
                                Json.object("exception", "Could not start device discovery scan")
                        ).toJson();
                    } else {
                        logger.warn("Started device discovery (external) scan with uuid {}", scan.getUuid());
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

        addCmd(DiscoveryEndpoints.ADD_CONNECTION, params -> {
            Json connectionJson = params.get("connection");
            Connection connection = null;
            try {
                connection = Connection.fromHMIJson(connectionJson, dbapiHandler.fetchDiscoveryProtocols());
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                logger.error("Could not fetch discovery protocols", e);
                return JsonApiResponse.error("Could not fetch discovery protocols",
                        Json.object("exception", e.getMessage())
                ).toJson();
            }
            if (connection == null) {
                return JsonApiResponse.error("Could not parse connection",
                        Json.object("exception", "Could not parse connection")
                ).toJson();
            }
            JsonApiResponse response;
            try {
                response = scanApiHandler.addConnectionInfo(connection);
                if (response.isSuccess()) {
                    // add connection info to dbapi
                    try {
                        String connectionUuid = response.getResult().get("uuid").asString();
                        connection.setUuid(connectionUuid);
                        String discoveryProtocolName = null;
                        if (connectionJson.get("discovery_protocol_name") != null && connectionJson.get("discovery_protocol_name").getValue() != null) {
                            discoveryProtocolName = (String) connectionJson.get("discovery_protocol_name").getValue();
                        }
                        dbapiHandler.createConnection(connection, discoveryProtocolName, connectionJson);
                    } catch (Exception e) {
                        logger.error("Could not add connection info to dbapi", e);
                        // remove connection info from scan
                        scanApiHandler.deleteEntity(connection.getUuid());
                        return JsonApiResponse.error("Could not add connection info to dbapi",
                                Json.object("exception", e.getMessage())
                        ).toJson();
                    }

                } else {
                    logger.error("Could not add connection info, {}", response.getError().toString());
                }
            } catch (Exception e) {
                logger.error("Could not add connection info", e);
                response = JsonApiResponse.error("Could not add connection info",
                        Json.object("exception", e.getMessage())
                );
            }
            return response.toJson();
        });

        addCmd(DiscoveryEndpoints.DELETE_CONNECTION, params -> {
            String connectionUuid = JsonUtil.getStringFromJson(params, "mongo_entity_id", null);
            if (connectionUuid == null) {
                return JsonApiResponse.error("Missing required parameter connection_uuid",
                        Json.object("exception", "Missing parameter connection_uuid")
                ).toJson();
            }
            JsonApiResponse response;
            try {
                response = scanApiHandler.deleteConnectionInfo(connectionUuid);
                if (response.isSuccess()) {
                    // delete connection info from dbapi
                    try {
                        dbapiHandler.deleteConnection(connectionUuid);
                    } catch (Exception e) {
                        logger.error("Could not delete connection info from dbapi", e);
                    }
                } else {
                    logger.error("Could not delete connection info, {}", response.getError().toString());
                }
            } catch (Exception e) {
                logger.error("Could not delete connection info", e);
                response = JsonApiResponse.error("Could not delete connection info",
                        Json.object("exception", e.getMessage())
                );
            }
            return response.toJson();
        });

        addCmd(DiscoveryEndpoints.UPDATE_CONNECTION, params -> {
            Json connectionJson = params.get("connection");
            Connection connection = null;
            try {
                connection = Connection.fromHMIJson(connectionJson, dbapiHandler.fetchDiscoveryProtocols());
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                logger.error("Could not fetch discovery protocols", e);
                return JsonApiResponse.error("Could not fetch discovery protocols",
                        Json.object("exception", e.getMessage())
                ).toJson();
            }
            if (connection == null) {
                return JsonApiResponse.error("Could not parse connection",
                        Json.object("exception", "Could not parse connection")
                ).toJson();
            }
            JsonApiResponse response;
            try {
                response = scanApiHandler.updateConnectionInfo(connection);
                if (response.isSuccess()) {
                    // update connection info in dbapi
                    try {
                        dbapiHandler.updateConnection(connection, connectionJson);
                    } catch (Exception e) {
                        logger.error("Could not update connection info in dbapi", e);
                        return JsonApiResponse.error("Could not update connection info in dbapi",
                                Json.object("exception", e.getMessage())
                        ).toJson();
                    }
                } else {
                    logger.error("Could not update connection info, {}", response.getError().toString());
                }
            } catch (Exception e) {
                logger.error("Could not update connection info", e);
                response = JsonApiResponse.error("Could not update connection info",
                        Json.object("exception", e.getMessage())
                );
            }
            return response.toJson();
        });

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
            if (!isRemote) {
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

    /**
     * Register an API command.
     *
     * @param name            The name of the command.
     * @param cmd             The callback to be executed when the command is invoked.
     * @param privilegeFamily The privilege family of the command.
     * @return A {@link String}
     */
    public String addCmd(String name, IJsonCmdWithFiles cmd, JsonCmdPrivilegeFamily privilegeFamily) {
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
    public String addCmd(String name, IJsonCmdWithFiles cmd, IJsonCmdHelp help, JsonCmdPrivilegeFamily privilegeFamily) {
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
        if (!isRemote) {
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
        if (!isRemote) {
            dbapiHandler.sendNewDevicesToScanner(scanApiHandler);
            try {
                connectionInfoSynchronizationService.syncData();
                cpeItemSynchronizationService.syncData();
                logger.trace("CPE items synchronization finished");
                microsoftKbSynchronizationService.syncData();
                logger.trace("Microsoft KB synchronization finished");
                deletedCpeItemsSynchronizationService.syncData();
                logger.trace("Deleted CPE items synchronization finished");
                deletedMicrosoftKbsSynchronizationService.syncData();
                logger.debug("Discovery synchronization finished : CPE items, microsoft KB, deleted CPE items and deleted microsoft KB");
            } catch (SynchronizationException e) {
                logger.warn("Could not synchronize CPE Items", e);
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

        if (!isRemote) {
            return scanWebSocketHandler.requestScan(entities);
        } else {
            return JsonApiResponse.error("Scan WebSocket not in use");
        }
    }
}
