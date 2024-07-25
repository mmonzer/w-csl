package main.services;

import com.ucsl.json.Json;

public class SuricataServices extends Service {

	/**
	 * Default constructor of the Suricata service. Name, description and configuration file are given here.
	 */
	public SuricataServices() {
		this("suricata",
				"DEPRECATED : Service for dealing with suricata configuration.",
				"");
	}

	/**
	 * Generic constructor of the Suricata service.
	 */
	public SuricataServices(String name, String description, String configFileSectionName) {
		super(name, description, configFileSectionName);
	}

	/**
	 * Initialization of the Suricata commands
	 * @param config the configuration section of the configuration file
	 * @param cslDir the CSL directory
	 * @return true if the initialization happened with no problems, false otherwise.
	 */
	@Override
	public boolean init(Json config, String cslDir) {
		
		addCmd("newSuricata", params -> Json.object("newSuricata","DEPRECATED"));
		
		addCmd("renameSuricata", params -> Json.object("renameSuricata","DEPRECATED"));
		
		addCmd("deleteSuricata", params -> Json.object("deleteSuricata","DEPRECATED"));
		
		addCmd("setSuricataIp", params -> Json.object("setSuricataIp","DEPRECATED"));
		
		addCmd("getSuricataRules", params -> Json.object("getSuricataRules","DEPRECATED"));
		
		addCmd("sendSuricataRules", params -> Json.object("sendSuricataRules","DEPRECATED"));
		
		addCmd("getConfiguredSuricata", params -> Json.object("getConfiguredSuricata","DEPRECATED"));
		
		addCmd("getSuricataState", params -> Json.object("getSuricataState","DEPRECATED"));
		
		addCmd("startSuricata", params -> Json.object("startSuricata","DEPRECATED"));
		
		addCmd("stopSuricata", params -> Json.object("stopSuricata","DEPRECATED"));
		
		addCmd("reloadRules", params -> Json.object("reloadRules","DEPRECATED"));

		System.out.println("Suricata service DEPRECATED");
		return true;
	}
}
