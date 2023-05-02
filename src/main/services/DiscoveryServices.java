package main.services;

import com.csl.core.CSLContext;
import com.csl.intercom.DbapiHandler;
import com.csl.intercom.broker.CSLMqttBrokerHandler;
import com.csl.intercom.broker.CSLMqttMessage;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.csl.logger.CSLLogger;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.ICSLService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.util.ScanWebSocketHandler;
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
public class DiscoveryServices implements ICSLService {
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

        addCmd("get_status", params -> getStatus());
        addCmd("add_entity", this::addEntity);
        addCmd("list_entities", params -> listEntities());
        addCmd("get_entity", params -> getEntity(params.get("id").asString()));
        addCmd("update_entity", this::updateEntity);
        addCmd("delete_entity", params -> deleteEntity(params.get("id").asString()));
        addCmd("get_all_cpes", params -> getAllCpes());
        addCmd("get_entity_cpes", params -> getEntityCpes(params.get("id").asString()));
        addCmd("get_cpes_since", params -> getCpeItemChangesSince(LocalDateTime.parse(JsonUtil.getStringFromJson(params, "date", null))));
        addCmd("global_status", params -> getServiceStatus());
        addCmd("scan_status", params -> getScanStatus(params.get("id").asString()));
        addCmd("entity_scan_status", params -> getEntityScanStatus(params.get("id").asString()));
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
        });
        addCmd("synchronize_devices", params -> handleDbapiDeviceChange());
        addCmd("get_last_cpe_items", params -> {
            String dateString = JsonUtil.getStringFromJson(params, "date", "");
            if (!dateString.equals("")) {
                Json changes = getCpeItemChangesSince(LocalDateTime.parse(dateString));
                try {
                    dbapiHandler.sendCpeItems(changes);
                } catch (Exception e) {
                    return Json.object("result", "NOK",
                            "error", Json.object("reason", "Could not send changes to DB-API")
                    );
                }
            } else {
                handleCpeItemChanges();
            }
            return Json.object("result", "OK");
        });

        // Test commands
        addCmd("get_entity_by_name", params -> {
            String name = JsonUtil.getStringFromJson(params, "name", "");
            return Json.object("uuid", getEntityUuid(getEntityByName(name)));
        });

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
                return Json.object("result", "NOK",
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
                    "result", "NOK",
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
                    "result", "NOK",
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
        return sendRequestToScanManager(HttpMethod.GET, "/entity/" + id, Json.object());
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
        return sendRequestToScanManager(HttpMethod.DELETE, "/entity/" + id, Json.object());
    }

    /**
     * Get all the detected SNMP objects found.
     *
     * @return A {@link Json} array containing all the SNMP objects discovered so far by the scanner.
     */
    public Json getAllCpes() {
        return getCpeItemChangesSince(null);
    }

    /**
     * Get the CPE items that have changed since the specified date.
     *
     * @param date The date to start receiving notifications. May be null to retrieve all the items.
     * @return A {@link Json} array containing the CPE items that have changed since the specified date, or all the items if date was null.
     */
    public Json getCpeItemChangesSince(LocalDateTime date) {
        OffsetDateTime utcDate = localTimeToUtc(date);
        Json cpeItems = Json.array();
        if (date == null) {
            cpeItems = sendRequestToScanManager(HttpMethod.GET, "/cpeItem/", Json.object());
        } else {
            cpeItems = sendRequestToScanManager(HttpMethod.GET, "/cpeItem/", Json.object("date", utcDate.toString()));
        }
        // Remove the items that have the *exact* same date as whe previously had
        List<Json> cpeItemsList = cpeItems.asJsonList();
        Iterator<Json> iterator = cpeItemsList.iterator();
        while (iterator.hasNext()) {
            Json cpeItem = iterator.next();
            LocalDateTime cpeItemUpdateDate = getCpeItemDateTime(cpeItem);
            if (cpeItemUpdateDate != null && cpeItemUpdateDate.atOffset(ZoneOffset.UTC).equals(utcDate)) {
                iterator.remove();
            }
        }
        return cpeItems;
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
     * Return the status of the scan manager
     *
     * @return The response from the scanner.
     */
    public Json getServiceStatus() {
        return sendRequestToScanManager(HttpMethod.GET, "/discovery/status", Json.object());
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
     * Get the service of the status.
     *
     * @return A {@link Json} object contining information about the status of sub services.
     */
    public Json getStatus() {
        Json status = Json.object();

        Json entitiesList = listEntities();
        if (entitiesList.isArray()) {
            status.set("httpRestApi", "OK");
        } else {
            status.set("httpRestApi", "NOK");
        }
        if (isConcentrator) {
            status.set("websocket", scanWebSocketHandler.getStatus());
        }

        return status;
    }

    /**
     * Synchronise the models :
     *   - Devices
     *   - CPE Items
     */
    public void syncAll() {
        handleDbapiDeviceChange();
        handleCpeItemChanges();
    }

    /**
     * The action to perform when a modification is notified on the CpeItems.
     */
    public void handleCpeItemChanges() {
        LocalDateTime lastChangesDate = null;
        try {
            lastChangesDate = dbapiHandler.getLastUpdateDate();
        } catch (Exception e) {
            System.err.println("[Discovery] Could not get last update date from dbapi, fetching all CPE Items from CSL-Scan");
        }
        Json changes = getCpeItemChangesSince(lastChangesDate);
        if (changes != null && changes.isArray()) {
            try {
                dbapiHandler.sendCpeItems(changes);
                mqttBroker.publish(CSLMqttBrokerHandler.Topic.CPE_ITEMS, CSLMqttMessage.message("synchronization_ended"));
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
        Json result = getApiCommands().exec("add_entity", newDevice);
        // if it failed, try to update it
        if (result.has("error")) {
            result = getApiCommands().exec("update_entity", newDevice);
        }
        if (result.has("error")) {
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
        List<String> deletedDevices;
        List<String> failedDevices = new LinkedList<>();
        LocalDateTime currentTime = LocalDateTime.now();
        try {
            newDevices = buildNewDevices(
                    dbapiHandler.getDevicesSince(lastDeviceModificationVerification),
                    dbapiHandler.getConnectionsSince(lastDeviceModificationVerification)
            );
            deletedDevices = dbapiHandler.getDeletedDevicesSince(lastDeviceModificationVerification);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return Json.object("result", "NOK",
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
                ? Json.object("result", "OK")
                : Json.object(
                "result", "NOK",
                "error", Json.object(
                        "reason", "Failed to send updated devices to CSL-Scan",
                        "failed_devices", Json.array(failedDevices.toArray())
                )
        );
    }


    /**
     * Synchronize devices between DB-API and CSL-Scan, and then request a new scan from DB-API.
     *
     * @param entities The entities' uuids to scan. May be null, in which case all entities are scanned.
     * @return The result of the scan request, in a {@link Json} :
     * either {"result": "OK"} if the scan request was successfully handed to CSL-Scan,
     * or {"result": "NOK", "error": {"reason":"..."}} otherwise
     */
    public Json startScan(List<String> entities) {
        Json syncResult = handleDbapiDeviceChange();
        if (syncResult.get("result").asString().equals("NOK")) {
            return Json.object("result", "NOK",
                    "error", Json.object(
                            "reason", "Could not retrieve devices from DB-API",
                            "failed_devices", syncResult.get("failed_devices")
                    )
            );
        }
        if (isConcentrator) {
            return scanWebSocketHandler.requestScan(entities);
        } else {
            return Json.object("result", "NOK",
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
        System.out.println(method.asString() + " " + URI);
        System.out.println("Payload: " + params.toString());
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
                    res = Json.read(response.getContentAsString());
                } else {
                    res = Json.object("result", response.getContentAsString());
                }
            }
        } catch (UnsupportedOperationException e) {
            res = Json.object("result", "NOK",
                    "error", Json.object("reason", e.getMessage())
            );
        } catch (Exception e) {
            if (e.getCause() instanceof ConnectException) {
                res = Json.object("result", "NOK",
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
     * @return A {@link List<Json>} with the entities to send to CSL-Scan.
     */
    private List<Json> buildNewDevices(List<Json> devices, List<Json> connections) {
        //region List the uuids we have in both list
        List<Integer> connectionUuidsInDevices = new ArrayList<>();
        List<String> deviceUuidsInConnections = new ArrayList<>();

        for (Json device : devices) {
            Json connection = device.get("connection");
            if (connection != null && !connection.isNull()) {
                connectionUuidsInDevices.add(connection.asInteger());
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
        return scanEntities;
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
