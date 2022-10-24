package com.ucsl.interfaces;

import com.ucsl.json.Json;

public interface ICSLService {
	
	public String getConfigFileSectionName() ;
	public boolean init(Json jConfig, String cslDir);
	public IApiCommands getApiCommands();
	public boolean terminate() ;
	
	
}
