package main.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.ICSLService;
import com.csl.intercom.jsoncmd.JsonCmd;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.xcsl.ids.IDSTrace;
import com.xcsl.json.Json;

public class MonitorService implements ICSLService {


	ApiCommands apiCommands= new ApiCommands("");
	

	String name="#undef";
	String configFileSectionName="config_"+name;


	//private IIDSRunner idsRunner=null;

	/*public void setIDSRunner(IDSRunner idsRunner) {
		// TODO Auto-generated method stub
		this.idsRunner=idsRunner;
	} */
	
	public MonitorService() {
		this.name="monitor";
		this.configFileSectionName="ids_conf";

		
	}

	public MonitorService(String name, String configFileSectionName) {
		this.name=name;
		this.configFileSectionName=configFileSectionName;
	}

	



	public String getName() {
		return name;
	}

	public String getConfigFileSectionName() {
		return configFileSectionName;

	}

	public boolean init(Json jConfig, String cslDir) {
		
		
		//idsRunner=CSLContext.instance.getIdsRunner();

		
		
		
		
		
		
		addCmd("stats_devices", new JsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("start exec ");
				System.out.println("Exec JCmd test_cmd :"+params);
				System.out.println("Fin exec");
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
		
		addCmd("stats_taps", new JsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("start exec ");
				System.out.println("Exec JCmd test_cmd :"+params);
				System.out.println("Fin exec");
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

	static private  String readAnyFile(String path) 
	{



		String content = "";

		try
		{
			content = new String ( Files.readAllBytes( Paths.get(path) ) );
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return "{\"Error\":\"File not found:"+e.getMessage()+"\"}";
		}

		return content;
	}

	static private Json loadAnyFileAsJson(String fullname) {


		String result="";
		Json j=Json.object();

		if (fullname!=null) {
			result=readAnyFile(fullname);
			Json z=Json.read(result);
			j.set("contents",z);
			IDSTrace.log(IDSTrace.WEB_DATABASE,
					"File Contents="+result);

		} else {
			j.set("contents",Json.object());
			j.set("error", "Nof file with name:"+fullname);
			IDSTrace.log(IDSTrace.WEB_DATABASE,
					"File Load error="+j.toString());

		}

		return j;
	}

	static public String startOf(String s) {
		int MAX=50;
		if (s.length()<=MAX) return s;
		else return s.substring(0,MAX-1)+"...";
	}


	public String addCmd(String name, JsonCmd j) {
		return apiCommands.registerCmd(name, j);
	}
	
	
	public String addCmd(String name, JsonCmd j, JsonCmdHelp jh) {
		return apiCommands.registerCmd(name, j,jh);
	}

	@Override
	public ApiCommands getApiCommands() {
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
