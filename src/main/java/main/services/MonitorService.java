package main.services;

import com.csl.ids.Tap;
import com.ucsl.json.Json;
import main.services.endpoints.MonitorEndpoints;

import java.util.HashMap;

import static main.services.TapsServices.configureTaps;

public class MonitorService extends Service {
    public static final String RESULT = "result";
    static HashMap<String, Tap> activeTaps = new HashMap<>();

    /**
     * Default constructor of the Monitor service.
     */
    public MonitorService() {
        this("monitor",
                "Service for getting a general view fo the different devices and CSL-Probes",
                "ids_conf");
    }

    /**
     * Generic constructor of the Monitor service.
     */
    public MonitorService(String name, String description, String configFileSectionName) {
        super(name, description, configFileSectionName);
    }

    private void defineServiceEndpoints() {
        activeTaps = configureTaps();

        addCmd(MonitorEndpoints.STATS_TAPS, params -> {
            Json j = Json.object();
            j.set("all", 5);
            j.set("running", 4);

            return j;
        });

        addCmd(MonitorEndpoints.SET_INTERFACES_MONITOR_TAP, this::setInterfaces);

        addCmd(MonitorEndpoints.GET_INTERFACES_MONITOR_TAP, this::getInterfaces);
    }

    /**
     * Initialization of the Monitor commands
     *
     * @return true if the initialization happened with no problems, false otherwise.
     */
    @Override
    public boolean init() {
        defineServiceEndpoints();
        return true;  // ok to start
    }

    /**
     * Get the monitoring interfaces of the given tap
     *
     * @param params the parameters of the request, with the name of the tap
     * @return the list of the monitoring interfaces of the given tap
     */
    public Json getInterfaces(Json params) {
        // Check if name of tap ok
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }

        return activeTaps.get(name).sendCmd("/monitoring",
                Json.read("{\"cmd\":\"monitorGetInterfaces\"}")).toJson().get(RESULT);
    }

    /**
     * Set the monitoring interfaces of the given tap
     *
     * @param params the parameters of the request, with the 'name' of the tap and a list of 'interfaces'
     * @return the list of the monitoring interfaces of the given tap
     */
    public Json setInterfaces(Json params) {

        // Check if name of tap ok
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("Tap's name missing from params").toJson();
        }
        String name = params.get("name").asString();
        if (!activeTaps.containsKey(name)) {
            return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
        }
        // Check if interfaces ok
        if (!params.has("interfaces")) {
            return JsonApiResponse.error("List of new interfaces must be included as 'interfaces'").toJson();
        }
        Json interfaces = params.get("interfaces");
        if (!interfaces.isArray()) {
            return JsonApiResponse.error("Interfaces must be a list").toJson().get(RESULT);
        }

        return activeTaps.get(name).sendCmd("/monitoring",
                Json.read("{\"cmd\":\"monitorSetInterfaces\",\"params\":{\"interfaces\":" + interfaces + "}}")).toJson().get(RESULT);
    }
}
