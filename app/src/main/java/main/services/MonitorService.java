package main.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
	
	IApiCommands apiCommands= new ApiCommandsFactory().createApiCommands("");
	@Getter
    String name="#undef";
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
				
				
				ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
				ActivityMonitor activityMonitor = ids.getActivityMonitor();
				Json j=Json.object();
				j.set("all", 103);
				j.set("running",87);


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
		apiCommands.setName(name);
		return apiCommands;
	}

	@Override
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}

}
