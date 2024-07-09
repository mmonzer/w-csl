package main.services;

import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.ICSLService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;
import lombok.Getter;

public class CSLService implements ICSLService {
	@Getter
	String name="#undef";
	@Getter
	IApiCommands apiCommands= new ApiCommandsFactory().createApiCommands(name);
	@Getter
	String configFileSectionName="config_"+name;;
	
	public CSLService() {
		this.name="my_service";
		this.configFileSectionName="config_"+name;
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
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}
	
	
}
