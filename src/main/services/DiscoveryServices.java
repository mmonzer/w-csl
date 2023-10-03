package main.services;

import com.csl.core.CSLContext;
import com.csl.intercom.broker.CSLMqttBrokerHandler;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.cslscan.ScanWebSocketHandler;
import com.csl.intercom.cslscan.models.CpeItem;
import com.csl.intercom.cslscan.models.EntityHttpConnection;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.dbapi.models.Connection;
import com.csl.intercom.dbapi.models.ConnectionProtocol;
import com.csl.intercom.dbapi.models.Device;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.csl.intercom.status.IStatusProvider;
import com.csl.logger.CSLLogger;
import com.csl.util.Pair;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.ICSLService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Service in charge of the SNMP manager microservice.
 * It should expose an API to request a scan and fetch the database.
 * It also allows to know the current status of the requested scans.
 */
public class DiscoveryServices implements ICSLService, IStatusProvider {
    static private final String defaultConfigFileSectionName = "discovery";
    static private final String defaultName = "discovery";

    private final CSLLogger logger = CSLLogger.instance;
    private final IApiCommands apiCommands = new ApiCommandsFactory().createApiCommands("");
    private final String name;
    private final String configFileSectionName;
    private final boolean isConcentrator;
    private ScanWebSocketHandler scanWebSocketHandler = null;
    //    private String apiKey;
    private DbapiHandler dbapiHandler = null;
    private ScanApiHandler scanApiHandler = null;
    private CSLMqttBrokerHandler mqttBroker = null;
    private ScheduledExecutorService synchronizationSchedule;

    public DiscoveryServices(String name, String configFileSectionName, boolean isConcentrator) {
        this.name = name;
        this.configFileSectionName = configFileSectionName;
        this.isConcentrator = isConcentrator;
    }

    public DiscoveryServices() {
        this(defaultName, defaultConfigFileSectionName, true);
    }

    public DiscoveryServices(boolean isConcentrator) {
        this(defaultName, defaultConfigFileSectionName, isConcentrator);
    }


