package main.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
<<<<<<< HEAD
import java.util.ArrayList;
import java.util.HashMap;

import com.csl.core.CSLContext;
import com.csl.ids.Tap;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.csl.modules.ModuleIDS;
import com.csl.monitor.ActivityMonitor;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.json.Json;
import main.services.endpoints.MonitorEndpoints;

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
		Tap tap;
		try {
			conf = readJsonFile(idsconf + "/taps/TapsConfiguration.json");
			if (conf.isArray()) {
				configuredTaps = (ArrayList<Json>) conf.asJsonList();
			} else {
				configuredTaps = new ArrayList<Json>();
			}
			for (Json j : configuredTaps) {
				tap = new Tap(j.at("idname").asString(),
						j.at("id").asString(),
						j.at("ip").asString(),
						j.at("port").asInteger(),
						j.at("includes").asJsonList()
				);
				activeTaps.put(tap.getId(), tap);
			}
		} catch (IOException e1) {
			System.err.println("No tap config found");
			configuredTaps = new ArrayList<Json>();
		} catch (Exception e) {
			System.err.println("Unable to parse conf or No tap config found in " + idsconf + "/taps/TapsConfiguration.json");
			configuredTaps = new ArrayList<Json>();
		}
		
		addCmd(MonitorEndpoints.STATS_DEVICES, new IJsonCmd() {

			@Override
			public Json exec(Json params) {
=======

import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.csl.modules.ModuleIDS;
import com.csl.monitor.ActivityMonitor;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.ICSLService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;
import lombok.Getter;

public class MonitorService implements ICSLService {
	@Getter
    String name="monitor";
	@Getter
	IApiCommands apiCommands= new ApiCommandsFactory().createApiCommands(name);
	@Getter
    String configFileSectionName="config_"+name;

	public MonitorService() {
		this.name="monitor";
		this.configFileSectionName="ids_conf";
	}

    public boolean init(Json jConfig, String cslDir) {
		addCmd("stats_devices", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				
				
>>>>>>> origin/feature/refactor_code
				ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
				ActivityMonitor activityMonitor = ids.getActivityMonitor();
				Json j=Json.object();
				j.set("all", 103);
				j.set("running",87);
<<<<<<< HEAD
				return j;
			}
		}				);

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
=======


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
		
		
		
		
>>>>>>> origin/feature/refactor_code

		return true;  // ok to start
	}

<<<<<<< HEAD
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
=======
	public String addCmd(String name, IJsonCmd j) {
		return apiCommands.registerCmd(name, j);
	}
	
	public String addCmd(String name, IJsonCmd j, IJsonCmdHelp jh) {
		return apiCommands.registerCmd(name, j,jh);
	}

	@Override
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}

>>>>>>> origin/feature/refactor_code
}
