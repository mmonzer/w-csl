package main.services;

import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.ICSLService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.json.Json;
import lombok.Getter;

public class CSLServiceDemo implements ICSLService {
	IApiCommands apiCommands= new ApiCommandsFactory().createApiCommands("");
	
	
	@Getter
    String name="#undef";
	@Getter
    String configFileSectionName="config_"+name;;
	
	
	public CSLServiceDemo() {
		this.name="demo";
		this.configFileSectionName="demo_conf";
		
		
	}
	
	public CSLServiceDemo(String name, String configFileSectionName) {
		this.name=name;
		this.configFileSectionName=configFileSectionName;
	}

    @Override
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}

	// ajouter les commandes ici
	// jConfig est la partie du fichier de conf dans le nom de section est donné plus haut
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
	

	public String addCmd(String name, IJsonCmd j) {
		return apiCommands.registerCmd(name, j);
	}

	@Override
	public IApiCommands getApiCommands() {
		apiCommands.setName(name);
		return apiCommands;
	}
	
}
