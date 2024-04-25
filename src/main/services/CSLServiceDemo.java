package main.services;

import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.ICSLService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.json.Json;

public class CSLServiceDemo extends Service {
	/**
	 * Default constructor of the Demo service.
	 */
	public CSLServiceDemo() {
		this("demo",
				"Demo service with dummy commands to check the api",
				"demo_conf");
	}

	/**
	 * Generic constructor of the Demo service.
	 */
	public CSLServiceDemo(String name, String description, String configFileSectionName) {
		super(name, description, configFileSectionName);
	}

	/**
	 * Initialization of the Demo service commands
	 * @param jConfig the configuration section of the configuration file
	 * @param cslDir the CSL directory
	 * @return true if the initialization happened with no problems, false otherwise.
	 */
	public boolean init(Json jConfig, String cslDir) {
		
		
		addCmd("demo_cmd", new IJsonCmd() {
			
			@Override
			public Json exec(Json params) {
					
				Json j= Json.object();
				j.set("result", "ok");
				
				return j;
			}
		});
		return true;  // ok to start
	}
}
