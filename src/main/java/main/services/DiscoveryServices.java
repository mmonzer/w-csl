package main.services;

import com.csl.core.CSLContext;
import com.csl.intercom.broker.CSLMqttBrokerHandler;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.cslscan.ScanWebSocketHandler;
import com.csl.intercom.cslscan.enums.DynamicDiscoveryFrequencyOption;
import com.csl.intercom.cslscan.models.CpeItem;
import com.csl.intercom.cslscan.models.EntityHttpConnection;
import com.csl.intercom.cslscan.models.EntityHttpConnectionTestResult;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.dbapi.enums.HttpConnectionField;
import com.csl.intercom.dbapi.enums.RemotePowershellConnectionField;
import com.csl.intercom.dbapi.enums.SNMPv3ConnectionField;
import com.csl.intercom.dbapi.enums.SshConnectionField;
import com.csl.intercom.dbapi.models.*;
import com.csl.intercom.services.*;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.csl.intercom.status.IStatusProvider;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.endpoints.DiscoveryEndpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Service in charge of the SNMP manager microservice.
 * It should expose an API to request a scan and fetch the database.
 * It also allows to know the current status of the requested scans.
 */
public class DiscoveryServices extends Service implements IStatusProvider {
    static private final String defaultConfigFileSectionName = "discovery";
    static private final String defaultName = "discovery";

