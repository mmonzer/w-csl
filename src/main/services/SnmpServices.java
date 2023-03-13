package main.services;

//import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.ucsl.interfaces.*;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import java.util.Map;

/**
 * Service in charge of the SNMP manager microservice.
 * It should expose an API to request a scan and fetch the database.
 * It also allows to know the current status of the requested scans.
 */
public class SnmpServices implements ICSLService {
    static private final String defaultConfigFileSectionName = "snmp";
    static private final String defaultName = "snmp";

    private final IApiCommands apiCommands= new ApiCommandsFactory().createApiCommands("");
    private final String name;
    private final String configFileSectionName;
    private String managerUrl;


    public SnmpServices(String name, String configFileSectionName) {
        this.name = name;
        this.configFileSectionName = configFileSectionName;
    }

    public SnmpServices() {
        this(defaultName, defaultConfigFileSectionName);
    }

    /**
     * Requests a scan of a host by its IP address.
     * Currently just a placeholder.
     *
     * @param targetIp A {@link String} containing the IP to be scanned.
     * @return {@link Json} message indicating that the scan has been registered in the manager.
     */
    public Json scanHost(String targetIp) {
        Json requestParams = Json.object();
        requestParams.set("ip", targetIp);
        return sendRequestToManager(HttpMethod.POST, "/scan", requestParams);
    }

    /**
     * Fetches the current status of a scan.
     *
     * @param id The ID of the scan.
     * @return {@link Json} message containing the current status of the scan.
     */
    public Json getScanStatus(int id) {
        Json requestParams = Json.object();
        requestParams.set("id", id);
        return sendRequestToManager(HttpMethod.GET, "/scan_status", requestParams);
    }

    public Json getData() {
        return sendRequestToManager(HttpMethod.GET, "/data", Json.object());
    }

    public Json getDataSince(long startTime) {
        return sendRequestToManager(HttpMethod.GET, "/data", Json.object("start_time", startTime));
    }

    /**
     * Initialize the service, setting the list of known managers and registering the commands.
     *
     * @param jConfig The configuration of the service (that is, the relevant section of the config file)
     * @param cslDir The working directory
     * @return True is the initialization was successful, false otherwise
     */
    @Override
    public boolean init(Json jConfig, String cslDir) {
        System.out.println("Initializing SNMP service ..");
//        String idsconf = CSLContext.instance.getCslConfDir();

        String managerIp = JsonUtil.getStringFromJson(jConfig, "manager_ip", "localhost");
        int managerPort = JsonUtil.getIntFromJson(jConfig, "manager_port", 8010);

        managerUrl = "http://" + managerIp + ":" + managerPort;

        addCmd("scan_host", params -> scanHost(JsonUtil.getStringFromJson(params, "ip", "localhost")));
        addCmd("scan_status", params -> getScanStatus(JsonUtil.getIntFromJson(params, "id", 1)));
        addCmd("get_data", params -> params.has("start_time")
                                            ? getDataSince(params.at("start_time").asLong())
                                            : getData());

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

    @Override
    public boolean terminate() {
        return false;
    }

    public String addCmd(String name, IJsonCmd cmd) {
        return apiCommands.registerCmd(name, cmd);
    }

    public String addCmd(String name, IJsonCmd cmd, IJsonCmdHelp help) {
        return apiCommands.registerCmd(name, cmd, help);
    }

    private enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE,
        OPTIONS,
        HEAD
    }
    static private String urlEncode(Json params) {
       StringBuilder getArgs_builder = new StringBuilder();
        boolean started = false;
        for (Map.Entry<String, Json> entry: params.asJsonMap().entrySet()) {
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

    private Json sendRequestToManager(HttpMethod method, String endpoint, Json params) {
        Json res = Json.object();
        switch(method) {
            case GET:
                String URI;
                if (params.asMap().isEmpty()) {
                    URI = managerUrl + endpoint;
                } else {
                    URI = managerUrl + endpoint + '?' + urlEncode(params);
                }
                System.out.println("GET " + URI);
                res.set("result", "pending");
                break;
            case POST:
                System.out.println("POST " + managerUrl);
                System.out.println("Payload: " + urlEncode(params));
                res.set("result", "OK");
                res.set("id", 1);
                break;
            default:
                System.out.println("Method not yet supported: " + method);
        }
        return res;
    }
}
