package main.services;

import com.csl.core.CSLContext;
import com.csl.devdb.DevicesUtil;
import com.csl.ids.IDSDataSetManager;
import com.csl.ids.IDSTapManager;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.csl.modules.ModuleIDS;
import com.csl.monitor.ActivityMonitor;
import com.csl.util.RulesUtil;
import com.csl.web.websockets.CSLWebSocket;
import com.ucsl.interfaces.IAlertDescriptor;
import com.ucsl.interfaces.IIDSOperationManager;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import com.wcsl.ids.IDSOperationManagerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CSLServiceIDS extends Service {


	//private IIDSRunner idsRunner=null;

	/*public void setIDSRunner(IDSRunner idsRunner) {
		// TODO Auto-generated method stub
		this.idsRunner=idsRunner;
	} */

	/**
	 * Default constructor of the IDS service.
	 */
	public CSLServiceIDS() {
		this("ids",
				"Service for managing previous IDS: start/stop, configure, change the running mode (learning, idle, recording, ...), ...",
				"ids_conf");
	}
	/**
	 * Generic constructor of the IDS service.
	 */
	public CSLServiceIDS(String name, String description, String configFileSectionName) {
		super(name, description,configFileSectionName);
	}

	/**
	 * Initialization of the IDS commands
	 * @param jConfig the configuration section of the configuration file
	 * @param cslDir the CSL directory
	 * @return true if the initialization happened with no problems, false otherwise.
	 */
	public boolean init(Json jConfig, String cslDir) {
		
		addCmd("test_console", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("test console");
				Json j=Json.object();
				j.set("result", "test console:ok");
				
				Json j2 = Json.object();
				j2.set("line", "Test console");
				j2.set("console_id","learn");
				CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE,j2 );
				
				return j;
			}
		});
		
		addCmd("stats_links", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
				ActivityMonitor monitor = ids.getActivityMonitor();
				return monitor.getHistoryJson();
			}
		},
				new JsonCmdHelp()
				.setDesc("return the number of active links (current value and list of values for a sampling time) ")
				.setResult("{nb_links: , histo:[0 , 0,0, ... ], period: (s) }", JsonCmdHelp.JSON)
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		addCmd("stats_network", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("start exec ");
				System.out.println("Exec JCmd test_cmd :"+params);
				System.out.println("Fin exec");
				Json j=Json.object();
				j.set("percent_flowrate", 20+(int)(Math.random()*50));
				
				return j;
			}
		},
				new JsonCmdHelp()
				.setDesc("return the network load ")
				.setResult("{percent_flowrate:  }", JsonCmdHelp.JSON)
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		addCmd("test_alert", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("start exec ");
				System.out.println("Exec JCmd test_cmd :"+params);
				System.out.println("Fin exec");
				Json j=Json.object();
				j.set("result", "ok");
				j.set("value",1);

				String ext=" #"+System.currentTimeMillis();
			
				
				IAlertDescriptor a3= CSLContext.instance.getIDSMainProcessor().getAlertFactory().createAlertDescriptor(3, "ALERT 3 ["+ext+"]", System.currentTimeMillis());
				
				CSLContext.instance.getCSLAlertManager().sendAlert(a3);
				return CSLContext.instance.getConfig();
			}
		});

		addCmd("set_param_boolean", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				// TODO Auto-generated method stub

				boolean b=JsonUtil.getBooleanFromJson(params, "value", true);
				String name= JsonUtil.getStringFromJson(params,"name","");

				Json j= Json.object();

				if (!name.isEmpty()) {
					CSLContext.instance.getIdsRunner().getIdsParams().setParamInCurrentVariables(name, b);

					j.set("value", b);
				}
				else {
					System.err.println("Invalid param in set_param_boolean:"+name);
					j.set("error","Invalid param in set_param_boolean:"+name );
				}
				return j;
			}
		},
				new JsonCmdHelp()
				.setDesc("set a flag in the IDS state (stored in the file CurrentIDSParams.json between runs ")
				.setParam("value", "true/false", JsonCmdHelp.BOOL)
				.setResult("return the result of the operation or error", JsonCmdHelp.STR)
				);

		addCmd("get_param_boolean", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				String name= JsonUtil.getStringFromJson(params,"name","");

				Json j= Json.object();

				
				boolean b=false;
				if (!name.isEmpty()) {
					b=CSLContext.instance.getIdsRunner().getIdsParams().getParamFromCurrentVariablesAsBoolean(name);

				}
				else {
					System.err.println("Invalid param in set_param_boolean:"+name);
					j.set("error","Invalid param in set_param_boolean:"+name );
				}
				j.set("value", b);
				return j;
			}
		});

		addCmd("set_param_string", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				// TODO Auto-generated method stub

				String b=JsonUtil.getStringFromJson(params, "value", "");
				String name= JsonUtil.getStringFromJson(params,"name","");

				Json j= Json.object();

				if (!name.isEmpty()) {
					CSLContext.instance.getIdsRunner().getIdsParams().setParamInCurrentVariables(name, b);

					j.set("value", b);
				}
				else {
					System.err.println("Invalid param in set_param_string:"+name);
					j.set("error","Invalid param in set_param_string:"+name );
				}
				return j;
			}
		});

		addCmd("get_param_string", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				String name= JsonUtil.getStringFromJson(params,"name","");

				Json j= Json.object();

				String s="";
				if (!name.isEmpty()) {
					s=CSLContext.instance.getIdsRunner().getIdsParams().getParamFromCurrentVariablesAsString(name);

				}
				else {
					System.err.println("Invalid param in set_param_string:"+name);
					j.set("error","Invalid param in set_param_string:"+name );
				}
				j.set("value", s);
				return j;
			}
		});

		addCmd("set_send_to_browser", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				// TODO Auto-generated method stub

				boolean b=JsonUtil.getBooleanFromJson(params, "value", true);
				System.out.println("Set send to browser:"+b);
				CSLContext.instance.getIdsRunner().setIdsSendToBrowser(b);
				Json j= Json.object();
				j.set("value", b);
				return j;
			}
		});

		addCmd("get_send_to_browser", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				// TODO Auto-generated method stub
				Json j= Json.object();
				j.set("value", CSLContext.instance.getIdsRunner().isIdsSendToBrowser());
				return j;
			}
		});
		addCmd("set_send_to_console", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				// TODO Auto-generated method stub

				boolean b=JsonUtil.getBooleanFromJson(params, "value", true);

				CSLContext.instance.getIdsRunner().setIdsSendToConsole(b);
				Json j= Json.object();
				j.set("value", b);
				return j;
			}
		});

		addCmd("get_send_to_console", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				// TODO Auto-generated method stub
				Json j= Json.object();
				j.set("value", CSLContext.instance.getIdsRunner().isIdsSendToConsole());
				return j;
			}
		});

		addCmd("select_dataset", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				// TODO Auto-generated method stub

				Json j=Json.object();
				int cat =0;

				String name = JsonUtil.getStringFromJson(params, "name", "dataset");
				int n=JsonUtil.getIntFromJson(params, "cat", 0);
				if ((cat>=1)||(cat<=3)) {
					CSLContext.instance.getIdsRunner().getIdsParams().getDatasetManager().selectDataSet(cat, name);

				}

				return j;
			}
		});

		addCmd("get_learned_rules", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				// TODO Auto-generated method stub
				Json j=CSLContext.instance.getIDSMainProcessor().getLearnedRules();

				System.out.println(j);

				return j;
			}
		});
		IIDSOperationManager opManager= IDSOperationManagerFactory.instance.createIDSOperationManagerFactory(CSLContext.instance.getIDSMainProcessor());
		
		addCmd("op_model_ids", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				Json j=opManager.exec(params);

				System.out.println(j);

				return j;
			}
		},	new JsonCmdHelp()
				.setDesc("Model operations")
					
				.setStatus(JsonCmdHelp.STATUS_OK)
				.setHelpProvider(opManager)
		);

		addCmd("idsmode", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				// TODO Auto-generated method stub

				System.out.println("set mode "+params);
				int n=JsonUtil.getIntFromJson(params, "mode", 0);
				CSLContext.instance.getIdsRunner().setIDSMode(n);
				Json j= Json.object();
				j.set("mode set to ",  CSLContext.instance.getIdsRunner().getIDSMode());

				return j;
			}
		});

		addCmd("idsMode", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("start exec");
				System.out.println("Exec JCmd test_cmd2 :"+params);
				System.out.println("Fin exec");
				Json j=Json.object();
				j.set("result", "ok");
				j.set("value",1);

				if (params.get("mode")==null) {
					System.out.println("Invalid mode");
				}
				else {
					int mode=params.get("mode").asInteger();
					CSLContext.instance.getIdsRunner().setIDSMode(mode);
				}
				return j;
			}
		});

		addCmd("setIdsMode", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
				System.out.println(ids.runningState());



				if (params.get("mode")==null) {
					System.out.println("Invalid mode");
				}
				else {
					int mode=params.get("mode").asInteger();
					CSLContext.instance.getIdsRunner().setIDSMode(mode);
				}

				Json j=Json.object();
				j.set("idsmode",CSLContext.instance.getIdsRunner().getIDSMode());
				j.set("idsmodestr",CSLContext.instance.getIdsRunner().getIDSModeAsString());

				return j;
			}
		});

		addCmd("getIdsMode", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
				System.out.println(ids.runningState());

				Json j=Json.object();
				j.set("idsmode",CSLContext.instance.getIdsRunner().getIDSMode());
				j.set("idsmodestr",CSLContext.instance.getIdsRunner().getIDSModeAsString());

				return j;
			}
		});

		addCmd("get_ids_config", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("start exec ");
				System.out.println("Exec JCmd test_cmd :"+params);
				System.out.println("Fin exec");
				Json j=Json.object();
				j.set("result", "ok");
				j.set("value",1);
				return CSLContext.instance.getConfig();
			}
		});

		addCmd("get_sys_config", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("start exec");
				System.out.println("Exec JCmd test_cmd :"+params);
				System.out.println("Fin exec");
				Json j=Json.object();
				j.set("result", "ok");
				j.set("value",1);
				
				Json jresult =CSLContext.instance.getIDSMainProcessor().getIDSVariables();
				return jresult;
			}
		});

		addCmd("get_learned_rules", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("start exec get_learned_rules");

				Json jresult =CSLContext.instance.getIDSMainProcessor().getLearnedRules();

				return jresult;
			}
		});
		addCmd("getDataSetList", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				int  category= JsonUtil.getIntFromJson(params, "category", 1);
				category= Math.max(1,  Math.min(category, 3));

				System.out.println("get List of dataset :"+category);


				Json j=CSLContext.instance.getIdsRunner().getIdsParams().getDatasetManager().getListOfDataSetAsJson(category);

				return j;
			}
		});

		// operations : create, delete, rename, select, copy
		addCmd("dataSetOperation", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				System.out.println("EXEC dataSetOperation:"+params);
				int  category= JsonUtil.getIntFromJson(params, "category", 1);
				category= Math.max(1,  Math.min(category, 3));
				String name =JsonUtil.getStringFromJson(params,"name","" );
				String targetName =JsonUtil.getStringFromJson(params,"target_name","" );

				String operation= JsonUtil.getStringFromJson(params,"operation","" ).toLowerCase();
				int targetCategory=JsonUtil.getIntFromJson(params, "target_category", 0);
				boolean overwrite=JsonUtil.getBooleanFromJson(params, "overwrite", false);

				System.out.println("start dataset operation "+operation+" name="+name+" category="+category+" target_category="+targetCategory+" overwrite="+overwrite);


				Json result=
						CSLContext.instance.getIdsRunner().getIdsParams().getDatasetManager().
						exec(operation, category, name,targetCategory, targetName, overwrite);

				System.out.println("RESULT:"+result);

				return result;
			}
		});

		// operations : create, delete, rename, select, copy
		/*
		 * add : id
		 * del : id
		 * set : id, json décrivant la config du tap
		 * lst : json avec la config de tous les taps
		 * 
		 * load
		 * save
		 * 
		 * 	type : 
		 * 		static public int SURICATA_RULES_BASE=3;
		 *		static public int SURICATA_RULES_LEARNED=4;
		 *		static public int SURICATA_CONFIG=5;
		 *		static public int TAP_CONFIG=6;
		 *
		 * 
		 *  “id”:””,                  // ID du tap 
		 *	“ip”:””,                  // IP de la machine distante 
		 *	“username”:””     // Username SSH
		 *	“password”:””     // Password SSH
		 *
		 */
		addCmd("opTaps", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				System.out.println("EXEC tapOperation:"+params);

				String operation= JsonUtil.getStringFromJson(params,"operation","" ).toLowerCase();
				String idname =JsonUtil.getStringFromJson(params,"idname","" );
				String new_idname =JsonUtil.getStringFromJson(params,"new_idname","" );

				String text =JsonUtil.getStringFromJson(params,"text","" );
				String text2=text;
				if (text2.length()>40) {
					text2=text2.substring(0, 40)+"...";
				}
				String ip =JsonUtil.getStringFromJson(params,"ip","" );
				String username =JsonUtil.getStringFromJson(params,"username","" );
				String password =JsonUtil.getStringFromJson(params,"password","" );

				int type =JsonUtil.getIntFromJson(params,"type",0 );

				System.out.println("start tap operation "+operation+" name="+idname+
						" ip:"+ip+" username:"+username+" password:"+password
						+" text:"+text2);

				Json desc= Json.object();
				desc.set("idname", idname);
				desc.set("new_idname", new_idname);
				desc.set("ip", ip);
				desc.set("username",username);
				desc.set("password", password);
				desc.set("type", type);

				IDSTapManager t=	new IDSTapManager(CSLContext.instance.getIDSMainProcessor().getIdsMainProcessorParams());
				Json result= t.exec(operation,idname, desc,text);

				System.out.println("RESULT:"+result);

				return result;
			}
		});

		// operations : load, save, compile
		addCmd("opRulesSet", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				return Json.object();
			}
		});

		// operations : load, save, compile
		addCmd("opSystemConfiguration", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				return Json.object();
			}
		});

		// operations : create, delete, rename, select, copy
		addCmd("reset_learned_model", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				System.out.println("EXEC start Learning:"+params);

				CSLContext.instance.getIdsRunner().switchModeToIdle();
				CSLContext.instance.getIDSMainProcessor().resetLearnedModel();
				Json result=Json.object();

				return result;
			}
		});
		// operations : create, delete, rename, select, copy
		addCmd("get_learned_model_table", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				System.out.println("EXEC add_to_dbdevices:"+params);

				Json result =DevicesUtil.getLearnedModelTableAsJson(CSLContext.instance.getIDSMainProcessor());

				return result;
			}
		});
		
		addCmd("get_learned_model_dpi_table", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				Json result =DevicesUtil.getLearnedModelTableAsJsonDpi(CSLContext.instance.getIDSMainProcessor());
				return result;
			}
		});
		// operations : create, delete, rename, select, copy
		addCmd("start_learning", new IJsonCmd() {
			@Override
			public Json exec(Json params) {

				System.out.println("EXEC start Learning:"+params);
				
				CSLContext.instance.getIDSMainProcessor().backupLearnedModel();

				CSLContext.instance.getIdsRunner().switchModeToLearn();

				Json result=Json.object();

				return result;
			}
		});

		// operations : create, delete, rename, select, copy
		addCmd("cancel_learning", new IJsonCmd() {
			@Override
			public Json exec(Json params) {

				System.out.println("EXEC cancel Learning:"+params);

				CSLContext.instance.getIdsRunner().switchModeToIdle();
				
				CSLContext.instance.getIDSMainProcessor().reverseBackupLearnedModel();

				Json result=Json.object();

				return result;
			}
		});
			
		addCmd("get_CSL_learning_args", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				// TODO Auto-generated method stub

				Json j= Json.array();
				j.add(Json.object().set("name", "IDS_MODE")
						.set("value", CSLContext.instance.getIdsRunner().getIDSModeAsString()));

				int cat=IDSDataSetManager.LEARNING;

				j.add(Json.object().set("name", "Dataset")
						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getDatasetManager().getCurrentdataSetOfCategory(cat)));

				j.add(Json.object().set("name", "Dataset path")
						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getFullPackets_dir_for_learning()));
				
				CSLContext.instance.getIDSMainProcessor().getParamsAsJsonNameValueArray(j);

				j.add(Json.object().set("name", "Data directory")
						.set("value", 				
								CSLContext.instance.getIdsRunner().getIdsParams().getDatasetManager().getDirOfCategory(IDSDataSetManager.LEARNING)));
				System.out.println(j);

				return j;
			}
		});

		// operations : create, delete, rename, select, copy
		addCmd("start_detection_offline", new IJsonCmd() {
			@Override
			public Json exec(Json params) {

				System.out.println("EXEC start detection offline:"+params);
				System.out.println(CSLContext.instance.getIdsRunner().getIdsParams().getIDSModeAsString());
				
				CSLContext.instance.getIdsRunner().switchModeToDetectOffline();

				Json result=Json.object();

				return result;
			}
		});

		// operations : create, delete, rename, select, copy
		addCmd("cancel_detection_offline", new IJsonCmd() {
			@Override
			public Json exec(Json params) {

				System.out.println("EXEC stop detection offline:"+params);
				
				CSLContext.instance.getIdsRunner().switchModeToIdle();
				Json result=Json.object();
				return result;
			}
		});

		addCmd("get_CSL_detection_offline_args", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				// TODO Auto-generated method stub
				Json j= Json.array();
				j.add(Json.object().set("name", "IDS_MODE")
						.set("value", CSLContext.instance.getIdsRunner().getIDSModeAsString()));

				int cat=IDSDataSetManager.DETECTION_OFFLINE;

				j.add(Json.object().set("name", "Dataset")
						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getDatasetManager().getCurrentdataSetOfCategory(cat)));

				j.add(Json.object().set("name", "Dataset path")
						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getFullPackets_dir_for_detection_offline()));
				
				CSLContext.instance.getIDSMainProcessor().getParamsAsJsonNameValueArray(j);

				j.add(Json.object().set("name", "Data directory")
						.set("value", 				
								CSLContext.instance.getIdsRunner().getIdsParams().getDatasetManager().getDirOfCategory(IDSDataSetManager.DETECTION_OFFLINE)));
				System.out.println(j);

				return j;
			}
		});

		// operations : create, delete, rename, select, copy
		addCmd("start_detection", new IJsonCmd() {
			@Override
			public Json exec(Json params) {

				System.out.println("EXEC start detection :"+params);
				CSLContext.instance.getIdsRunner().switchModeToDetectOnline();

				Json result=Json.object();

				return result;
			}
		});

		// operations : create, delete, rename, select, copy
		addCmd("start_recording_only", new IJsonCmd() {
			@Override
			public Json exec(Json params) {

				System.out.println("EXEC start recording only:"+params);

				CSLContext.instance.getIdsRunner().switchModeToRecording();
				
				System.out.println("IDSMODE:"+CSLContext.instance.getIdsRunner().getIdsParams().getIDSModeAsString());
				System.out.println("Console log:"+CSLContext.instance.getIdsRunner().getIdsParams().isSendToConsole());
				System.out.println("Browser log:"+CSLContext.instance.getIdsRunner().getIdsParams().isSendToBrowser());


				Json result=Json.object();

				return result;
			}
		});

		// operations : create, delete, rename, select, copy
		addCmd("set_idle_mode", new IJsonCmd() {
			@Override
			public Json exec(Json params) {


				ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
				System.err.println(ids.runningState());


				System.out.println("EXEC set idle mode:"+params);
			
				CSLContext.instance.getIdsRunner().switchModeToIdle();

				System.out.println("mode="+CSLContext.instance.getIdsRunner().getIdsParams().getIDSModeAsString());
				Json result=Json.object();
				return result;
			}
		});

		addCmd("get_CSL_running_args", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				// TODO Auto-generated method stub
				Json j= Json.array();
				j.add(Json.object().set("name", "IDS_MODE")
						.set("value", CSLContext.instance.getIdsRunner().getIDSModeAsString()));

				int cat=IDSDataSetManager.RECORDING;


				j.add(Json.object().set("name", "ids_output_to_nrowser")
						.set("value", CSLContext.instance.getIdsRunner().isIdsSendToBrowser()));

				j.add(Json.object().set("name", "ids_output_to_console")
						.set("value", CSLContext.instance.getIdsRunner().isIdsSendToConsole()));


				j.add(Json.object().set("name", "Dataset")
						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getDatasetManager().getCurrentdataSetOfCategory(cat)));

				j.add(Json.object().set("name", "Dataset path")
						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getFullPackets_dir_for_recording()));
				
				CSLContext.instance.getIDSMainProcessor().getParamsAsJsonNameValueArray(j);

				j.add(Json.object().set("name", "Data directory")
						.set("value", 				
								CSLContext.instance.getIdsRunner().getIdsParams().getDatasetManager().getDirOfCategory(IDSDataSetManager.RECORDING)));
				System.out.println(j);

				return j;
			}
		});
		// Gestion des alertes
		addCmd("get_alerts_list", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				Json j=CSLContext.instance.getCSLAlertManager().getListOfCurrentAlertsAsJson();
				return j;
			}
		});

		addCmd("op_alert", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				Json j=CSLContext.instance.getCSLAlertManager().execOpAlert(params);
				return j;
			}
		});
		addCmd("generate_suricata_rules", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				Json devices=params.get("devices");
				Json options=params.get("options");

				Json rules = new RulesUtil().toSuricataRules(devices, options);
				
				return rules;
			}
		});

		return true;  // ok to start
	}
	static private  String readAnyFile(String path) {
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
			System.err.println(
					"File Contents="+result);

		} else {
			j.set("contents",Json.object());
			j.set("error", "Nof file with name:"+fullname);
			System.err.println(
					"File Load error="+j.toString());

		}

		return j;
	}

	static public String startOf(String s) {
		int MAX=50;
		if (s.length()<=MAX) return s;
		else return s.substring(0,MAX-1)+"...";
	}
}
