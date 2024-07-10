package main.services;

<<<<<<< HEAD
import com.csl.intercom.jsoncmd.ApiCommands;
=======
>>>>>>> origin/feature/refactor_code
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.ICSLService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;
<<<<<<< HEAD

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
				
=======
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
>>>>>>> origin/feature/refactor_code
				System.out.println("it works");
				Json j= Json.object();
				j.set("rep",  "ok");
				
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

	public String addCmd(String name, IJsonCmd j, IJsonCmdHelp jh) {
		return apiCommands.registerCmd(name, j,jh);
	}

	@Override
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}
	
	
>>>>>>> origin/feature/refactor_code
}