    /**
     * Initialize the service, setting the list of known managers and registering the commands.
     *
     * @param jConfig The configuration of the service (that is, the relevant section of the config file)
     * @param cslDir  The working directory
     * @return True is the initialization was successful, false otherwise
     */
    @Override
    public boolean init(Json jConfig, String cslDir) {
        System.out.println("Initializing SNMP service ..");
        logger.warn("Hello from logger");


        String scanManagerDiscoveryUrl = ScanUtils.generateScanDiscoveryUrlFromConfig(jConfig);

        if (isConcentrator) {
            scanWebSocketHandler = new ScanWebSocketHandler(this, scanManagerDiscoveryUrl);
        }

        dbapiHandler = new DbapiHandler();
        scanApiHandler = new ScanApiHandler();

        Json globalConfig = CSLContext.instance.getConfig().get("global");

        if (isConcentrator) {
            mqttBroker = CSLContext.instance.getMqttBroker();
            mqttBroker.subscribeToTopic(CSLMqttBrokerHandler.Topic.DEVICES, message -> {
                dbapiHandler.sendNewDevicesToScanner(scanApiHandler);
            });
            mqttBroker.subscribeToTopic(CSLMqttBrokerHandler.Topic.CPE_ITEMS, message -> {
                handleDeletedCpes();
            });
        }

        synchronizationSchedule = Executors.newScheduledThreadPool(1);
        synchronizationSchedule.scheduleAtFixedRate(this::syncAll, 0, 300, TimeUnit.SECONDS);

        addCmd("get_status", params -> getStatus(),
                new JsonCmdHelp()
                        .setDesc("Retrieve the status of the service.")
                        .setResult("A status notification: " +
                                "<code>" +
                                "{" +
                                "\"is_http_api_reachable\": true/false" +
                                "\"is_websocket_connected\": true/false" +
                                "}" +
                                "</code>", IJsonCmdHelp.JSON).setStatus(IJsonCmdHelp.STATUS_OK));
//        addCmd("add_entity", this::addEntity);
        addCmd("list_entities", params -> scanApiHandler.listEntities().toJson(),
                new JsonCmdHelp().setDesc("Retieve the entities registered in CSL-Scan")
                        .setResult("The list of entities' information as returned by CSL-Scan, in the format" +
                                "<code>{\"success\": true, \"result\": [...]}</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_entity", params -> scanApiHandler.getEntity(params.get("id").asString()).toJson(),
                new JsonCmdHelp().setDesc("Retrieve a specific entity from CSL-Scan")
                        .setParam("id", "The uuid of the entity to retrieve", IJsonCmdHelp.STR)
                        .setResult("The entity as returned by CSL-Scan", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
//        addCmd("update_entity", this::updateEntity);
        addCmd("delete_entity", params -> scanApiHandler.deleteEntity(params.get("id").asString()).toJson(),
                new JsonCmdHelp().setDesc("Remove a specific entity from CSL-Scan")
                        .setParam("id", "The uuid of the entity to delete", IJsonCmdHelp.STR)
                        .setResult("<code>{ \"success\": true/false }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_all_cpes", params -> Json.object("success", true, "result", Json.array(getAllCpes().toArray())),
                new JsonCmdHelp().setDesc("Get the CPE Items in CSL-Scan")
                        .setResult("The list of CPE Items, in the format <code>{\"success\": true, \"result\": [...]}", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_entity_cpes", params -> scanApiHandler.getEntityCpes(params.get("id").asString()).toJson(),
                new JsonCmdHelp().setDesc("Get an entity's CPE Items")
                        .setParam("id", "The entity's uuid", IJsonCmdHelp.STR)
                        .setResult("The list of CPE Items of the entity, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_cpes_since", params -> Json.object("success", true, "result", Json.array(scanApiHandler.getCpeItemChangesSince(OffsetDateTime.parse(JsonUtil.getStringFromJson(params, "date", null))).toArray())),
                new JsonCmdHelp().setDesc("Retrieve CPE Items that change strictly after a specified date")
                        .setParam("date", "in ISO format, example: 2023-04-13T13:56:56.66 (local date format)", IJsonCmdHelp.STR)
                        .setResult("The list of CPE Items that changed strictly after <code>date</code>, in the format" +
                                "<code>{\"success\": true, \"result\": [...]</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
//        addCmd("global_status", params -> getServiceStatus());
        addCmd("scan_status", params -> scanApiHandler.getScanStatus(params.get("id").asString()).toJson(),
                new JsonCmdHelp().setDesc("Get the status of a specific scan")
                        .setParam("id", "The uuid of the scan to inquire", JsonCmdHelp.STR)
                        .setResult("The status of the scan, in the format <code>{ \"success\": true, \"result\": { ... } }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
//        addCmd("entity_scan_status", params -> getEntityScanStatus(params.get("id").asString()));
        addCmd("start_scan", params -> {
                    List<String> entities = new ArrayList<>();
                    if (params.has("entities")) {
                        for (Json entity : params.get("entities").asJsonList()) {
                            if (entity.isString()) {
                                entities.add(entity.asString());
                            }
                        }
                    }
                    return startScan(entities).toJson();
                },
                new JsonCmdHelp().setDesc("Start a scan from CSL-Scan")
                        .setParam("entities", "An array of strings with the uuids of the entities to scan. May be omitted or null, resulting in scanning all entities.", IJsonCmdHelp.JSON)
                        .setResult("<code>{ \"success\": true }</code> if the scan was started successfully", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("synchronize_devices", params -> dbapiHandler.sendNewDevicesToScanner(scanApiHandler).toJson(),
                new JsonCmdHelp().setDesc("Synchronize devices between DB-API and CSL-Scan.")
                        .setResult("<code>{\"success\": true }</code> if the synchronisation went without error," +
                                "<code>{\"success\": false, \"error\", {\"reason\": \"...\", \"failed_devices\": [...]}}</code> otherwise. The failed_devices field is present if devices were actually fetched from DB-API.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("send_last_cpe_items", params -> {
                    String dateString = JsonUtil.getStringFromJson(params, "date", "");
                    if (!dateString.equals("")) {
                        List<CpeItem> changes;
                        if (dateString.equals("all")) {
                            changes = scanApiHandler.getCpeItemChangesSince(null);
                        } else {
                            changes = scanApiHandler.getCpeItemChangesSince(OffsetDateTime.parse(dateString));
                        }
                        try {
                            dbapiHandler.sendCpeItems(changes);
                        } catch (Exception e) {
                            return JsonApiResponse.error("Could not send changes to DB-API",
                                    Json.object("exception", e.getMessage())
                            ).toJson();
                        }
                    } else {
                        scanApiHandler.sendNewCpeItemsToDbapi(dbapiHandler);
                    }
                    return JsonApiResponse.success().toJson();
                },
                new JsonCmdHelp().setDesc("Trigger a synchronisation of the CPE Items between CSL-Scan and DB-API")
                        .setParam("date", "The date of last CPE Items update on DB-API, in ISO local date format as above." +
                                "May by \"all\" to send all CPE Items to DB-API." +
                                "May also be omitted or null, in which case the date is fetched directly from DB-API.", IJsonCmdHelp.STR)
                        .setResult("<code>{ \"success\": true }</code> if the synchronisation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise." +
                                "The details field should contain the list of failed items if relevant.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("drop_all_collections", params -> {
                    try {
                        scanApiHandler.dropAllCollections();
                        return JsonApiResponse.success().toJson();
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                        return JsonApiResponse.error("Could not drop collections",
                                Json.object("exception", e.getMessage())
                        ).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Drop all collections in DB-API")
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_entity_http_connections", params -> {
                    List<EntityHttpConnection> entityHttpConnections = scanApiHandler.getAllEntityHttpConnections();
                    if (entityHttpConnections == null) {
                        return JsonApiResponse.error("Could not fetch entity HTTP connections from CSL-Scan",
                                Json.object("exception", "Could not fetch entity HTTP connections from CSL-Scan")
                        ).toJson();
                    } else {
                        return JsonApiResponse.result(Json.array(entityHttpConnections.stream().map(EntityHttpConnection::serializeForDbapi).toArray())).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Get all entity HTTP connections from CSL-Scan")
                        .setResult("The list of entity HTTP connections, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_entity_http_connection", params -> {
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
                        return JsonApiResponse.error("Missing required parameter uuid",
                                Json.object("exception", "Missing parameter uuid, of type string or integer")
                        ).toJson();
                    }
                    EntityHttpConnection entityHttpConnection = scanApiHandler.getEntityHttpConnection(uuid);
                    if (entityHttpConnection == null) {
                        return JsonApiResponse.error("Could not fetch entity HTTP connection from CSL-Scan",
                                Json.object("exception", "Could not fetch entity HTTP connection from CSL-Scan")
                        ).toJson();
                    } else {
                        return JsonApiResponse.result(entityHttpConnection.serializeForDbapi()).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Get a specific entity HTTP connection from CSL-Scan")
                        .setParam("uuid", "The uuid of the entity HTTP connection to retrieve", IJsonCmdHelp.STR)
                        .setResult("The entity HTTP connection, in the format <code>{ \"success\": true, \"result\": { ... } }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("delete_entity_http_connection", params -> {
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
                    return scanApiHandler.deleteEntityHttpConnection(uuid).toJson();
                },
                new JsonCmdHelp().setDesc("Delete an EntityHttpConnection from CSL-Scan")
                        .setParam("uuid", "The uuid of the EntityHttpConnection to delete", IJsonCmdHelp.STR)
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("add_entity_http_connection", params -> {
                    Json entityHttpConnectionJson = params.get("entity_http_connection");
                    if (entityHttpConnectionJson == null) {
                        return JsonApiResponse.error("Missing required parameter entity_http_connection",
                                Json.object("exception", "Missing parameter entity_http_connection, of type object")
                        ).toJson();
                    }
                    EntityHttpConnection entityHttpConnection = EntityHttpConnection.fromDbapiJson(entityHttpConnectionJson);
                    if (entityHttpConnection == null) {
                        return JsonApiResponse.error("Could not parse entity_http_connection",
                                Json.object("exception", "Could not parse entity_http_connection")
                        ).toJson();
                    }
                    return scanApiHandler.createOrUpdateEntityHttpConnection(entityHttpConnection).toJson();
                },
                new JsonCmdHelp().setDesc("Add an EntityHttpConnection to CSL-Scan")
                        .setParam("entity_http_connection", "The EntityHttpConnection to add", IJsonCmdHelp.JSON)
                        .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("test_connection", params -> {
                    String deviceUuid = JsonUtil.getStringFromJson(params, "device_uuid", null);
                    String connectionId;
                    Json connectionIdJson = params.get("connection_id");
                    if (connectionIdJson != null && connectionIdJson.isString()) {
                        connectionId = connectionIdJson.asString();
                    } else if (connectionIdJson != null && connectionIdJson.isNumber()) {
                        connectionId = String.valueOf(connectionIdJson.asInteger());
                    } else {
                        connectionId = null;
                    }
                    if (deviceUuid == null || connectionId == null) {
                        return JsonApiResponse.error("Missing required parameter device_uuid or connection_id",
                                Json.object("exception", "Missing parameter device_uuid or connection_uuid, of type string")
                        ).toJson();
                    } else {
                        return scanApiHandler.testConnection(deviceUuid, connectionId).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Test if an existing connection is valid")
                        .setParam("device_uuid", "The uuid of the device to test the connection on", IJsonCmdHelp.STR)
                        .setParam("connection_id", "The id of the connection to test", IJsonCmdHelp.STR)
                        .setResult("<code>{ \"success\": true, \"result\": { \"value\": \"true/false\" }</code> if the operation went without error, " +
                                "where result contains \"true\" (as a String) if the connection is valid," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK));
        addCmd("test_new_connection", params -> {
                    String ipAddress = JsonUtil.getStringFromJson(params, "ip_address", null);
                    Json connectionJson = params.get("connection");
                    connectionJson.set("id", 0);
                    connectionJson.set("connected_devices", Json.array(0));
                    List<ConnectionProtocol> protocols;
                    try {
                        protocols = dbapiHandler.fetchDiscoveryProtocols();
                    } catch (ExecutionException | InterruptedException | TimeoutException e) {
                        throw new RuntimeException(e);
                    }
                    Connection connection = Connection.fromJson(connectionJson, protocols);
                    if (ipAddress == null || connection == null) {
                        return JsonApiResponse.error("Missing required parameter device or connection",
                                Json.object("exception", "Missing parameter device or connection, of type object")
                        ).toJson();
                    } else {
                        Device device = Device.fromIpAddress(ipAddress);
                        device.setConnections(List.of(connection));
                        return scanApiHandler.testConnection(device).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Test if a new connection is valid")
                        .setParam("ip_address", "The IP address to test the connection on", IJsonCmdHelp.STR)
                        .setParam("connection", "The connection to test", IJsonCmdHelp.JSON)
                        .setResult("<code>{ \"success\": true, \"result\": { \"value\": \"true/false\" }</code> if the operation went without error, " +
                                "where result contains \"true\" (as a String) if the connection is valid," +
                                "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK));

        CSLContext.instance.getStatusNotifier().registerStatusProvider(name, this);

        System.out.println("SNMP service operational");
        return true;
    }

    @Override
    public String getConfigFileSectionName() {
        return configFileSectionName;
    }

    @Override
    public IApiCommands getApiCommands() {
        apiCommands.setName(name);
        return apiCommands;
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
            e.printStackTrace(System.err);
        }
        return false;
    }

    /**
     * Register an API command.
     *
     * @param name The name of the command.
     * @param cmd  The callback to be executed when the command is invoked.
     * @return A {@link String}
     */
    public String addCmd(String name, IJsonCmd cmd) {
        return apiCommands.registerCmd(name, cmd);
    }

    /**
     * Register an API command.
     *
     * @param name The name of the command.
     * @param cmd  The callback to be executed when the command is invoked.
     * @param help The helper to display in the '/apihelp' page.
     * @return A {@link String}
     */
    public String addCmd(String name, IJsonCmd cmd, IJsonCmdHelp help) {
        return apiCommands.registerCmd(name, cmd, help);
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
            boolean requests_ws_status = JsonUtil.getBooleanFromJson(websocketStatus, "is_requests_websocket_connected", false);
            boolean notifications_ws_status = JsonUtil.getBooleanFromJson(websocketStatus, "is_notifications_websocket_connected", false);
            status.set("is_websocket_connected", requests_ws_status && notifications_ws_status);
        }

        return status;
    }

    /**
     * Synchronise the models :
     * - Devices
     * - CPE Items
     */
    public void syncAll() {
        dbapiHandler.sendNewDevicesToScanner(scanApiHandler);
        scanApiHandler.sendNewCpeItemsToDbapi(dbapiHandler);
        handleDeletedCpes();
    }

    /**
     * Function to execute when CPE Item deletions are notified.
     * Retrieves the deleted CPE Items from DB-API and requests the deletion to CSL-Scan.
     *
     * @return A {@link Json} with the result of the operation: { "result": "OK" } if the deletion was successful,
     * { "result": "NOK", "error": { "reason": ..., "details": ...}} otherwise.
     */
    private JsonApiResponse handleDeletedCpes() {
        List<Pair<String, OffsetDateTime>> deletedCpes = null;
        try {
            OffsetDateTime lastCpeItemDeletionVerification = scanApiHandler.getLastCpeItemsDeletionDate();
            deletedCpes = dbapiHandler.getDeletedCpeItemsSince(lastCpeItemDeletionVerification);
        } catch (Exception e) {
            return JsonApiResponse.error("Failed to fetch deleted CPE Items",
                    Json.object("exception", e.getMessage())
            );
        }
        scanApiHandler.deleteCpeItemsFromScan(deletedCpes);
        return JsonApiResponse.success();
    }

    /**
     * Synchronize devices between DB-API and CSL-Scan, and then request a new scan from DB-API.
     *
     * @param entities The entities' uuids to scan. May be null, in which case all entities are scanned.
     * @return The result of the scan request, in a {@link JsonApiResponse}
     */
    public JsonApiResponse startScan(List<String> entities) {
        // Synchronize devices between DB-API and CSL-Scan
        JsonApiResponse syncResult = dbapiHandler.sendNewDevicesToScanner(scanApiHandler);
        if (!syncResult.isSuccess()) {
            return JsonApiResponse.error(
                    "Could not retrieve devices from DB-API",
                    Json.object("failed_devices", syncResult.getError().getDetails().get("failed_devices"))
            );
        }

        // Get deleted CPE Items from DB-API and delete them from CSL-Scan
        JsonApiResponse cpeDeletionResult = handleDeletedCpes();
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
