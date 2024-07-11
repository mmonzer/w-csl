package main.services;

import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.json.Json;

public class CSLService extends Service {
	/**
	 * Default constructor of the second Demo service.
	 */
	public CSLService() {
		this("my_service",
		 "my_service description",
				"my_service");
	}

	/**
	 * Generic constructor of the second Demo service.
	 */
	public CSLService(String name, String description, String configFileSectionName) {
		super(name, description, configFileSectionName);
	}

	/**
	 * Initialization of a second Demo service commands
	 * @param jConfig the configuration section of the configuration file
	 * @param cslDir the CSL directory
	 * @return true if the initialization happened with no problems, false otherwise.
	 */
	public boolean init(Json jConfig, String cslDir) {
		addCmd("test", new IJsonCmd() {
			
			@Override
			public Json exec(Json params) {
				// TODO Auto-generated method stub
				System.out.println("it works");
				Json j= Json.object();
				j.set("rep",  "ok");
				
				return j;
			}
		});
		return true;  // ok to start
	}
}
