package com.csl.intercom.jsoncmd;

import com.xcsl.json.Json;

public interface ICSLService {

	
	
	ApiCommands getApiCommands();
	
	String getConfigFileSectionName();
	boolean init(Json jConfig, String userdir);
	
	boolean terminate();
	
}

	
	
