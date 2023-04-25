package main.services;

import com.csl.core.CSLContext;
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
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
    private final HttpClient dbapiHttpClient = new HttpClient();
    private String scanManagerDiscoveryUrl;
    private String scanManagerApiUrl;
    private String scanManagerProtocol;
    private LocalDateTime lastCpeItemModification;
    private LocalDateTime lastDeviceModificationVerification = null;
    private final boolean useWebSocket;
    private ScanWebSocketHandler scanWebSocketHandler = null;
    private String dbapiUrl;
    private String apiKey;
    private ZoneId zoneId;
    private String mqttBrokerUrl;
    private MqttClient mqttClient;
    private String mqttTopic;
    private ScheduledExecutorService mqttConnetionAttempts;

    private static final Map<String, String> connectionFieldsDbapiToLocal = new HashMap<>() {{
        put("discovery_protocol", "protocol");
        put("port_number", "port");
        put("custom_devices_uuid", "devices");
        put("snmp_community", "community");
        put("username", "user");
        put("password", "pass");
        put("snmp_privacy_key", "privPassPhrase");
        put("authentication_algorithm", "authProtocolName");
    }};
    private static final Map<String, String> connectionInfoFields = new HashMap<>() {{
        put("queryProtocol", "queryProtocol");
        put("community", "community");
        put("user", "user");
        put("pass", "pass");
        put("privPassPhrase", "privPassPhrase");
        put("securityLevel", "securityLevel");
        put("authProtocolName", "authProtocolName");
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
    }};

    public DiscoveryServices(String name, String configFileSectionName, boolean useWebSocket) {
        this.name = name;
        this.configFileSectionName = configFileSectionName;
        this.lastCpeItemModification = LocalDateTime.parse("2023-04-10T10:25:01.808");
        this.useWebSocket = useWebSocket;
    }

    public DiscoveryServices() {
        this(defaultName, defaultConfigFileSectionName, true);
    }

    public DiscoveryServices(boolean useWebSocket) {
        this(defaultName, defaultConfigFileSectionName, useWebSocket);
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
//        String idsconf = CSLContext.instance.getCslConfDir();

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
            dbapiHttpClient.start();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        if (useWebSocket) {
            scanWebSocketHandler = new ScanWebSocketHandler(this, scanManagerDiscoveryUrl);
        }

        Json globalConfig = CSLContext.instance.getConfig().get("global");
        if (globalConfig != null) {
            dbapiUrl = JsonUtil.getBooleanFromJson(globalConfig, "use_ssl", true) ? "https://" : "http://";
            dbapiUrl += JsonUtil.getStringFromJson(globalConfig, "ip_server_remote", "localhost");
            dbapiUrl += "/api";
            apiKey = JsonUtil.getStringFromJson(globalConfig, "api_key", "");
        } else {
            dbapiUrl = "https://localhost/api";
        }

        zoneId = ZoneId.of(JsonUtil.getStringFromJson(globalConfig, "timezone", "Europe/Paris"));
        mqttBrokerUrl = JsonUtil.getStringFromJson(globalConfig, "mqtt_broker_url", "tcp://localhost:1883");

        mqttConnetionAttempts = Executors.newScheduledThreadPool(1);
        mqttConnetionAttempts.scheduleAtFixedRate(this::connectMqttIfNecessary, 0, 2, TimeUnit.SECONDS);

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
                    sendCpeItemsToDbapi(changes);
                } catch (Exception e) {
                    return Json.object("result", "NOK",
                            "error", "Could not send changes to DB-API");
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
            if (useWebSocket) {
                scanWebSocketHandler.stop();
            }
            scanHttpClient.stop();
            mqttClient.disconnect();
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
     * Try to connect to MQTT broker if not already connected.
     */
    private void connectMqttIfNecessary() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            try {
                mqttClient = new MqttClient(
                        mqttBrokerUrl,
                        "CSL-concentrator",
                        new MemoryPersistence());
                MqttConnectOptions connectOptions = new MqttConnectOptions();
                connectOptions.setCleanSession(true);
                mqttClient.connect(connectOptions);
                mqttClient.subscribe(mqttTopic);
            } catch (MqttException e) {
                mqttClient = null;
            }
        }
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
                        JsonUtil.getStringFromJson(entity, "authProtocolName", null)
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
                    "error", "The fields 'id', 'name' and 'ip' are required"
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
     * @param authProtocolName The authentication protocol (MD5 or SHA).
     * @return The result from the scanner.
     */
    private Json addSnmpv3Entity(String uuid, String name, String ip, int port, String user, String pass, String privPassPhrase, String securityLevel, String authProtocolName) {
        if (uuid == null || name == null || ip == null) {
            return Json.object(
                    "result", "NOK",
                    "error", "The fields 'id', 'name' and 'ip' are required"
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
                            "authProtocolName", authProtocolName
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
            if (getCpeItemDateTime(cpeItem).atOffset(ZoneOffset.UTC).equals(utcDate)) {
                iterator.remove();
            }
        }
        if (!cpeItemsList.isEmpty()) {
            // Update lastCpeItemModification.
            // Currently, computed locally, should retrieve it from scan service latter.
            for (Json cpeItem : cpeItemsList) {
                LocalDateTime cpeItemUpdateTime = getCpeItemDateTime(cpeItem);
                if (cpeItemUpdateTime.isAfter(lastCpeItemModification)) {
                    lastCpeItemModification = cpeItemUpdateTime;
                }
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
        if (useWebSocket) {
            status.set("websocket", scanWebSocketHandler.getStatus());
        }

        return status;
    }

    /**
     * The action to perform when a modification is notified on the CpeItems.
     */
    public void handleCpeItemChanges() {
        LocalDateTime lastChangesDate;
        try {
            lastChangesDate = getDbapiLastUpdateDate();
        } catch (Exception e) {
            lastChangesDate = lastCpeItemModification;
            System.err.println("[Discovery] Could not get last update date from dbapi, falling back to " + lastCpeItemModification.toString());
        }
        Json changes = getCpeItemChangesSince(lastChangesDate);
        if (changes != null && changes.isArray()) {
            try {
                sendCpeItemsToDbapi(changes);
            } catch (Exception e) {
                lastCpeItemModification = lastChangesDate;
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
                    getDbapiDevicesSince(lastDeviceModificationVerification),
                    getDbapiConnectionsSince(lastDeviceModificationVerification)
            );
//            deletedDevices = getDbapiDeletedDevciesSince(lastDeviceModificationVerification);
            deletedDevices = new ArrayList<>()
//            {{ add("bf9ff4bd-11d1-4749-8c39-76667f84b3bb"); }}
            ;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return Json.object("result", "NOK",
                    "error", "Could not get changes from DBAPI");
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
                "error", Json.object("failed_devices", Json.array(failedDevices.toArray()))
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
        if (useWebSocket) {
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
                    "error", e.getMessage());
        } catch (Exception e) {
            if (e.getCause() instanceof ConnectException) {
                res = Json.object("result", "NOK",
                        "error", "Connection error with CSL-Scan");
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
        return LocalDateTime.parse(JsonUtil.getStringFromJson(cpeItem, "updatedAt", lastCpeItemModification.toString()));
    }

    /**
     * Send a CPE Item to DB-API
     *
     * @param cpeItem The CPE Item to send
     * @throws Exception If the sending fail
     */
    private void sendCpeItemToDbapi(Json cpeItem) throws Exception {
        Json requestContents = Json.object("cpe_data", cpeItem);
        if (cpeItem.has("updatedAt")) {
            requestContents.set("discovered_date", cpeItem.get("updatedAt"));
        }
        requestContents.set("device", cpeItem.get("entityUuid").asString());
        Request request = createDbapiRequest(HttpMethod.POST, "/cpe_discovered_items")
                .content(new StringContentProvider(requestContents.toString()), "application/json");
        ContentResponse response = request.send();
        if (response.getStatus() != 201) {
            throw new Exception("Error sending CpeItem to dbapi: got unexpected status " + response.getStatus());
        }
    }

    /**
     * Send a list of CPE Items to DB-API
     *
     * @param cpeItems A {@link List<Json>} with the CPE Items to send
     * @throws Exception If any item failed
     */
    private void sendCpeItemsToDbapi(Json cpeItems) throws Exception {
        for (Json cpeItem : cpeItems.asJsonList()) {
            sendCpeItemToDbapi(cpeItem);
        }
    }

    /**
     * Fetch the last updated date of CPE Items in DB-API.
     *
     * @return The last update of CPE-Items in DB-API.
     * @throws Exception If it was not possible to fetch from DB-API or the format was not recognised.
     */
    private LocalDateTime getDbapiLastUpdateDate() throws Exception {
        Request request = createDbapiRequest(HttpMethod.GET, "/cpe_discovered_items/get_last_discovered_date");
        ContentResponse response = request.send();
        Json responseContents = Json.read(response.getContentAsString());
        String lastUpdatedDateString;
        if (responseContents.isString()) {
            lastUpdatedDateString = responseContents.asString();
        } else if (responseContents.isObject()) {
            lastUpdatedDateString = responseContents.get("updatedAt").asString();
        } else {
            lastUpdatedDateString = responseContents.toString();
        }
        return dbapiDateToLocal(lastUpdatedDateString);
    }

    /**
     * Get the devices from DB-API that were changed since an optional date.
     *
     * @param date The date of start of modifications to fecth. May be null, in wich case fetches all devices.
     * @return The {@link List<Json>} of devices that were changed since date.
     * @throws Exception If the fetching failed.
     */
    private List<Json> getDbapiDevicesSince(LocalDateTime date) throws Exception {
        OffsetDateTime dateUtc = localTimeToUtc(date);
        Request request = createDbapiRequest(HttpMethod.GET, "/devices");
        if (dateUtc != null) {
            request.param("updated_at__gte", dateUtc.toString());
        }
        Json response = Json.read(request.send().getContentAsString());

        List<Json> devices = new ArrayList<>();
        for (Json jsonDevice : response.asJsonList()) {
            Json device = parseDbapiDevice(jsonDevice);
            devices.add(device);
        }
        return devices;
    }

    /**
     * Get the connections from DB-API that were changed since an optional date.
     *
     * @param date The date of start of modifications to fecth. May be null, in wich case fetches all connections.
     * @return The {@link List<Json>} of connections that were changed since date.
     * @throws Exception If the fetching failed.
     */
    private List<Json> getDbapiConnectionsSince(LocalDateTime date) throws Exception {
        OffsetDateTime dateUtc = localTimeToUtc(date);
        Request request = createDbapiRequest(HttpMethod.GET, "/connections");
        if (dateUtc != null) {
            request.param("updated_at__gte", dateUtc.toString());
        }
        Json response = Json.read(request.send().getContentAsString());
        List<Json> connections = new ArrayList<>(response.asList().size());
        for (Json jsonConnection : response.asJsonList()) {
            Json connection = parseDbapiConnection(jsonConnection);
            connections.add(connection);
        }
        return connections;
    }

    /**
     * Get the deleted devices from DB-API that were changed since an optional date.
     *
     * @param date The date of start of deletions to fecth. May be null, in wich case fetches all deletions.
     * @return The {@link List<String>} of device uuids that were deleted since date.
     * @throws Exception If the fetching failed.
     */
    private List<String> getDbapiDeletedDevciesSince(LocalDateTime date) throws Exception {
        OffsetDateTime dateUtc = localTimeToUtc(date);
        Request request = createDbapiRequest(HttpMethod.GET, "/connections/deleted");
        if (dateUtc != null) {
            request.param("updated_at__gte", dateUtc.toString());
        }
        Json response = Json.read(request.send().getContentAsString());
        List<String> deletedDevices = new ArrayList<>(response.asList().size());
        for (Json deletedDevice : response) {
            if (deletedDevice.isString()) {
                deletedDevices.add(deletedDevice.asString());
            } else if (deletedDevice.isObject()) {
                deletedDevices.add(deletedDevice.get("uuid").asString());
            }
        }
        return deletedDevices;
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
                deviceUuidsInConnections.add(device.get("uuid").asString());
            }
        }
        //endregion

        //region Check for the ones missing on one side or the other
        List<Integer> connectionsToGet = new ArrayList<>();
        List<String> devicesToGet = new ArrayList<>();

        for (int connectionId : connectionUuidsInDevices) {
            if (getConnectionById(connections, connectionId) == null) {
                connectionsToGet.add(connectionId);
            }
        }
        for (String deviceId : deviceUuidsInConnections) {
            if (getDeviceById(devices, deviceId) == null) {
                devicesToGet.add(deviceId);
            }
        }
        //endregion

        //region Get the missing parts
        try {
            connections.addAll(fetchConnectionsFromDbapi(connectionsToGet));
            devices.addAll(fetchDevicesFromDbapi(devicesToGet));
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace(System.err);
        }
        //endregion

        //region Build the entities to send
        List<Json> protocols = null;
        try {
            protocols = fetchDiscoveryProtocolsFromDbapi();
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace(System.err);
        }

        List<Json> scanEntities = new ArrayList<>();
        for (Json device : devices) {
            if (!device.has("connection") || device.get("connection").isNull()) {
                continue;
            }
            Json connection = getConnectionById(connections, device.get("connection").asInteger());
            Json scanEntity = Json.object(
                    "id", device.get("id"),
                    "name", device.get("name"),
                    "ip", device.get("ip"),
                    "port", connection.get("port"),
                    "protocol", getProtocolById(protocols, connection.get("protocol").asInteger()),
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
     * Get a connection in a {@link List<Json>} from its id.
     *
     * @param connections The list of connections to search
     * @param id          The ID of the connection we seek.
     * @return The {@link Json} of the connection we sought, or null if not found.
     */
    private static Json getConnectionById(List<Json> connections, int id) {
        for (Json connection : connections) {
            if (connection.get("id").asInteger() == id) {
                return connection;
            }
        }
        return null;
    }

    /**
     * Get a device in a {@link List<Json>} from its id.
     *
     * @param devices The list of devices to search
     * @param id      The ID of the device we seek.
     * @return The {@link Json} of the devices we sought, or null if not found.
     */
    private static Json getDeviceById(List<Json> devices, String id) {
        for (Json device : devices) {
            if (device.get("id").asString().equals(id)) {
                return device;
            }
        }
        return null;
    }

    /**
     * Get a protocol in a {@link List<Json>} from its id.
     *
     * @param protocols The list of protocols to search
     * @param id        The ID of the protocol we seek.
     * @return The {@link Json} of the protocols we sought, or null if not found.
     */
    private static String getProtocolById(List<Json> protocols, int id) {
        String name;
        for (Json protocol : protocols) {
            if (protocol.get("id").asInteger() == id) {
                name = protocol.get("name").asString().toLowerCase();
                if (name.equals("snmpv1")) {
                    return "SNMPV1";
                } else if (name.equals("snmpv2c")) {
                    return "SNMPV2c";
                } else if (name.equals("snmpv3")) {
                    return "SNMPV3";
                }
                return name;
            }
        }
        return null;
    }

    /**
     * Parse a device as received from DB-API and return a format suitable for our use.
     *
     * @param dbapiDevice The device as we got it from DB-API.
     * @return A {@link Json} with a format suitable for further processing:
     * <code>
     * {
     * "id": uuid (String),
     * "name": name (String),
     * "ip": IP address (String)
     * }
     * </code>
     */
    private static Json parseDbapiDevice(Json dbapiDevice) {
        Json device = Json.object(
                "id", dbapiDevice.get("uuid"),
                "name", dbapiDevice.get("name")
        );
        if (dbapiDevice.has("ipv4")) {
            device.set("ip", dbapiDevice.get("ipv4"));
        } else if (dbapiDevice.has("ipv6")) {
            device.set("ip", dbapiDevice.get("ipv6"));
        }
        Json connectionsIdJson = dbapiDevice.get("connections");
        Integer connectionId = null;
        if (connectionsIdJson != null) {
            if (connectionsIdJson.isArray() && !connectionsIdJson.asJsonList().isEmpty()) {
                connectionId = connectionsIdJson.asJsonList().get(0).asInteger();
            }
        }
        device.set("connection", connectionId);
        return device;
    }

    /**
     * Parse a connection as received from DB-API and return a format suitable for our use.
     *
     * @param dbapiConnection The connection as we got it from DB-API.
     * @return A {@link Json} with a format suitable for further processing:
     * <code>
     * {
     * "id": uuid (int),
     * "protocol": id (int),
     * "port": port number (int),
     * "devices": device uuids list ([String]),
     * "community": SNMP community (String)
     * }
     * </code>
     */
    private static Json parseDbapiConnection(Json dbapiConnection) {
        Json connection = Json.object(
                "id", dbapiConnection.get("id")
        );
        for (Map.Entry<String, String> field : connectionFieldsDbapiToLocal.entrySet()) {
            String key = field.getKey();
            String value = field.getValue();
            if (dbapiConnection.has(key) && !dbapiConnection.isNull()) {
                connection.set(value, dbapiConnection.get(key));
            }
        }

        Json otherData = dbapiConnection.get("other_data");
        String authString = "noAuth";
        String privacyString = "NoPriv";
        if (otherData.has("privacy_algorithm")) {
            privacyString = "Priv";
        }
        if (otherData.has("authentication_algorithm")) {
            authString = "auth";
        }
        connection.set("securityLevel", authString + privacyString);

        return connection;
    }

    /**
     * Fetch a list of connections from DB-API.
     *
     * @param ids The {@link List<Integer>} of IDs of connections to retrieve from DB-API
     * @return The {@link List<Json>} of connections fetched from DB-API
     * @throws ExecutionException   If the fetch failed.
     * @throws InterruptedException If the connection with DB-API was interrupted.
     * @throws TimeoutException     If the connection with DB-API times out.
     */
    private List<Json> fetchConnectionsFromDbapi(List<Integer> ids) throws ExecutionException, InterruptedException, TimeoutException {
        List<Json> connections = new ArrayList<>();
        for (int id : ids) {
            Request request = createDbapiRequest(HttpMethod.GET, "/connections");
            request.param("id", String.valueOf(id));
            Json response = Json.read(request.send().getContentAsString());
            if (response.isArray()) {
                connections.add(parseDbapiConnection(response.at(0)));
            } else {
                connections.add(parseDbapiConnection(response));
            }
        }
        return connections;
    }

    /**
     * Fetch a list of devices from DB-API.
     *
     * @param uuids The {@link List<Integer>} of IDs of devices to retrieve from DB-API
     * @return The {@link List<Json>} of devices fetched from DB-API
     * @throws ExecutionException   If the fetch failed.
     * @throws InterruptedException If the connection with DB-API was interrupted.
     * @throws TimeoutException     If the connection with DB-API times out.
     */
    private List<Json> fetchDevicesFromDbapi(List<String> uuids) throws ExecutionException, InterruptedException, TimeoutException {
        List<Json> devices = new ArrayList<>();
        if (uuids == null || uuids.isEmpty()) {
            return devices;
        }
        Request request = createDbapiRequest(HttpMethod.GET, "/devices");
        request.param("uuid", String.join(",", uuids));
        for (Json device : Json.read(request.send().getContentAsString()).asJsonList()) {
            devices.add(parseDbapiDevice(device));
        }
        return devices;
    }

    /**
     * Fetch the list discovery protocols from DB-API.
     *
     * @return The {@link List<Json>} of protocols fetched from DB-API
     * @throws ExecutionException   If the fetch failed.
     * @throws InterruptedException If the connection with DB-API was interrupted.
     * @throws TimeoutException     If the connection with DB-API times out.
     */
    private List<Json> fetchDiscoveryProtocolsFromDbapi() throws ExecutionException, InterruptedException, TimeoutException {
        Request request = createDbapiRequest(HttpMethod.GET, "/cpe_discovery_api");
        return Json.read(request.send().getContentAsString()).asJsonList();
    }

    /**
     * Create a DB-API request. Most notably, adds the API key to the request header.
     *
     * @param method   The HTTP method to use (GET, POST, ...)
     * @param endpoint The endpoint to contact in DB-API.
     * @return The crafted {@link Request}.
     */
    private Request createDbapiRequest(HttpMethod method, String endpoint) {
        return dbapiHttpClient.newRequest(dbapiUrl + endpoint)
                .method(method)
                .header(HttpHeader.AUTHORIZATION, "Api-Key " + apiKey);
    }

    /**
     * Translate a date from DB-API's datetime format to LocalDateTime.
     *
     * @param dateTime A serialized date as received from DB-API.
     * @return The {@link LocalDateTime} corresponding to the date provided by DB-API.
     */
    LocalDateTime dbapiDateToLocal(String dateTime) {
        return dbapiDateToLocal(OffsetDateTime.parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    /**
     * Translate a date from DB-API's datetime format to LocalDateTime.
     *
     * @param dateTime An {@link OffsetDateTime} date as received from DB-API.
     * @return The {@link LocalDateTime} corresponding to the date provided by DB-API.
     */
    LocalDateTime dbapiDateToLocal(OffsetDateTime dateTime) {
        return dateTime.atZoneSameInstant(zoneId).toLocalDateTime();
    }

    /**
     * Translate local date to UTC, as used by CSL-Scan.
     *
     * @param localDateTime
     * @return
     */
    OffsetDateTime localTimeToUtc(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        OffsetDateTime utcDateTime = OffsetDateTime.parse(localDateTime.atZone(zoneId).toInstant().toString());
        return utcDateTime;
    }
}
