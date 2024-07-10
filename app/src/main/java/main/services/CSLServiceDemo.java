package main.services;

<<<<<<< HEAD
import com.csl.intercom.jsoncmd.ApiCommands;
=======
>>>>>>> origin/feature/refactor_code
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.ICSLService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.json.Json;
<<<<<<< HEAD

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
			
=======
import lombok.Getter;

public class CSLServiceDemo implements ICSLService {
	@Getter
    String name="#undef";
	@Getter
	IApiCommands apiCommands= new ApiCommandsFactory().createApiCommands("");
	@Getter
    String configFileSectionName="config_"+name;;

	public CSLServiceDemo() {
		this.name="demo";
		apiCommands.setName(name);
		this.configFileSectionName="demo_conf";
		
		
	}

    @Override
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean init(Json jConfig, String cslDir) {
		addCmd("demo_cmd", new IJsonCmd() {
>>>>>>> origin/feature/refactor_code
			@Override
			public Json exec(Json params) {
					
				Json j= Json.object();
				j.set("result", "ok");
				
				return j;
			}
		});
		return true;  // ok to start
	}
<<<<<<< HEAD
=======

	public String addCmd(String name, IJsonCmd j) {
		return apiCommands.registerCmd(name, j);
	}
	
>>>>>>> origin/feature/refactor_code
}
