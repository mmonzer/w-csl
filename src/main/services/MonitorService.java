package main.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import com.csl.core.CSLContext;
import com.csl.ids.Tap;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.csl.modules.ModuleIDS;
import com.csl.monitor.ActivityMonitor;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.ICSLService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;

import static main.services.TapsServices.readJsonFile;

public class MonitorService extends Service {
	static ArrayList<Json> configuredTaps;
	static HashMap<String, Tap> activeTaps = new HashMap<>();
	static String idsconf;

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

	/**
	 * Initialization of the Monitor commands
	 * @param jConfig the configuration section of the configuration file
	 * @param cslDir the CSL directory
	 * @return true if the initialization happened with no problems, false otherwise.
	 */
	@Override
	public boolean init(Json jConfig, String cslDir) {
		// TODO : duplicated with TapService
		idsconf = CSLContext.instance.getCslConfDir();
		Json conf;
		try {
			conf = readJsonFile(idsconf + "/taps/TapsConfiguration.json");
			if (conf.isArray()) {
				configuredTaps = (ArrayList<Json>) conf.asJsonList();
			} else {
				configuredTaps = new ArrayList<Json>();
			}
			for (Json j : configuredTaps) {
				activeTaps.put(j.at("idname").asString(),
						new Tap(j.at("idname").asString(),
								j.at("id").asString(),
								j.at("ip").asString(),
								j.at("port").asInteger(),
								j.at("includes").asJsonList()
						)
				);
			}
		} catch (IOException e1) {
			System.err.println("No tap config found");
			configuredTaps = new ArrayList<Json>();
		}
		
		addCmd("stats_devices", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
				ActivityMonitor activityMonitor = ids.getActivityMonitor();
				Json j=Json.object();
				j.set("all", 103);
				j.set("running",87);
				return j;
			}
		},
				new JsonCmdHelp()
				.setDesc("return the number of devices ")
				.setResult("{all:, running: ", JsonCmdHelp.JSON)
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);

		addCmd("stats_taps", new IJsonCmd() {

					@Override
					public Json exec(Json params) {
						Json j=Json.object();
						j.set("all", 5);
						j.set("running",4);

						return j;
					}
				},
				new JsonCmdHelp()
						.setDesc("return the number of taps ")
						.setResult("{all:n, running: ", JsonCmdHelp.JSON)
						.setStatus(JsonCmdHelp.STATUS_TODO)
		);

		addCmd("set_interfaces_monitor_tap", this::setInterfaces,
				new JsonCmdHelp()
						.setDesc("Set the new monitoring interfaces of the given tap")
						.setParam("name","name of the tap", JsonCmdHelp.STR)
						.setParam("interfaces","list of the new interfaces", JsonCmdHelp.STR)
						.setResult("list of the new interfaces", JsonCmdHelp.JSON)
						.setStatus(JsonCmdHelp.STATUS_TODO)
		);

		addCmd("get_interfaces_monitor_tap", this::getInterfaces,
				new JsonCmdHelp()
						.setDesc("Ge the monitoring interfaces of the given tap")
						.setParam("name","name of the tap", JsonCmdHelp.STR)
						.setResult("list of the monitored interfaces in the given tap", JsonCmdHelp.JSON)
						.setStatus(JsonCmdHelp.STATUS_TODO)
		);

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
			e.printStackTrace();
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

	static public String startOf(String s) {
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
