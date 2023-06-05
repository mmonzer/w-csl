package main.services;

import com.csl.core.CSLContext;
import com.csl.intercom.broker.CSLMqttBrokerHandler;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.ScanWebSocketHandler;
import com.csl.intercom.cslscan.models.CpeItem;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.dbapi.DbapiUtils;
import com.csl.intercom.dbapi.models.Connection;
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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
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
    private OffsetDateTime lastCpeItemDeletionVerification = null;
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

        String scanManagerIp = JsonUtil.getStringFromJson(jConfig, "manager_ip", "localhost");
        int scanManagerPort = JsonUtil.getIntFromJson(jConfig, "manager_port", 8010);

        String scanManagerProtocol = JsonUtil.getStringFromJson(jConfig, "manager_protocol", "http");
        String scanManagerDiscoveryUrl;
        if ("https".equals(scanManagerProtocol)) {
            scanManagerDiscoveryUrl = "wss://" + scanManagerIp + ":" + scanManagerPort + "/csl-scan/";
        } else {
            scanManagerDiscoveryUrl = "ws://" + scanManagerIp + ":" + scanManagerPort + "/csl-scan/";
        }
        String scanManagerApiUrl = scanManagerProtocol + "://" + scanManagerIp + ":" + scanManagerPort + "/api";
        if (isConcentrator) {
            scanWebSocketHandler = new ScanWebSocketHandler(this, scanManagerDiscoveryUrl);
        }

        dbapiHandler = new DbapiHandler();
        scanApiHandler = new ScanApiHandler(scanManagerApiUrl);

        Json globalConfig = CSLContext.instance.getConfig().get("global");

        if (isConcentrator) {
            mqttBroker = CSLContext.instance.getMqttBroker();
            mqttBroker.subscribeToTopic(CSLMqttBrokerHandler.Topic.DEVICES, message -> {
                System.out.println("[DEBUG " + LocalDateTime.now() + "] Received device change notification");
                handleDbapiDeviceChange();
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
        addCmd("synchronize_devices", params -> handleDbapiDeviceChange().toJson(),
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
                        handleCpeItemChanges();
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
        handleDbapiDeviceChange();
        handleCpeItemChanges();
        handleDeletedCpes();
    }

    /**
     * The action to perform when a modification is notified on the CpeItems.
     */
    public void handleCpeItemChanges() {
        OffsetDateTime lastChangesDate = null;
        try {
            lastChangesDate = dbapiHandler.getCpeItemsLastUpdateDate();
        } catch (Exception e) {
            System.err.println("[Discovery] Could not get last update date from dbapi, fetching all CPE Items from CSL-Scan");
        }
        List<CpeItem> changes = scanApiHandler.getCpeItemChangesSince(lastChangesDate);
        if (changes != null) {
            try {
                dbapiHandler.sendCpeItems(changes);
//                mqttBroker.publish(CSLMqttBrokerHandler.Topic.CPE_ITEMS, CSLMqttMessage.message("synchronization_ended"));
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * Send a device to CSL-Scan.
     * First tries to create a new one, and on failure tries to modify the device (assuming it already exists).
     *
     * @param newDevice The device to send.
     * @throws Exception If we were not able to send the device to CSL-Scan, that is neither creating a new one nor modify an existing one worked.
     */
    private void sendNewDeviceToScanner(Device newDevice) throws Exception {
        JsonApiResponse result = scanApiHandler.createOrUpdateEntity(newDevice);
        if (!result.isSuccess()) {
            throw new Exception("Could not push the entity " + newDevice.getId() + " to CSL-Scan.");
        }
    }

    /**
     * Handle the changes in the devices on DB-API.
     *
     * @return A {@link Json} containing the result (success or failure).
     */
    public JsonApiResponse handleDbapiDeviceChange() {
        List<Device> newDevices;
        List<String> deletedDevices = new ArrayList<>();
        List<String> failedDevices = new LinkedList<>();
        OffsetDateTime currentTime = OffsetDateTime.now();
        try {
            OffsetDateTime lastDeviceModification = scanApiHandler.getLastLastEntityUpdateDate();
            Pair<List<Device>, List<String>> buildResult = buildNewDevices(
                    dbapiHandler.getDevicesSince(lastDeviceModification),
                    dbapiHandler.getConnectionsSince(lastDeviceModification)
            );
            newDevices = buildResult.getFirst();
            deletedDevices.addAll(buildResult.getSecond());
            deletedDevices.addAll(dbapiHandler.getDeletedDevicesSince(lastDeviceModification));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return JsonApiResponse.error("Could not get changes from DBAPI");
        }
//        lastDeviceModificationVerification = currentTime;
        for (Device newDevice : newDevices) {
            try {
                sendNewDeviceToScanner(newDevice);
            } catch (Exception e) {
                failedDevices.add(newDevice.getId());
            }
        }

        for (String deletedDevice : deletedDevices) {
            try {
                scanApiHandler.deleteEntity(deletedDevice);
            } catch (Exception e) {
                failedDevices.add(deletedDevice);
            }
        }
        return failedDevices.isEmpty()
                ? JsonApiResponse.success()
                : JsonApiResponse.error(
                "Failed to send updated devices to CSL-Scan",
                Json.object("failed_devices", Json.array(failedDevices.toArray()))
        );
    }

    /**
     * Function to execute when CPE Item deletions are notified.
     * Retrieves the deleted CPE Items from DB-API and requests the deletion to CSL-Scan.
     *
     * @return A {@link Json} with the result of the operation: { "result": "OK" } if the deletion was successful,
     * { "result": "NOK", "error": { "reason": ..., "details": ...}} otherwise.
     */
    private JsonApiResponse handleDeletedCpes() {
        List<String> deletedCpes = null;
        try {
            OffsetDateTime currentTime = OffsetDateTime.now();
            deletedCpes = dbapiHandler.getDeletedCpeItemsSince(lastCpeItemDeletionVerification);
            lastCpeItemDeletionVerification = currentTime;
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
        JsonApiResponse syncResult = handleDbapiDeviceChange();
        if (!syncResult.isSuccess()) {
            return JsonApiResponse.error(
                    "Could not retrieve devices from DB-API",
                    Json.object("failed_devices", syncResult.getError().getDetails().get("failed_devices"))
            );
        }
        if (isConcentrator) {
            return scanWebSocketHandler.requestScan(entities);
        } else {
            return JsonApiResponse.error("Scan WebSocket not in use");
        }
    }


    /**
     * Create the list of entities to update on CSL-Scan.
     *
     * @param devices     The list of devices that were created or modified.
     * @param connections The list of connections that were created or modified.
     * @return A {@link Json} with the entities to send to CSL-Scan and the ones to delete, in the format
     * <code>
     * {
     * "scan_entities": [Json objects],
     * "devices_to_delete": [id (strings)]
     * }
     * </code>
     */
    private Pair<List<Device>, List<String>> buildNewDevices(List<Device> devices, List<Connection> connections) {
        //region List the uuids we have in both list
        List<String> devicesToDelete = new ArrayList<>();
        List<Integer> connectionUuidsInDevices = new ArrayList<>();
        List<String> deviceUuidsInConnections = new ArrayList<>();

        for (Device device : devices) {
            List<Integer> connectionsIds = device.getConnectionsIds();
            if (connectionsIds.isEmpty()) {
                devicesToDelete.add(device.getId());
            } else {
                connectionUuidsInDevices.addAll(connectionsIds);
            }
        }
        for (Connection connection : connections) {
            deviceUuidsInConnections.addAll(connection.getDevicesIds());
        }
        //endregion

        //region Check for the ones missing on one side or the other
        List<Integer> connectionsToGet = new ArrayList<>();
        List<String> devicesToGet = new ArrayList<>();

        for (int connectionId : connectionUuidsInDevices) {
            if (DbapiUtils.getConnectionById(connections, connectionId) == null) {
                connectionsToGet.add(connectionId);
            }
        }
        for (String deviceId : deviceUuidsInConnections) {
            if (DbapiUtils.getDeviceById(devices, deviceId) == null) {
                devicesToGet.add(deviceId);
            }
        }
        //endregion

        //region Get the missing parts
        try {
            connections.addAll(dbapiHandler.fetchConnections(connectionsToGet));
            devices.addAll(dbapiHandler.fetchDevices(devicesToGet));
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace(System.err);
        }
        //endregion

        // Fill the connections into devices
        devices.forEach(device -> device.setConnections(connections));

        return new Pair<>(devices, devicesToDelete);
    }

}
