package com.xcsl.interfaces;

import com.xcsl.json.Json;

public interface ICSLService {
	
	public String getConfigFileSectionName() ;
	public boolean init(Json jConfig, String cslDir);
	public IApiCommands getApiCommands();
	public boolean terminate() ;
	
	
}
