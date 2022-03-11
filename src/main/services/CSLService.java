package main.services;

import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.xcsl.interfaces.IApiCommands;
import com.xcsl.interfaces.ICSLService;
import com.xcsl.interfaces.IJsonCmd;
import com.xcsl.interfaces.IJsonCmdHelp;
import com.xcsl.json.Json;

public class CSLService implements ICSLService {
	
	
	//HashMap<String, JsonCmd> cmds = new HashMap<String, JsonCmd>();

	//ApiCommands apiCommands= new ApiCommands("");
	IApiCommands apiCommands= new ApiCommandsFactory().createApiCommands("");
	
	String name="#undef";
	String configFileSectionName="config_"+name;;
	
	
	public CSLService() {
		this.name="my_service";
		this.configFileSectionName="config_"+name;
		apiCommands.setName(name);
		
		
	}
	
	public CSLService(String name) {
		this.name=name;
		this.configFileSectionName="config_"+name;
		apiCommands.setName(name);
		
		
	}
	public CSLService(String name, String configFileSectionName) {
		this.name=name;
		this.configFileSectionName=configFileSectionName;
	}
	
	@Override
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public String getName() {
		return name;
	}
	
	public String getConfigFileSectionName() {
		return "my_service";
		
	}
	
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
	

	
	
	
	public String addCmd(String name, IJsonCmd j) {
		return apiCommands.registerCmd(name, j);
	}
	
	
	public String addCmd(String name, IJsonCmd j, IJsonCmdHelp jh) {
		return apiCommands.registerCmd(name, j,jh);
	}

	@Override
	public IApiCommands getApiCommands() {
		// TODO Auto-generated method stub
		return apiCommands;
	}
	
	
}
