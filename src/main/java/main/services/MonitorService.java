package main.services;

import com.csl.ids.Tap;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.json.Json;
import main.services.endpoints.MonitorEndpoints;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import static main.services.TapsServices.configureTaps;

public class MonitorService extends Service {
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
		super(name,description,configFileSectionName);
	}

	private void defineServiceEndpoints() {
		activeTaps = configureTaps();

		addCmd(MonitorEndpoints.STATS_TAPS, new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				Json j=Json.object();
				j.set("all", 5);
				j.set("running",4);

				return j;
			}
		}		);

		addCmd(MonitorEndpoints.SET_INTERFACES_MONITOR_TAP, this::setInterfaces		);

		addCmd(MonitorEndpoints.GET_INTERFACES_MONITOR_TAP,this::getInterfaces		);

	}

	/**
	 * Initialization of the Monitor commands
	 * @return true if the initialization happened with no problems, false otherwise.
	 */
	@Override
	public boolean init() {
		defineServiceEndpoints();
		return true;  // ok to start
	}

	static private  String readAnyFile(String path) {
		String content = "";

		try
		{
			content = new String ( Files.readAllBytes( Paths.get(path) ) );
		} 
		catch (IOException e) 
		{
			// e.printStackTrace();
			return "{\"Error\":\"File not found:"+e.getMessage()+"\"}";
		}

		return content;
	}

	static private Json loadAnyFileAsJson(String fullname) {


		String result="";
		Json j=Json.object();

		if (fullname!=null) {
			result=readAnyFile(fullname);
			Json z=Json.read(result);
			j.set("contents",z);
//			IDSTrace.log(IDSTrace.WEB_DATABASE,
//					"File Contents="+result);

		} else {
			j.set("contents",Json.object());
			j.set("error", "Nof file with name:"+fullname);
//			IDSTrace.log(IDSTrace.WEB_DATABASE,
//					"File Load error="+j.toString());

		}

		return j;
	}

	public static String startOf(String s) {
		int MAX=50;
		if (s.length()<=MAX) return s;
		else return s.substring(0,MAX-1)+"...";
	}

	/**
	 * Get the monitoring interfaces of the given tap
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
					Json.read("{\"cmd\":\"monitorGetInterfaces\"}")).toJson().get("result");
		}

	/**
	 * Set the monitoring interfaces of the given tap
	 * @param params the parameters of the request, with the 'name' of the tap and a list of 'interfaces'
	 * @return the list of the monitoring interfaces of the given tap
	 */
	public Json setInterfaces(Json params) {
		{
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
				return JsonApiResponse.error("Interfaces must be a list").toJson().get("result");
			}

			return activeTaps.get(name).sendCmd("/monitoring",
					Json.read("{\"cmd\":\"monitorSetInterfaces\",\"params\":{\"interfaces\":"+interfaces+"}}")).toJson().get("result");
		}
	}
}