    //    private static final Logger logger = LoggerFactory.getLogger(DiscoveryServices.class);
    private static final Logger logger = LoggerFactory.getLogger(DiscoveryServices.class);
    private final boolean isConcentrator;
    private ScanWebSocketHandler scanWebSocketHandler = null;
    //    private String apiKey;
    private DbapiHandlerForCSLScan dbapiHandler = null;
    private ScanApiHandler scanApiHandler = null;
    private CSLMqttBrokerHandler mqttBroker = null;
    private DataSynchronizationService cpeItemSynchronizationService = null;
    private DataSynchronizationService microsoftKbSynchronizationService = null;
    private DataSynchronizationService deletedCpeItemsSynchronizationService = null;
    private DataSynchronizationService deletedMicrosoftKbsSynchronizationService = null;
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
     * @param jConfig The configuration of the service (that is, the relevant section of the config file)
     * @param cslDir  The working directory
     * @return True is the initialization was successful, false otherwise
     */
    @Override
    public boolean init(Json jConfig, String cslDir) {
        logger.info("Initializing SNMP service ..");

        String scanManagerDiscoveryUrl = ScanUtils.generateScanDiscoveryUrlFromConfig(jConfig);

        dbapiHandler = new DbapiHandlerForCSLScan();
        scanApiHandler = new ScanApiHandler();

        Json globalConfig = CSLContext.instance.getConfig().get("global");

        if (isConcentrator) {
            cpeScanService = new CpeScanService();
            cpeItemSynchronizationService = new CpeItemsSynchronizationService(cpeScanService);
            microsoftKbSynchronizationService = new MicrosoftKbSynchronizationService(cpeScanService);
            deletedCpeItemsSynchronizationService = new DeletedCpeItemsSynchronizationService();
            deletedMicrosoftKbsSynchronizationService = new DeletedMicrosoftKbsSynchronizationService();
            cpeScanService.init(cpeItemSynchronizationService, microsoftKbSynchronizationService);
            scanWebSocketHandler = new ScanWebSocketHandler(this, scanManagerDiscoveryUrl, cpeScanService);

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
        }

        synchronizationSchedule = Executors.newScheduledThreadPool(1);
        synchronizationSchedule.scheduleAtFixedRate(this::syncAll, 0, 300, TimeUnit.SECONDS);
        addCmd(DiscoveryEndpoints.GET_STATUS, params -> getStatus());
//        addCmd("add_entity", this::addEntity);
        addCmd(DiscoveryEndpoints.LIST_ENTITIES, params -> scanApiHandler.listEntities().toJson());
        addCmd(DiscoveryEndpoints.GET_ENTITY, params -> scanApiHandler.getEntity(params.get("id").asString()).toJson());
//        addCmd("update_entity", this::updateEntity);
        addCmd(DiscoveryEndpoints.DELETE_ENTITY, params -> scanApiHandler.deleteEntity(params.get("id").asString()).toJson());
        addCmd(DiscoveryEndpoints.GET_ALL_CPES, params -> Json.object("success", true, "result", Json.array(getAllCpes().toArray())));
        addCmd(DiscoveryEndpoints.GET_ENTITY_CPES, params -> scanApiHandler.getEntityCpes(params.get("id").asString()).toJson());
        addCmd(DiscoveryEndpoints.GET_CPES_SINCE, params -> Json.object("success", true, "result", Json.array(scanApiHandler.getCpeItemChangesSince(OffsetDateTime.parse(JsonUtil.getStringFromJson(params, "date", null))).toArray())));
//        addCmd("global_status", params -> getServiceStatus());
        addCmd(DiscoveryEndpoints.SCAN_STATUS, params -> scanApiHandler.getScanStatus(params.get("id").asString()).toJson());
//        addCmd("entity_scan_status", params -> getEntityScanStatus(params.get("id").asString()));
        addCmd(DiscoveryEndpoints.START_SCAN, params -> {
            List<String> entities = new ArrayList<>();
            if (params.has("entities")) {
                for (Json entity : params.get("entities").asJsonList()) {
                    if (entity.isString()) {
                        entities.add(entity.asString());
                    }
                }
            }
            return startScan(entities).toJson();
        });
        addCmd(DiscoveryEndpoints.STOP_SCAN, params -> {
            try {
                this.cpeScanService.cancelScan();
                return JsonApiResponse.success().toJson();
            } catch (Exception e) {
                return JsonApiResponse.error("Could not stop the scan", Json.object("exception", e.getMessage())).toJson();
            }
        });
        addCmd(DiscoveryEndpoints.SYNCHRONIZE_DEVICES, params -> {
            return dbapiHandler.sendNewDevicesToScanner(scanApiHandler).toJson();
        });
        addCmd(DiscoveryEndpoints.DROP_ALL_COLLECTIONS, params -> {
            try {
                scanApiHandler.dropAllCollections();
                return JsonApiResponse.success().toJson();
            } catch (Exception e) {
                logger.error("Could not drop collections", e);
                return JsonApiResponse.error("Could not drop collections",
                        Json.object("exception", e.getMessage())
                ).toJson();
            }
        });
        addCmd(DiscoveryEndpoints.GET_ALL_ENTITY_HTTP_CONNECTIONS, params -> {
            List<EntityHttpConnection> entityHttpConnections = scanApiHandler.getAllEntityHttpConnections(true);
            if (entityHttpConnections == null) {
                return JsonApiResponse.error("Could not fetch entity HTTP connections from CSL-Scan",
                        Json.object("exception", "Could not fetch entity HTTP connections from CSL-Scan")
                ).toJson();
            } else {
                return JsonApiResponse.result(Json.array(entityHttpConnections.stream().map(EntityHttpConnection::serializeForDbapi).toArray())).toJson();
            }
        });
        addCmd(DiscoveryEndpoints.GET_ALL_ENTITY_HTTP_CONNECTIONS_FULL, params -> {
            List<EntityHttpConnection> entityHttpConnections = scanApiHandler.getAllEntityHttpConnections(false);
            if (entityHttpConnections == null) {
                return JsonApiResponse.error("Could not fetch entity HTTP connections from CSL-Scan",
                        Json.object("exception", "Could not fetch entity HTTP connections from CSL-Scan")
                ).toJson();
            } else {
                return JsonApiResponse.result(Json.array(entityHttpConnections.stream().map(EntityHttpConnection::serializeForDbapi).toArray())).toJson();
            }
        });
        addCmd(DiscoveryEndpoints.GET_ENTITY_HTTP_CONNECTION, params -> {
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
            EntityHttpConnection entityHttpConnection = scanApiHandler.getEntityHttpConnection(uuid, true);
            if (entityHttpConnection == null) {
                return JsonApiResponse.error("Could not fetch entity HTTP connection from CSL-Scan",
                        Json.object("exception", "Could not fetch entity HTTP connection from CSL-Scan")
                ).toJson();
            } else {
                return JsonApiResponse.result(entityHttpConnection.serializeForDbapi()).toJson();
            }
        });
        addCmd(DiscoveryEndpoints.GET_ENTITY_HTTP_CONNECTION_FULL, params -> {
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
            EntityHttpConnection entityHttpConnection = scanApiHandler.getEntityHttpConnection(uuid, false);
            if (entityHttpConnection == null) {
                return JsonApiResponse.error("Could not fetch entity HTTP connection from CSL-Scan",
                        Json.object("exception", "Could not fetch entity HTTP connection from CSL-Scan")
                ).toJson();
            } else {
                return JsonApiResponse.result(entityHttpConnection.serializeForDbapi()).toJson();
            }
        });
        addCmd(DiscoveryEndpoints.DELETE_ENTITY_HTTP_CONNECTION, params -> {
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
            EntityHttpConnection entityHttpConnection = scanApiHandler.getEntityHttpConnection(uuid, false);
            try {
                JsonApiResponse response = scanApiHandler.deleteEntityHttpConnection(uuid);
                if (response.isSuccess()) {
                    dbapiHandler.deleteDiscoveryProtocol(uuid);
                } else {
                    scanApiHandler.createEntityHttpConnection(entityHttpConnection);
                    return response.toJson();
                }
            } catch (Exception e) {
                logger.error("Could not delete entity HTTP connection from CSL-Scan", e);
                return JsonApiResponse.error("Could not delete entity HTTP connection from CSL-Scan",
                        Json.object("exception", e.getMessage())
                ).toJson();
            }
            return JsonApiResponse.success().toJson();
        });
        addCmd(DiscoveryEndpoints.ADD_ENTITY_HTTP_CONNECTION, params -> {
            Json entityHttpConnectionJson = params.get("entity_http_connection");
            EntityHttpConnection entityHttpConnection = EntityHttpConnection.fromDbapiJson(entityHttpConnectionJson);
            if (entityHttpConnection == null) {
                return JsonApiResponse.error("Could not parse entity_http_connection",
                        Json.object("exception", "Could not parse entity_http_connection")
                ).toJson();
            }
            JsonApiResponse response;
            if (entityHttpConnection.getUuid() == null) {
                response = scanApiHandler.createEntityHttpConnection(entityHttpConnection);
                if (response.isSuccess()) {
                    EntityHttpConnection createdEntityHttpConnection = EntityHttpConnection.fromDbapiJson(response.getResult());
                    try {
                        if (createdEntityHttpConnection == null) {
                            throw new Exception("Could not parse the created entity_http_connection");
                        }
                        dbapiHandler.createDiscoveryProtocol(createdEntityHttpConnection);
                    } catch (Exception e) {
                        logger.error("Could not create discovery protocol", e);
                        scanApiHandler.deleteEntityHttpConnection(createdEntityHttpConnection.getUuid());
                        response = JsonApiResponse.error("Could not create discovery protocol",
                                Json.object("exception", e.getMessage())
                        );
                    }
                }
            } else {
                EntityHttpConnection previousEntityHttpConnection = scanApiHandler.getEntityHttpConnection(entityHttpConnection.getUuid(), false);
                response = scanApiHandler.updateEntityHttpConnection(entityHttpConnection);
                try {
                    dbapiHandler.updateDiscoveryProtocol(entityHttpConnection);
                } catch (Exception e) {
                    logger.error("Could not update discovery protocol", e);
                    scanApiHandler.updateEntityHttpConnection(previousEntityHttpConnection);
                    response = JsonApiResponse.error("Could not update discovery protocol",
                            Json.object("exception", e.getMessage())
                    );
                }
            }
            return response.toJson();
        });
        addCmd(DiscoveryEndpoints.TEST_CONNECTION, params -> {
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
        });
        addCmd(DiscoveryEndpoints.TEST_NEW_CONNECTION, params -> {
            String ipAddress = JsonUtil.getStringFromJson(params, "ip_address", null);
            Json connectionJson = params.get("connection");
            Json baseConnectionIdJson = params.get("base_connection_id");

            connectionJson.set("id", 0);
            connectionJson.set("connected_devices", Json.array(0));
            List<ConnectionProtocol> protocols;
            try {
                protocols = dbapiHandler.fetchDiscoveryProtocols();
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                logger.error("Could not fetch discovery protocols", e);
                throw new RuntimeException(e);
            }

            // Fetch the password from the base connection if needed
            if (baseConnectionIdJson != null && baseConnectionIdJson.isNumber()) {
                try {
                    Connection baseConnection = dbapiHandler.fetchConnections(List.of(baseConnectionIdJson.asInteger()), protocols).get(0);
                    if (!connectionJson.has("read_only_connection_data")) {
                        connectionJson.set("read_only_connection_data", Json.object());
                    }
                    Json otherDataJson = connectionJson.get("read_only_other_data");
                    switch (baseConnection.getProtocol()) {
                        case SNMPv3:
                            if (!connectionJson.has(SNMPv3ConnectionField.PASSWORD.dbapiName())) {
                                connectionJson.set(SNMPv3ConnectionField.PASSWORD.dbapiName(), ((SNMPv3Connection) baseConnection).getPassword());
                            }
                            if (!otherDataJson.has(SNMPv3ConnectionField.PASSPHRASE.dbapiName())) {
                                otherDataJson.set(SNMPv3ConnectionField.PASSPHRASE.dbapiName(), ((SNMPv3Connection) baseConnection).getPassphrase());
                            }
                            break;
                        case RemotePowershell:
                            if (!connectionJson.has(RemotePowershellConnectionField.PASSWORD.dbapiName())) {
                                connectionJson.set(RemotePowershellConnectionField.PASSWORD.dbapiName(), ((RemotePowershellConnection) baseConnection).getPassword());
                            }
                            break;
                        case SSH:
                            if (!connectionJson.has(SshConnectionField.PASSWORD.dbapiName())) {
                                connectionJson.set(SshConnectionField.PASSWORD.dbapiName(), ((SshConnection) baseConnection).getPassword());
                            }
                            if (!otherDataJson.has(SshConnectionField.PASSPHRASE.dbapiName())) {
                                otherDataJson.set(SshConnectionField.PASSPHRASE.dbapiName(), ((SshConnection) baseConnection).getPassphrase());
                            }
                            if (!otherDataJson.has(SshConnectionField.PRIVATE_KEY.dbapiName())) {
                                otherDataJson.set(SshConnectionField.PRIVATE_KEY.dbapiName(), ((SshConnection) baseConnection).getPrivateKey());
                            }
                            break;
                        case HTTP:
                            if (!connectionJson.has(HttpConnectionField.PASSWORD.dbapiName())) {
                                connectionJson.set(HttpConnectionField.PASSWORD.dbapiName(), ((HttpConnection) baseConnection).getPassword());
                            }
                            // Add the password of the base connection to the stages config
                            Map<Integer, HttpConnection.StageConfig> baseStagesConfig = ((HttpConnection) baseConnection).getStagesConfig();
                            for (Map.Entry<String, Json> stageConfig : otherDataJson.get(HttpConnectionField.STAGES_CONFIG.dbapiName()).asJsonMap().entrySet()) {
                                try {
                                    String stagePassword = baseStagesConfig.get(Integer.parseInt(stageConfig.getKey())).getPassword();
                                    if (stagePassword != null) {
                                        if (!stageConfig.getValue().has(SNMPv3ConnectionField.PASSWORD.dbapiName())) {
                                            stageConfig.getValue().set(SNMPv3ConnectionField.PASSWORD.dbapiName(), stagePassword);
                                        }
                                    }
                                } catch (NullPointerException e) {
                                    continue;
                                }
                            }
                            // Add the inputs from the base connection
                            Map<String, String> baseInputs = ((HttpConnection) baseConnection).getInputs();
                            if (!otherDataJson.has(HttpConnectionField.INPUTS.dbapiName())) {
                                otherDataJson.set(HttpConnectionField.INPUTS.dbapiName(), Json.object());
                            }
                            Json inputsJson = otherDataJson.get(HttpConnectionField.INPUTS.dbapiName());
                            baseInputs.entrySet().stream()
                                    .filter(input -> !inputsJson.has(input.getKey()) || !inputsJson.get(input.getKey()).isString() || !inputsJson.get(input.getKey()).asString().isEmpty())
                                    .forEach(input -> inputsJson.set(input.getKey(), input.getValue()));
                            break;
                        default:
                            break;
                    }
                } catch (ExecutionException | InterruptedException | TimeoutException | IndexOutOfBoundsException |
                         NullPointerException e) {
                    logger.error("Could not fetch base connection", e);
                    return JsonApiResponse.error("Could not fetch base connection",
                            Json.object("exception", e.getMessage())
                    ).toJson();
                }
            }

            Connection connection = Connection.fromDbapiJson(connectionJson, protocols);
            if (ipAddress == null || connection == null) {
                return JsonApiResponse.error("Missing required parameter device or connection",
                        Json.object("exception", "Missing parameter device or connection, of type object")
                ).toJson();
            } else {
                Device device = Device.fromIpAddress(ipAddress);
                device.setConnections(List.of(connection));
                return scanApiHandler.testConnection(device).toJson();
            }
        });
        addCmd(DiscoveryEndpoints.FETCH_HTTP_CONNECTION_STAGE, params -> {
            String ipAddress = JsonUtil.getStringFromJson(params, "ip_address", null);
            String port = JsonUtil.getStringFromJson(params, "port", null);
            String username = JsonUtil.getStringFromJson(params, "username", null);
            String password = JsonUtil.getStringFromJson(params, "password", null);
            String realm = JsonUtil.getStringFromJson(params, "realm", null);
            String token = JsonUtil.getStringFromJson(params, "token", null);

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

            if (ipAddress == null || port == null || entityHttpConnection == null) {
                return JsonApiResponse.error("Missing required parameter ip_address, port or stage",
                        Json.object("exception", "Missing parameter ip_address, port or stage, of type string, int or object")
                ).toJson();
            } else {
                return scanApiHandler.fetchHttpConnectionStage(ipAddress, port, username, password, realm, token, entityHttpConnection, stageIndex).toJson();
            }
        });
        addCmd(DiscoveryEndpoints.GET_PREDEFINED_HTTP_VARIABLES, params -> scanApiHandler.getPredefinedHttpVariables().toJson());
        addCmd(DiscoveryEndpoints.TEST_HTTP_TEMPLATE, params -> {
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

            EntityHttpConnection entityHttpConnection = null;
            Device device = null;
            Connection connection = null;
            List<ConnectionProtocol> protocols;
            try {
                protocols = dbapiHandler.fetchDiscoveryProtocols();
            } catch (Exception e) {
                logger.error("Could not fetch discovery protocols", e);
                return JsonApiResponse.error("Could not fetch discovery protocols from DB-API",
                        Json.object("exception", e.getMessage())
                ).toJson();
            }

            if (connectionJson != null) {
                connectionJson.set("id", connectionId != null ? connectionId : 0);
                connectionJson.set("connected_devices", Json.array(deviceId != null ? deviceId : 0));
                // If the template is provided, use it instead of existing discovery protocols
                if (templateJson != null) {
                    protocols = List.of(ConnectionProtocol.fromTemplateId(templateJson.has("id") ? templateJson.get("id").asString() : null));
                    connectionJson.set("discovery_protocol", protocols.get(0).getId());
                }
                connection = Connection.fromDbapiJson(connectionJson, protocols);
            } else if (connectionId != null) {
                try {
                    List<Connection> connections = dbapiHandler.fetchConnections(List.of(connectionId), protocols);
                    if (!connections.isEmpty()) {
                        connection = connections.get(0);
                    }
                } catch (Exception e) {
                    logger.error("Could not fetch connection from DB-API", e);
                    return JsonApiResponse.error("Could not fetch connection from DB-API",
                            Json.object("exception", e.getMessage())
                    ).toJson();
                }
            } else {
                return JsonApiResponse.error("Missing required parameter connection or connection_id",
                        Json.object("exception", "Missing parameter connection or connection_id, of type object or int")
                ).toJson();
            }

            if (ipAddress != null && connection != null) {
                device = Device.fromIpAddress(ipAddress).setConnectionsIds(List.of(connectionId != null ? connectionId : 0));
                device.setConnections(List.of(connection));
            } else if (deviceId != null && connection != null) {
                try {
                    List<Device> devices = dbapiHandler.fetchDevices(List.of(deviceId));
                    if (!devices.isEmpty()) {
                        device = devices.get(0).setConnectionsIds(List.of(connectionId != null ? connectionId : 0));
                        device.setConnections(List.of(connection));
                    }
                } catch (Exception e) {
                    logger.error("Could not fetch device from DB-API", e);
                    return JsonApiResponse.error("Could not fetch device from DB-API",
                            Json.object("exception", e.getMessage())
                    ).toJson();
                }
            }
            if (templateJson != null) {
                entityHttpConnection = EntityHttpConnection.fromDbapiJson(templateJson);
            }
            EntityHttpConnectionTestResult result = null;
            try {
                result = scanApiHandler.testEntityHttpConnection(templateId, entityHttpConnection, deviceId, device, connectionId);
            } catch (Exception e) {
                logger.error("Could not test entity HTTP connection", e);
                return JsonApiResponse.error("Could not test entity HTTP connection",
                        Json.object("exception", e.getMessage())
                ).toJson();
            }
            if (result == null) {
                return JsonApiResponse.error("Could not test entity HTTP connection",
                        Json.object("exception", "Could not test entity HTTP connection")
                ).toJson();
            } else {
                return JsonApiResponse.result(result.serializeForDbapi()).toJson();
            }
        });
        addCmd(DiscoveryEndpoints.GET_DISCOVERY_CRON, params -> {
            try {
                Json cron = scanApiHandler.getDiscoveryCron();
                if (cron == null) {
                    throw new Exception("Could not fetch discovery cron");
                }
                return JsonApiResponse.result(cron).toJson();
            } catch (Exception e) {
                logger.error("Could not fetch discovery cron", e);
                return JsonApiResponse.error("Could not fetch discovery cron",
                        Json.object("exception", e.getMessage())
                ).toJson();
            }
        });
        addCmd(DiscoveryEndpoints.SET_DISCOVERY_CRON, params -> {
            String cron = null;
            if (params.has("cron") && params.get("cron").isString()) {
                cron = params.get("cron").asString();
            }
            if (cron == null) {
                return JsonApiResponse.error("Missing required parameter cron",
                        Json.object("exception", "Missing parameter cron, of type string")
                ).toJson();
            }
            DynamicDiscoveryFrequencyOption frequencyOption = null;
            if (params.has("frequencyOption") && params.get("frequencyOption").isString()) {
                frequencyOption = DynamicDiscoveryFrequencyOption.fromDbapiName(params.get("frequencyOption").asString());
            }
            try {
                scanApiHandler.setDiscoveryCron(cron, frequencyOption);
                return JsonApiResponse.success().toJson();
            } catch (Exception e) {
                logger.error("Could not set discovery cron", e);
                return JsonApiResponse.error("Could not set discovery cron",
                        Json.object("exception", e.getMessage())
                ).toJson();
            }
        });
        addCmd(DiscoveryEndpoints.IS_DISCOVERY_CRON_ACTIVE, params -> {
            try {
                return JsonApiResponse.result(Json.object("isActive", scanApiHandler.isDiscoveryCronActive())).toJson();
            } catch (Exception e) {
                logger.error("Could not fetch discovery cron status", e);
                return JsonApiResponse.error("Could not fetch discovery cron status",
                        Json.object("exception", e.getMessage())
                ).toJson();
            }
        });
        addCmd(DiscoveryEndpoints.SET_DISCOVERY_CRON_ACTIVE, params -> {
            Boolean isActive = null;
            if (params.has("isActive") && params.get("isActive").isBoolean()) {
                isActive = params.get("isActive").asBoolean();
            }
            if (isActive == null) {
                return JsonApiResponse.error("Missing required parameter isActive",
                        Json.object("exception", "Missing parameter isActive, of type boolean")
                ).toJson();
            }
            try {
                scanApiHandler.setDiscoveryCronActive(isActive);
                return JsonApiResponse.success().toJson();
            } catch (Exception e) {
                logger.error("Could not set discovery cron status", e);
                return JsonApiResponse.error("Could not set discovery cron status",
                        Json.object("exception", e.getMessage())
                ).toJson();
            }
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
        if (isConcentrator) {
            dbapiHandler.sendNewDevicesToScanner(scanApiHandler);
            try {
                cpeItemSynchronizationService.syncData();
                microsoftKbSynchronizationService.syncData();
                deletedCpeItemsSynchronizationService.syncData();
                deletedMicrosoftKbsSynchronizationService.syncData();
            } catch (SynchronizationException e) {
                logger.trace("Could not synchronize CPE Items", e);
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
        // Synchronize devices between DB-API and CSL-Scan
        JsonApiResponse syncResult = dbapiHandler.sendNewDevicesToScanner(scanApiHandler);
        if (!syncResult.isSuccess()) {
            return JsonApiResponse.error(
                    "Could not retrieve devices from DB-API",

                    Json.object("failed_devices", syncResult.getError().getReason())
            );
        }

        // Get deleted CPE Items from DB-API and delete them from CSL-Scan
        JsonApiResponse cpeDeletionResult = JsonApiResponse.success();
        try {
            deletedCpeItemsSynchronizationService.syncData();
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
