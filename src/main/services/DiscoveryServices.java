package main.services;

import com.csl.core.CSLContext;
import com.csl.intercom.broker.CSLMqttBrokerHandler;
import com.csl.intercom.cslscan.CpeItem;
import com.csl.intercom.cslscan.ScanWebSocketHandler;
import com.csl.intercom.dbapi.DbapiHandler;
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
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
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
    private final String scanManagerApiBaseEndpoint = "/api";
    private final HttpClient scanHttpClient = new HttpClient();
    private String scanManagerDiscoveryUrl;
    private String scanManagerApiUrl;
    private String scanManagerProtocol;
    //    private LocalDateTime lastCpeItemModification;
    private LocalDateTime lastDeviceModificationVerification = null;
    private LocalDateTime lastCpeItemDeletionVerification = null;
    private final boolean isConcentrator;
    private ScanWebSocketHandler scanWebSocketHandler = null;
    //    private String apiKey;
    private DbapiHandler dbapiHandler;
    private ZoneId zoneId;
    private CSLMqttBrokerHandler mqttBroker = null;
    private ScheduledExecutorService synchronizationSchedule;


    private static final Map<String, String> connectionInfoFields = new HashMap<>() {{
        put("queryProtocol", "queryProtocol");
        put("community", "community");
        put("user", "user");
        put("pass", "pass");
        put("privPassPhrase", "privPassPhrase");
        put("securityLevel", "securityLevel");
        put("authProtocolName", "authProtocolName");
        put("privProtocolName", "privProtocolName");
    }};
    private static final List<String> snmpv2cConnectionInfoFields = new ArrayList<>() {{
        add("queryProtocol");
        add("community");
    }};
    private static final List<String> snmpv3ConnectionInfoFields = new ArrayList<>() {{
        add("queryProtocol");
        add("user");
        add("pass");
        add("privPassPhrase");
        add("securityLevel");
        add("authProtocolName");
        add("privProtocolName");
    }};

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

        scanManagerProtocol = JsonUtil.getStringFromJson(jConfig, "manager_protocol", "http");
        if ("https".equals(scanManagerProtocol)) {
            scanManagerDiscoveryUrl = "wss://" + scanManagerIp + ":" + scanManagerPort + "/csl-scan/";
        } else {
            scanManagerDiscoveryUrl = "ws://" + scanManagerIp + ":" + scanManagerPort + "/csl-scan/";
        }
        scanManagerApiUrl = scanManagerProtocol + "://" + scanManagerIp + ":" + scanManagerPort + scanManagerApiBaseEndpoint;
        try {
            scanHttpClient.start();
//            dbapiHttpClient.start();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        if (isConcentrator) {
            scanWebSocketHandler = new ScanWebSocketHandler(this, scanManagerDiscoveryUrl);
        }

        dbapiHandler = new DbapiHandler();

        Json globalConfig = CSLContext.instance.getConfig().get("global");

        zoneId = ZoneId.of(JsonUtil.getStringFromJson(globalConfig, "timezone", "Europe/Paris"));
        if (isConcentrator) {
            mqttBroker = CSLContext.instance.getMqttBroker();
            mqttBroker.subscribeToTopic(CSLMqttBrokerHandler.Topic.DEVICES, message -> handleDbapiDeviceChange());
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
        addCmd("list_entities", params -> listEntities(),
                new JsonCmdHelp().setDesc("Retieve the entities registered in CSL-Scan")
                        .setResult("The list of entities' information as returned by CSL-Scan, in the format" +
                                "<code>{\"success\": true, \"result\": [...]}</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_entity", params -> getEntity(params.get("id").asString()),
                new JsonCmdHelp().setDesc("Retrieve a specific entity from CSL-Scan")
                        .setParam("id", "The uuid of the entity to retrieve", IJsonCmdHelp.STR)
                        .setResult("The entity as returned by CSL-Scan", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
//        addCmd("update_entity", this::updateEntity);
        addCmd("delete_entity", params -> deleteEntity(params.get("id").asString()),
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
        addCmd("get_entity_cpes", params -> getEntityCpes(params.get("id").asString()),
                new JsonCmdHelp().setDesc("Get an entity's CPE Items")
                        .setParam("id", "The entity's uuid", IJsonCmdHelp.STR)
                        .setResult("The list of CPE Items of the entity, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("get_cpes_since", params -> Json.object("success", true, "result", Json.array(getCpeItemChangesSince(LocalDateTime.parse(JsonUtil.getStringFromJson(params, "date", null))).toArray())),
                new JsonCmdHelp().setDesc("Retrieve CPE Items that change strictly after a specified date")
                        .setParam("date", "in ISO format, example: 2023-04-13T13:56:56.66 (local date format)", IJsonCmdHelp.STR)
                        .setResult("The list of CPE Items that changed strictly after <code>date</code>, in the format" +
                                "<code>{\"success\": true, \"result\": [...]</code>", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
//        addCmd("global_status", params -> getServiceStatus());
        addCmd("scan_status", params -> getScanStatus(params.get("id").asString()),
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
                    return startScan(entities);
                },
                new JsonCmdHelp().setDesc("Start a scan from CSL-Scan")
                        .setParam("entities", "An array of strings with the uuids of the entities to scan. May be omitted or null, resulting in scanning all entities.", IJsonCmdHelp.JSON)
                        .setResult("<code>{ \"success\": true }</code> if the scan was started successfully", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );
        addCmd("synchronize_devices", params -> handleDbapiDeviceChange(),
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
                            changes = getCpeItemChangesSince(null);
                        } else {
                            changes = getCpeItemChangesSince(LocalDateTime.parse(dateString));
                        }
                        try {
                            dbapiHandler.sendCpeItems(changes);
                        } catch (Exception e) {
                            return Json.object("success", false,
                                    "error", Json.object("reason", "Could not send changes to DB-API",
                                            "details", e.getMessage())
                            );
                        }
                    } else {
                        handleCpeItemChanges();
                    }
                    return Json.object("success", true);
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
            scanHttpClient.stop();
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
     * Create a new entity in the scanner
     *
     * @param entity A {@link Json} containing the entity to add. Fields 'id', 'name' and 'ip' are required.
     * @return A {@link Json} containing the newly created entity, as handed by the scanner
     */
    public Json addEntity(Json entity) {
        String protocol = JsonUtil.getStringFromJson(entity, "protocol", "");
        switch (protocol.toLowerCase()) {
            case "snmpv2c":
                return addSnmpv2cEntity(
                        JsonUtil.getStringFromJson(entity, "id", null),
                        JsonUtil.getStringFromJson(entity, "name", null),
                        JsonUtil.getStringFromJson(entity, "ip", null),
                        JsonUtil.getIntFromJson(entity, "port", 161),
                        JsonUtil.getStringFromJson(entity, "community", "public")
                );

            case "snmpv3":
                return addSnmpv3Entity(
                        JsonUtil.getStringFromJson(entity, "id", null),
                        JsonUtil.getStringFromJson(entity, "name", null),
                        JsonUtil.getStringFromJson(entity, "ip", null),
                        JsonUtil.getIntFromJson(entity, "port", 161),
                        JsonUtil.getStringFromJson(entity, "user", null),
                        JsonUtil.getStringFromJson(entity, "pass", null),
                        JsonUtil.getStringFromJson(entity, "privPassPhrase", null),
                        JsonUtil.getStringFromJson(entity, "securityLevel", null),
                        JsonUtil.getStringFromJson(entity, "authProtocolName", null),
                        JsonUtil.getStringFromJson(entity, "privProtocolName", null)
                );

            default:
                return Json.object("success", false,
                        "error", Json.object("reason", "Unsupported protocol: " + protocol)
                );
        }
    }

    /**
     * Send a new SNMPv2c entity to CSL-Scan.
     *
     * @param uuid      The unique id of the new entity.
     * @param name      The name of the entity.
     * @param ip        The IP address of the entity.
     * @param port      The SNMP port on which to contact the entity.
     * @param community The SNMP community of the entity.
     * @return The result from the scanner.
     */
    private Json addSnmpv2cEntity(String uuid, String name, String ip, int port, String community) {
        if (uuid == null || name == null || ip == null) {
            return Json.object(
                    "success", false,
                    "error", Json.object("reason", "The fields 'id', 'name' and 'ip' are required")
            );
        } else {
            return sendRequestToScanManager(HttpMethod.POST, "/entity/", Json.object(
                    "uuid", uuid,
                    "name", name,
                    "ipAddress", ip,
                    "port", port,
                    "connectionInfo", Json.object(
                            "queryProtocol", "SNMPV2c",
                            "community", community
                    ),
                    "isDeleted", false
            ));
        }
    }

    /**
     * Send a new SNMPv3 entity to CSL-Scan.
     *
     * @param uuid             The unique id of the new entity.
     * @param name             The name of the entity.
     * @param ip               The IP address of the entity.
     * @param port             The SNMP port on which to contact the entity.
     * @param user             The username for the entity.
     * @param pass             The SNMP password of the entity.
     * @param privPassPhrase   The privacy pass phrase of the entity.
     * @param securityLevel    The security level (authPriv, noAuthNoPriv or authNoPriv).
     * @param authProtocolName The authentication protocol (AuthMD5 or SHA-256).
     * @param privProtocolName The privacy protocol (PrivAES128 or PrivDES).
     * @return The result from the scanner.
     */
    private Json addSnmpv3Entity(String uuid, String name, String ip, int port, String user, String pass, String privPassPhrase, String securityLevel, String authProtocolName, String privProtocolName) {
        if (uuid == null || name == null || ip == null) {
            return Json.object(
                    "success", false,
                    "error", Json.object("reason", "The fields 'id', 'name' and 'ip' are required")
            );
        } else {
            return sendRequestToScanManager(HttpMethod.POST, "/entity/", Json.object(
                    "uuid", uuid,
                    "name", name,
                    "ipAddress", ip,
                    "port", port,
                    "connectionInfo", Json.object(
                            "queryProtocol", "SNMPV3",
                            "user", user,
                            "pass", pass,
                            "privPassPhrase", privPassPhrase,
                            "securityLevel", securityLevel,
                            "authProtocolName", authProtocolName,
                            "privProtocolName", privProtocolName
                    ),
                    "isDeleted", false
            ));
        }
    }

    /**
     * Get the list of configured entities in the scanner.
     *
     * @return A {@link Json} array containing the all the configured entities in the scanner.
     */
    public Json listEntities() {
        return sendRequestToScanManager(HttpMethod.GET, "/entity/", Json.object());
    }

    /**
     * Get a specific entity.
     *
     * @param id The unique identifier created by the scanner, as returned at creation or in a list.
     * @return A {@link Json} containing the specified entity.
     */
    public Json getEntity(String id) {
        Json response = sendRequestToScanManager(HttpMethod.GET, "/entity/" + id, Json.object());
        if (response.get("status_code").asInteger() != 200) {
            return Json.object("success", false,
                    "error", Json.object("reason", "Could not get the entity",
                            "details", response.get("result"))
            );
        } else {
            // The status code is not relevant for the http response.
            response.delAt("status_code");
            return response;
        }
    }

    /**
     * Change fields in an already existing entity, leaves unchanged the ones not provided (ie, that are null).
     *
     * @param entity A {@link Json} with the entity's new information. The 'id' field is required.
     * @return The old version of the entity, not reflecting the changes made.
     */
    public Json updateEntity(Json entity) {
        Map<String, Json> entityMap = entity.asJsonMap();
        String id = JsonUtil.getStringFromJson(entity, "id", null);
        Json params = getEntity(id);
        String name = JsonUtil.getStringFromJson(entity, "name", null);
        if (name != null) {
            params.set("name", name);
        }
        String ip = JsonUtil.getStringFromJson(entity, "ip", null);
        if (ip != null) {
            params.set("ipAddress", ip);
        }
        int port = JsonUtil.getIntFromJson(entity, "port", 0);
        if (port != 0) {
            params.set("port", port);
        }
        for (Map.Entry<String, String> connectionInfo : connectionInfoFields.entrySet()) {
            String key = connectionInfo.getKey();
            String value = connectionInfo.getValue();
            if (entityMap.containsKey(key)) {
                params.get("connectionInfo").set(connectionInfoFields.get(value), entity.get(key));
            }
        }
        switch (entity.get("protocol").asString().toLowerCase()) {
            case "snmpv2c":
                for (String field : connectionInfoFields.values()) {
                    if (!snmpv2cConnectionInfoFields.contains(field) && params.get("connectionInfo").has(field)) {
                        params.get("connectionInfo").delAt(field);
                    }
                }
                break;

            case "snmpv3":
                for (String field : connectionInfoFields.values()) {
                    if (!snmpv3ConnectionInfoFields.contains(field) && params.get("connectionInfo").has(field)) {
                        params.get("connectionInfo").delAt(field);
                    }
                }
                break;
        }
        return sendRequestToScanManager(HttpMethod.PUT, "/entity/" + id, params);
    }

    /**
     * Delete an entity from the scanner.
     *
     * @param id The unique identifier of the entity, as returned at creation or in a list.
     * @return An empty object on success, an error message on failure.
     */
    public Json deleteEntity(String id) {
        Json response = sendRequestToScanManager(HttpMethod.DELETE, "/entity/" + id, Json.object());
        boolean success = response.get("success").asBoolean();
        Json result = response.get("result");
        if (success) {
            if (response.get("status_code").asInteger() == 404) {
                return Json.object("success", false,
                        "error", Json.object("reason", "Could not find entity " + id + " in CSL-Scan (got 404)"));
            }
            return Json.object("success", true);
        } else {
            return Json.object("success", false,
                    "error", Json.object("reason", "Could not delete the entity " + id + " from CSL-Scan")
            );
        }
    }

    /**
     * Get all the detected SNMP objects found.
     *
     * @return A {@link Json} array containing all the SNMP objects discovered so far by the scanner.
     */
    public List<CpeItem> getAllCpes() {
        return getCpeItemChangesSince(null);
    }

    /**
     * Get the CPE items that have changed since the specified date.
     *
     * @param date The date to start receiving notifications. May be null to retrieve all the items.
     * @return A {@link Json} array containing the CPE items that have changed since the specified date, or all the items if date was null.
     */
    public List<CpeItem> getCpeItemChangesSince(LocalDateTime date) {
        OffsetDateTime utcDate = localTimeToUtc(date);
        Json response;
        Json cpeItems = Json.array();
        if (date == null) {
            response = sendRequestToScanManager(HttpMethod.GET, "/cpeItem/", Json.object());
        } else {
            response = sendRequestToScanManager(HttpMethod.GET, "/cpeItem/", Json.object("date", utcDate.toString()));
        }
        if (response.get("success").asBoolean() && response.get("status_code").asInteger() == 200) {
            cpeItems = response.get("result");
        } else {
//            return Json.object("success", false,
//                    "error", Json.object("reason", "Could not retrieve CPE Items from CSL-Scan",
//                            "details", response.get("result"))
//            );
            return null;
        }
        // Remove the items that have the *exact* same date as whe previously had
//        List<Json> cpeItemsList = cpeItems.asJsonList();
//        Iterator<Json> iterator = cpeItemsList.iterator();
//        while (iterator.hasNext()) {
//            Json cpeItem = iterator.next();
//            LocalDateTime cpeItemUpdateDate = getCpeItemDateTime(cpeItem);
//            if (cpeItemUpdateDate != null && cpeItemUpdateDate.atOffset(ZoneOffset.UTC).equals(utcDate)) {
//                iterator.remove();
//            }
//        }
        List<CpeItem> cpeItemsList = new ArrayList<>(cpeItems.asJsonList().size());
        for (Json cpeItem: cpeItems.asJsonList()) {
            CpeItem parsedCpeItem = CpeItem.fromScanCpeItem(cpeItem);
            if (!parsedCpeItem.getDiscoveredDate().equals(utcDate)) {
                cpeItemsList.add(parsedCpeItem);
            }
        }
//        return Json.object("success", true, "result", cpeItems);
        return cpeItemsList;
    }

    /**
     * Get all the SNMP objects discovered on a particular entity.
     *
     * @param id The unique identifier of the entity, as returned at creation or in a list.
     * @return A {@link Json} array containing all the SNMP objects discovered so far by the scanner on this entity.
     */
    public Json getEntityCpes(String id) {
        return sendRequestToScanManager(HttpMethod.GET, "/cpeItem/entity/" + id, Json.object());
    }

    /**
     * Get the status of a specific scan task.
     *
     * @param id The unique id of the task.
     * @return The status of the scan.
     */
    public Json getScanStatus(String id) {
        return sendRequestToScanManager(HttpMethod.GET, "/discovery/status/" + id, Json.object());
    }

    /**
     * Get the status of an entity's scan
     *
     * @param id The entity's unique id.
     * @return The status of the scan.
     */
    public Json getEntityScanStatus(String id) {
        return sendRequestToScanManager(HttpMethod.GET, "/status/entity/" + id, Json.object());
    }

    /**
     * Fetch CSL-Scan's status.
     *
     * @return A {@link Json} with CSL-Scan's status as it was received, or with an error in the 'error' field.
     */
    private Json getScanManagerStatus() {
        return sendRequestToScanManager(HttpMethod.GET, "/discovery/status", Json.object());
    }

    /**
     * Get the service of the status.
     *
     * @return A {@link Json} object contining information about the status of sub services.
     */
    public Json getStatus() {
        Json status = Json.object();

        Json scanManagerStatus = getScanManagerStatus();
        if (scanManagerStatus.isObject() && !scanManagerStatus.has("error")) {
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
        LocalDateTime lastChangesDate = null;
        try {
            lastChangesDate = dbapiHandler.getCpeItemsLastUpdateDate();
        } catch (Exception e) {
            System.err.println("[Discovery] Could not get last update date from dbapi, fetching all CPE Items from CSL-Scan");
        }
        List<CpeItem> changes = getCpeItemChangesSince(lastChangesDate);
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
     * @param newDevice The {@link Json} containing the device to send.
     * @throws Exception If we were not able to send the device to CSL-Scan, that is neither creating a new one nor modify an existing one worked.
     */
    private void sendNewDeviceToScanner(Json newDevice) throws Exception {
        // first try to create the entity
        Json result = addEntity(newDevice);
        // if it failed, try to update it
        if (!result.get("success").asBoolean()) {
            result = updateEntity(newDevice);
        }
        if (!result.get("success").asBoolean()) {
            throw new Exception("Could not push the entity " + JsonUtil.getStringFromJson(newDevice, "id", "") + " to CSL-Scan.");
        }
    }

    /**
     * Handle the changes in the devices on DB-API.
     *
     * @return A {@link Json} containing the result (success or failure).
     */
    public Json handleDbapiDeviceChange() {
        List<Json> newDevices;
        List<String> deletedDevices = new ArrayList<>();
        List<String> failedDevices = new LinkedList<>();
        LocalDateTime currentTime = LocalDateTime.now();
        try {
            Pair<List<Json>, List<String>> buildResult = buildNewDevices(
                    dbapiHandler.getDevicesSince(lastDeviceModificationVerification),
                    dbapiHandler.getConnectionsSince(lastDeviceModificationVerification)
            );
            newDevices = buildResult.getFirst();
//            deletedDevices.addAll(buildResult.getSecond());
            deletedDevices.addAll(dbapiHandler.getDeletedDevicesSince(lastDeviceModificationVerification));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return Json.object("success", false,
                    "error", Json.object("reason", "Could not get changes from DBAPI")
            );
        }
        lastDeviceModificationVerification = currentTime;
        for (Json newDevice : newDevices) {
            try {
                sendNewDeviceToScanner(newDevice);
            } catch (Exception e) {
                failedDevices.add(JsonUtil.getStringFromJson(newDevice, "id", ""));
            }
        }

        for (String deletedDevice : deletedDevices) {
            try {
                deleteEntity(deletedDevice);
            } catch (Exception e) {
                failedDevices.add(deletedDevice);
            }
        }
        return failedDevices.isEmpty()
                ? Json.object("success", true)
                : Json.object(
                "success", false,
                "error", Json.object(
                        "reason", "Failed to send updated devices to CSL-Scan",
                        "failed_devices", Json.array(failedDevices.toArray())
                )
        );
    }

    /**
     * Function to execute when CPE Item deletions are notified.
     * Retrieves the deleted CPE Items from DB-API and requests the deletion to CSL-Scan.
     *
     * @return A {@link Json} with the result of the operation: { "result": "OK" } if the deletion was successful,
     * { "result": "NOK", "error": { "reason": ..., "details": ...}} otherwise.
     */
    private Json handleDeletedCpes() {
        List<String> deletedCpes = null;
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            deletedCpes = dbapiHandler.getDeletedCpeItemsSince(lastCpeItemDeletionVerification);
            lastCpeItemDeletionVerification = currentTime;
        } catch (Exception e) {
            return Json.object(
                    "success", false,
                    "error", Json.object(
                            "reason", "Failed to fetch deleted CPE Items",
                            "details", e.getMessage()
                    )
            );
        }
        deleteCpeItemsFromScan(deletedCpes);
        return Json.object("result", "OK");
    }

    /**
     * Request the deletion of a CPE Item to CSL-Scan.
     *
     * @param id The uuid of the CPE Item to delete.
     */
    private void deleteCpeItemFromScan(String id) {
        sendRequestToScanManager(HttpMethod.DELETE, "/cpeItem/entity/" + id, Json.object());
    }

    /**
     * Request multiple deletions of CPE Items to CSL-Scan.
     *
     * @param ids The list of CPE Items to delete.
     */
    private void deleteCpeItemsFromScan(List<String> ids) {
        for (String id : ids) {
            deleteCpeItemFromScan(id);
        }
    }

    /**
     * Synchronize devices between DB-API and CSL-Scan, and then request a new scan from DB-API.
     *
     * @param entities The entities' uuids to scan. May be null, in which case all entities are scanned.
     * @return The result of the scan request, in a {@link Json} :
     * either {"success": true} if the scan request was successfully handed to CSL-Scan,
     * or {"success": false, "error": {"reason":"..."}} otherwise
     */
    public Json startScan(List<String> entities) {
        Json syncResult = handleDbapiDeviceChange();
        if (!syncResult.get("success").asBoolean()) {
            return Json.object("success", false,
                    "error", Json.object(
                            "reason", "Could not retrieve devices from DB-API",
                            "failed_devices", syncResult.get("error").get("failed_devices")
                    )
            );
        }
        if (isConcentrator) {
            return scanWebSocketHandler.requestScan(entities);
        } else {
            return Json.object("success", false,
                    "error", Json.object("reason", "Scan WebSocket not in use"));
        }
    }


    /**
     * Send an HTTP request to the scanner.
     *
     * @param method   The HTTP method to use (GET, POST, PUT, ...)
     * @param endpoint The endpoint on the API to use.
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    private Json sendRequestToScanManager(HttpMethod method, String endpoint, Json params) {
        Json res = Json.object();
        Request request;
        String URI = scanManagerApiUrl + endpoint;

        request = scanHttpClient.newRequest(URI);
        request.method(method);
//        System.out.println(method.asString() + " " + URI);
//        System.out.println("Payload: " + params.toString());
        try {
            switch (method) {
                case GET:
                case DELETE:
                    for (Map.Entry<String, Json> param : params.asJsonMap().entrySet()) {
                        if (param.getValue().isString()) {
                            request.param(param.getKey(), param.getValue().asString());
                        } else {
                            request.param(param.getKey(), param.getValue().toString());
                        }
                    }
                    break;

                case POST:
                case PUT:
                    request.content(new StringContentProvider(params.toString()), "application/json");
                    break;

                default:
                    throw new UnsupportedOperationException("Unsupported HTTP method: " + method.asString());
            }
            ContentResponse response = request.send();
            if (response.getContent().length > 0) {
                if (response.getContent()[0] == '{' || response.getContent()[0] == '[') {
                    res = Json.object("success", true,
                            "status_code", response.getStatus(),
                            "result", Json.read(response.getContentAsString())
                    );
                } else {
                    res = Json.object("success", true,
                            "status_code", response.getStatus(),
                            "result", response.getContentAsString()
                    );
                }
            } else {
                res = Json.object("success", true,
                        "status_code", response.getStatus(),
                        "result", null
                );
            }
        } catch (UnsupportedOperationException e) {
            res = Json.object("success", false,
                    "error", Json.object("reason", e.getMessage())
            );
        } catch (Exception e) {
            if (e.getCause() instanceof ConnectException) {
                res = Json.object("success", false,
                        "error", Json.object("reason", "Connection error with CSL-Scan")
                );
            }
            e.printStackTrace(System.err);
        }
        return res;
    }

    /**
     * Contact the scan service to retrieve an entity through by its name.
     * Stops at the first match, thus ignores extra results if the name is duplicated.
     *
     * @param name The name to seek in the entities
     * @return null if the scan service is down, "" if the name was not found.
     */
    public Json getEntityByName(String name) {
        Json entities = listEntities();
        if (entities == null || !entities.isArray()) {
            return null;
        }
        Json result = Json.object();
        for (Json entity : entities.asJsonList()) {
            if (JsonUtil.getStringFromJson(entity, "name", "").equals(name)) {
                result = entity;
            }
        }
        return result;
    }

    /**
     * Contact the scan service to retrieve an entity through by its IP address.
     * Stops at the first match, thus ignores extra results if the IP address is duplicated.
     *
     * @param ipAddress The IP address to seek in the entities
     * @return null if the scan service is down, "" if the address was not found.
     */
    public Json getEntityByIp(String ipAddress) {
        Json entities = listEntities();
        if (entities == null || !entities.isArray()) {
            return null;
        }
        Json result = Json.object();
        for (Json entity : entities.asJsonList()) {
            if (JsonUtil.getStringFromJson(entity, "ipAddress", "").equals(ipAddress)) {
                result = entity;
            }
        }
        return result;
    }

    /**
     * Extract a CPE Item's modification date from its {@link Json} form
     *
     * @param cpeItem The CPE Item we want to read
     * @return A {@link LocalDateTime} with the last modification date of the CPE Item
     */
    private LocalDateTime getCpeItemDateTime(Json cpeItem) {
//        return LocalDateTime.parse(JsonUtil.getStringFromJson(cpeItem, "updatedAt", lastCpeItemModification.toString()));
        String cpeItemDate = JsonUtil.getStringFromJson(cpeItem, "updatedAt", null);
        return cpeItemDate == null ? null : LocalDateTime.parse(cpeItemDate);
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
    private Pair<List<Json>, List<String>> buildNewDevices(List<Json> devices, List<Json> connections) {
        //region List the uuids we have in both list
        List<String> devicesToDelete = new ArrayList<>();
        List<Integer> connectionUuidsInDevices = new ArrayList<>();
        List<String> deviceUuidsInConnections = new ArrayList<>();

        for (Json device : devices) {
            Json connection = device.get("connection");
            if (connection != null && !connection.isNull()) {
                connectionUuidsInDevices.add(connection.asInteger());
            } else {
                devicesToDelete.add(device.get("id").asString());
            }
        }
        for (Json connection : connections) {
            for (Json device : connection.get("devices").asJsonList()) {
                deviceUuidsInConnections.add(device.asString());
            }
        }
        //endregion

        //region Check for the ones missing on one side or the other
        List<Integer> connectionsToGet = new ArrayList<>();
        List<String> devicesToGet = new ArrayList<>();

        for (int connectionId : connectionUuidsInDevices) {
            if (dbapiHandler.getConnectionById(connections, connectionId) == null) {
                connectionsToGet.add(connectionId);
            }
        }
        for (String deviceId : deviceUuidsInConnections) {
            if (dbapiHandler.getDeviceById(devices, deviceId) == null) {
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

        //region Build the entities to send
        List<Json> protocols = null;
        try {
            protocols = dbapiHandler.fetchDiscoveryProtocols();
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace(System.err);
        }

        List<Json> scanEntities = new ArrayList<>();
        for (Json device : devices) {
            if (!device.has("connection") || device.get("connection").isNull()) {
                continue;
            }
            Json connection = dbapiHandler.getConnectionById(connections, device.get("connection").asInteger());
            Json scanEntity = Json.object(
                    "id", device.get("id"),
                    "name", device.get("name"),
                    "ip", device.get("ip"),
                    "port", connection.get("port"),
                    "protocol", dbapiHandler.getProtocolById(protocols, connection.get("protocol").asInteger()),
                    "isDeleted", false
            );
            for (Map.Entry<String, String> connectionInfoField : connectionInfoFields.entrySet()) {
                String key = connectionInfoField.getKey();
                String value = connectionInfoField.getValue();
                if (connection.has(key)) {
                    scanEntity.set(value, connection.get(key));
                }
            }
            scanEntities.add(scanEntity);
        }
        //endregion
        return new Pair<>(scanEntities, devicesToDelete);
    }

    /**
     * Translate local date to UTC, as used by CSL-Scan.
     *
     * @param localDateTime The local date time.
     * @return The same date time in UTC.
     */
    private OffsetDateTime localTimeToUtc(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        OffsetDateTime utcDateTime = OffsetDateTime.parse(localDateTime.atZone(zoneId).toInstant().toString());
        return utcDateTime;
    }
}
