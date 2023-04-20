package main.services;

import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
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

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service in charge of the SNMP manager microservice.
 * It should expose an API to request a scan and fetch the database.
 * It also allows to know the current status of the requested scans.
 */
public class DiscoveryServices implements ICSLService {
    static private final String defaultConfigFileSectionName = "discovery";
    static private final String defaultName = "discovery";

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
    private ScanWebSocketHandler scanWebSocketHandler = null;
    private String dbapiUrl;
    private String apiKey;


    public DiscoveryServices(String name, String configFileSectionName) {
        this.name = name;
        this.configFileSectionName = configFileSectionName;
        this.lastCpeItemModification = LocalDateTime.parse("2023-04-10T10:25:01.808");
    }

    public DiscoveryServices() {
        this(defaultName, defaultConfigFileSectionName);
    }

    /**
     * Encode a list of parameters to be used as GET parameters.
     *
     * @param params A {@link Json} object containing a dictionary of parameters.
     * @return A {@link String} with the encoded parameters.
     */
    static private String urlEncode(Json params) {
        StringBuilder getArgs_builder = new StringBuilder();
        boolean started = false;
        for (Map.Entry<String, Json> entry : params.asJsonMap().entrySet()) {
            if (!started) {
                started = true;
            } else {
                getArgs_builder.append('&');
            }
            getArgs_builder.append(entry.getKey());
            getArgs_builder.append("=");
            if (entry.getValue().isString()) {
                getArgs_builder.append(entry.getValue().asString());
            } else {
                getArgs_builder.append(entry.getValue().toString());
            }
        }
        return getArgs_builder.toString();
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
     * Requests a scan of a host by its IP address.
     * Currently just a placeholder.
     *
     * @param targetIps A {@link Json} containing the IP to be scanned. Can be a {@link Json} array or a {@link Json} string, ignored otherwise.
     * @return {@link Json} message indicating that the scan has been registered in the scan manager together with a list of ids for future status requests.
     */
    public Json scanHosts(Json targetIps) {
        Json requestParams = Json.object();
        if (targetIps == null) {
            requestParams.set("ips", Json.array());
        } else if (targetIps.isArray()) {
            requestParams.set("ips", targetIps);
        } else if (targetIps.isString()) {
            requestParams.set("ips", Json.array().add(targetIps));
        } else {
            requestParams.set("ips", Json.array());
        }
        return sendRequestToScanManager(HttpMethod.POST, "/scan", requestParams);
    }

    /**
     * Fetches the current status of a scan.
     *
     * @param ids The IDs of the scan. Can be in a {@link Json} array of integers or a single integer. Ignored otherwise.
     * @return {@link Json} message containing the current status of the scan.
     */
    public Json getScansStatus(Json ids) {
        Json requestParams = Json.object();
        if (ids == null) {
            requestParams.set("ids", Json.array());
        } else if (ids.isArray()) {
            requestParams.set("ids", ids);
        } else if (ids.isNumber()) {
            requestParams.set("ids", Json.array().add(ids));
        } else {
            requestParams.set("ids", Json.array());
        }
        return sendRequestToScanManager(HttpMethod.GET, "/scan_status", requestParams);
    }

    public Json getData() {
        return sendRequestToScanManager(HttpMethod.GET, "/data", Json.object());
    }

    public Json getDataSince(long startTime) {
        return sendRequestToScanManager(HttpMethod.GET, "/data", Json.object("start_time", startTime));
    }

    /**
     * Create a new entity in the scanner
     *
     * @param uuid      The uuid to give to the new enyity, in a {@link String}
     * @param name      The name to give to the new entity, in a {@link String}
     * @param ip        The IP address of the entity, in a {@link String}
     * @param port      The SNMP port
     * @param protocol  The SNMP version, should be either "SNMPV2c" or "SNMPV3"
     * @param community The SNMP community to contact the entity
     * @return A {@link Json} containing the newly created entity, as handed by the scanner
     */
    public Json addEntity(String uuid, String name, String ip, int port, String protocol, String community) {
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
                            "queryProtocol", protocol,
                            "community", community
                    )
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
     * @param id        The unique identifier of the entity, as returned at creation or in a list.
     * @param name      A {@link String} with the new name of the entity. Unchanged if null.
     * @param ip        A {@link String} with the new IP address of the entity. Unchanged if null.
     * @param port      An int with the new SNMP port. Unchanged if 0.
     * @param protocol  The new SNMP version. Unchanged if null.
     * @param community The new SNMP community. Unchanged if null.
     * @return The old version of the entity, not reflecting the changes made.
     */
    public Json updateEntity(String id, String name, String ip, int port, String protocol, String community) {
        Json params = getEntity(id);
        if (name != null) {
            params.set("name", name);
        }
        if (ip != null) {
            params.set("ipAddress", ip);
        }
        if (port != 0) {
            params.set("port", port);
        }
        if (protocol != null) {
            params.get("connectionInfo").set("queryProtocol", protocol);
        }
        if (community != null) {
            params.get("connectionInfo").set("community", community);
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
        Json cpeItems = Json.array();
        if (date == null) {
            cpeItems = sendRequestToScanManager(HttpMethod.GET, "/cpeItem/", Json.object());
        } else {
            cpeItems = sendRequestToScanManager(HttpMethod.GET, "/cpeItem/", Json.object("date", date.toString()));
        }
        if (!cpeItems.asList().isEmpty()) {
            // Update lastCpeItemModification.
            // Currently, computed locally, should retrieve it from scan service latter.
            for (Json cpeItem : cpeItems.asJsonList()) {
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
        status.set("websocket", scanWebSocketHandler.getStatus());

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

    public Json handleDbapiDeviceChange(LocalDateTime date) {
        // TODO finish the function
        try {
            List<Json> changes = getDbapiDevicesSince(date);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return Json.object("result", "NOK",
                    "error", "Could not get changes from DBAPI");
        }
        return Json.object();
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
//        String idsconf = CSLContext.instance.getCslConfDir();

        String scanManagerIp = JsonUtil.getStringFromJson(jConfig, "manager_ip", "localhost");
        int scanManagerPort = JsonUtil.getIntFromJson(jConfig, "manager_port", 8010);

        scanManagerProtocol = JsonUtil.getStringFromJson(jConfig, "manager_protocol", "http");
        if (scanManagerProtocol == "https") {
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
        scanWebSocketHandler = new ScanWebSocketHandler(this, scanManagerDiscoveryUrl);

        Json globalConfig = CSLContext.instance.getConfig().get("global");
        if (globalConfig != null) {
            dbapiUrl = JsonUtil.getBooleanFromJson(globalConfig, "use_ssl", true) ? "https://" : "http://";
            dbapiUrl += JsonUtil.getStringFromJson(globalConfig, "ip_server_remote", "localhost");
            dbapiUrl += "/api";
            apiKey = JsonUtil.getStringFromJson(globalConfig, "api_key", "");
        } else {
            dbapiUrl = "https://localhost/api";
        }

//        addCmd("scan_hosts", params -> scanHosts(params.get("ips")));
//        addCmd("scans_status", params -> getScansStatus(params.get("ids")));
//        addCmd("get_data", params -> params.has("start_time")
//                                            ? getDataSince(params.at("start_time").asLong())
//                                            : getData());
        addCmd("get_status", params -> getStatus());
        addCmd("add_entity", params -> addEntity(
                        JsonUtil.getStringFromJson(params, "id", null),
                        JsonUtil.getStringFromJson(params, "name", null),
                        JsonUtil.getStringFromJson(params, "ip", null),
                        JsonUtil.getIntFromJson(params, "port", 161),
                        JsonUtil.getStringFromJson(params, "protocol", "SNMPV2c"),
                        JsonUtil.getStringFromJson(params, "community", "public")
                )
        );
        addCmd("list_entities", params -> listEntities());
        addCmd("get_entity", params -> getEntity(params.get("id").asString()));
        addCmd("update_entity", params -> updateEntity(
                        params.get("id").asString(),
                        JsonUtil.getStringFromJson(params, "name", null),
                        JsonUtil.getStringFromJson(params, "ip", null),
                        JsonUtil.getIntFromJson(params, "port", 0),
                        JsonUtil.getStringFromJson(params, "protocol", null),
                        JsonUtil.getStringFromJson(params, "community", null)
                )
        );
        addCmd("delete_entity", params -> deleteEntity(params.get("id").asString()));
        addCmd("get_all_cpes", params -> getAllCpes());
        addCmd("get_entity_cpes", params -> getEntityCpes(params.get("id").asString()));
        addCmd("get_cpes_since", params -> getCpeItemChangesSince(LocalDateTime.parse(JsonUtil.getStringFromJson(params, "date", null))));
        addCmd("global_status", params -> getServiceStatus());
        addCmd("scan_status", params -> getScanStatus(params.get("id").asString()));
        addCmd("entity_scan_status", params -> getEntityScanStatus(params.get("id").asString()));
        addCmd("start_scan", params -> {
            if (params.has("entities")) {
                List<String> entities = new ArrayList<>();
                for (Json entity : params.get("entities").asJsonList()) {
                    if (entity.isString()) {
                        entities.add(entity.asString());
                    }
                }
                return scanWebSocketHandler.requestScan(entities);
            } else {
                return scanWebSocketHandler.requestScan(new ArrayList<>());
            }
        });
        addCmd("get_last_cpe_items", params -> {
            handleCpeItemChanges();
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
            scanWebSocketHandler.stop();
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

    public String addCmd(String name, IJsonCmd cmd, IJsonCmdHelp help) {
        return apiCommands.registerCmd(name, cmd, help);
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

        try {
            switch (method) {
                case GET:
                case DELETE:
                    if (!params.asMap().isEmpty()) {
                        URI = scanManagerApiUrl + endpoint + '?' + urlEncode(params);
                    }
                    request = scanHttpClient.newRequest(URI);
                    request.method(method);
                    System.out.println(method.asString() + " " + URI);
                    break;

                case POST:
                case PUT:
                    System.out.println(method.asString() + " " + URI);
                    System.out.println("Payload: " + params.toString());
                    request = scanHttpClient.newRequest(URI);
                    request.method(method);
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

    public LocalDateTime getCpeItemDateTime(Json cpeItem) {
        return LocalDateTime.parse(JsonUtil.getStringFromJson(cpeItem, "updatedAt", lastCpeItemModification.toString()));
    }

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

    private void sendCpeItemsToDbapi(Json cpeItems) throws Exception {
        for (Json cpeItem : cpeItems.asJsonList()) {
            sendCpeItemToDbapi(cpeItem);
        }
    }

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
        return OffsetDateTime.parse(lastUpdatedDateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
    }

    private List<Json> getDbapiDevicesSince(LocalDateTime date) throws Exception {
        Request request = createDbapiRequest(HttpMethod.GET, "/devices/since");
        request.param("date", date.toString());
        Json response = Json.read(request.send().getContentAsString());

        return response.asJsonList();
    }

    private Request createDbapiRequest(HttpMethod method, String endpoint) {
        return dbapiHttpClient.newRequest(dbapiUrl + endpoint)
                .method(method)
                .header(HttpHeader.AUTHORIZATION, "Api-Key " + apiKey);
    }
}
