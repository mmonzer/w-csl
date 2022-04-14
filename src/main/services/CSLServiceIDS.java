package main.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.csl.core.CSLContext;
import com.csl.devdb.DevicesUtil;
import com.csl.ids.IDSDataSetManager;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.xcsl.interfaces.IApiCommands;
import com.xcsl.interfaces.ICSLService;
import com.xcsl.interfaces.IJsonCmd;
import com.xcsl.interfaces.IJsonCmdHelp;
import com.csl.modules.ModuleIDS;
import com.csl.util.FileUtils;
import com.csl.web.websockets.CSLWebSocket;
import com.xcsl.ids.IDSTrace;
import com.xcsl.ids.RulesEditor;
import com.xcsl.interfaces.IAlertDescriptor;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;
import com.xcsl.learning.IDSLearnedRules;

public class CSLServiceIDS implements ICSLService {


	IApiCommands apiCommands= new ApiCommandsFactory().createApiCommands("");
//	ApiCommands apiCommands= new ApiCommands("");
	

	String name="#undef";
	String configFileSectionName="config_"+name;


	//private IIDSRunner idsRunner=null;

	/*public void setIDSRunner(IDSRunner idsRunner) {
		// TODO Auto-generated method stub
		this.idsRunner=idsRunner;
	} */
	
	public CSLServiceIDS() {
		this.name="ids";
		this.configFileSectionName="ids_conf";

		
	}

	public CSLServiceIDS(String name, String configFileSectionName) {
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
		
		addCmd("test_console", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("test console");
				Json j=Json.object();
				j.set("result", "test console:ok");
				
				Json j2 = Json.object();
				j2.set("line", "Test console");
				j2.set("console_id","learn");
				//			CSLWebSocketForConsole.broadcastMessageJson("log", j);
				CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE,j2 );
				
				return j;
			}
		});
	
		
		addCmd("stats_links", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("start exec ");
				System.out.println("Exec JCmd test_cmd :"+params);
				System.out.println("Fin exec");
				Json j=Json.object();
				j.set("nb_links", 50+(int)(Math.random()*50));
				
				Json jHisto= Json.array();
				
				long time=CSLContext.instance.getTimeSystemCurrent();
				
				int period =60;
				Json times= Json.array();
				
				for (int i=0;i<10;i++) {
					jHisto.add(50+(int)(Math.random()*50));
					times.add(time-period*1000*(10-i));
				}
				
				
				j.set("histo",jHisto);
				j.set("times",times );
				j.set("period",period);

				return j;
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

			
				
				IAlertDescriptor a3= CSLContext.instance.getIDSMainProcessor().getAlertFactory().createAlertDescriptor(3, "ALERT 3", System.currentTimeMillis());
				
				CSLContext.instance.getCSLAlertManager().sendAlert(a3);
			
				//CSLAlertManager.instance.sendAlert("HIGH","test alert","xxx=testval");
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
				.setDesc("set a flag in the IDS state (stored in the file CurrentIDSParams.json bewteen runs ")
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

		//		addCmd("switch_send_to_browser", new JsonCmd() {
		//			
		//			@Override
		//			public Json exec(Json params) {
		//				// TODO Auto-generated method stub
		//				
		//				
		//				boolean b= CSLContext.instance.getIdsRunner().isIdsSendToBrowser();
		//				CSLContext.instance.getIdsRunner().setIdsSendToBrowser(!b);
		//				Json j= Json.object();
		//				j.set("state", !b);
		//				return j;
		//			}
		//		});

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


		//		addCmd("switch_send_to_console", new JsonCmd() {
		//			
		//			@Override
		//			public Json exec(Json params) {
		//				// TODO Auto-generated method stub
		//				
		//				
		//				boolean b= CSLContext.instance.getIdsRunner().isIdsSendToConsole();
		//				CSLContext.instance.getIdsRunner().setIdsSendToConsole(!b);
		//				Json j= Json.object();
		//				j.set("state", !b);
		//				
		//				return j;
		//			}
		//		});

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

		//		addCmd("set_show_alerts_on_hmi", new JsonCmd() {
		//			
		//			@Override
		//			public Json exec(Json params) {
		//				// TODO Auto-generated method stub
		//				
		//				boolean b=JsonUtil.getBooleanFromJson(params, "value", true);
		//				
		//				CSLContext.instance.getIdsRunner().getIdsParams().setShowAlertsOnHMI(b);
		//				Json j= Json.object();
		//				j.set("value", b);
		//				return j;
		//			}
		//		});
		//		
		//		addCmd("get_show_alerts_on_hmi", new JsonCmd() {
		//			
		//			@Override
		//			public Json exec(Json params) {
		//				// TODO Auto-generated method stub
		//				Json j= Json.object();
		//				j.set("value", CSLContext.instance.getIdsRunner().getIdsParams().isShowAlertsOnHMI());
		//				return j;
		//			}
		//		});
		//		
		//		addCmd("set_show_console_on_hmi", new JsonCmd() {
		//			
		//			@Override
		//			public Json exec(Json params) {
		//				// TODO Auto-generated method stub
		//				
		//				boolean b=JsonUtil.getBooleanFromJson(params, "value", true);
		//				
		//				CSLContext.instance.getIdsRunner().getIdsParams().setShowConsoleOnHMI(b);
		//				Json j= Json.object();
		//				j.set("value", b);
		//				return j;
		//			}
		//		});
		//		
		//		addCmd("get_show_console_on_hmi", new JsonCmd() {
		//			
		//			@Override
		//			public Json exec(Json params) {
		//				// TODO Auto-generated method stub
		//				Json j= Json.object();
		//				j.set("value", CSLContext.instance.getIdsRunner().getIdsParams().isShowConsoleOnHMI());
		//				return j;
		//			}
		//		});

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
				else {
					j.set("error", "invalid category:");
				}

				return j;
			}
		});

		addCmd("get_learned_rules", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				// TODO Auto-generated method stub




				Json j=CSLContext.instance.getIDSMainProcessor().getLearnedRules();
						//getIdsRunner().getLearnedRulesAsJson();

				System.out.println(j);

				return j;
			}
		});



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

				//cslAlertManager.sendAlert("HIGH","test alert","xxx=testval",true,false);
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

				//String fileName=CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir()+File.separator+CSLContext.instance.getIdsRunner().getIdsParams().getVariablesFileName();

				
				//System.out.println("FILE NAME:"+fileName);

				
				Json jresult =CSLContext.instance.getIDSMainProcessor().getIDSVariables();
						//loadAnyFileAsJson(fileName);


				//cslAlertManager.sendAlert("HIGH","test alert","xxx=testval",true,false);
				return jresult;
			}
		});


		addCmd("get_learned_rules", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("start exec get_learned_rules");
				

				Json jresult =CSLContext.instance.getIDSMainProcessor().getLearnedRules();
				
//				String fileName=CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir()+File.separator+CSLContext.instance.getIdsRunner().getIdsParams().getLearnedRulesFileName();
//
//				System.out.println("Reading file of learned rules:"+fileName);
//
//				Json jresult =loadAnyFileAsJson(fileName);
				return jresult;
			}
		});

		//		addCmd("getCSLRunningArgs", new JsonCmd() {
		//
		//			@Override
		//			public Json exec(Json params) {
		//				System.out.println("start exec");
		//				System.out.println("Exec JCmd test_cmd :"+params);
		//				System.out.println("Fin exec");
		//				Json j=Json.object();
		//				j.set("params", CSLContext.instance.getIdsRunner().getIdsParams().getAsJson());
		//				j.set("idsmode",CSLContext.instance.getIdsRunner().getIDSMode());
		//				j.set("idsmodestr",CSLContext.instance.getIdsRunner().getIDSModeAsString());
		//
		//				j.set("ids_output_to_console",  CSLContext.instance.getIdsRunner().isIdsSendToConsole());
		//				j.set("ids_output_to_browser",  CSLContext.instance.getIdsRunner().isIdsSendToBrowser());
		//				
		//
		//
		//				return j;
		//			}
		//		});


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


				Json result=
						CSLContext.instance.getIDSMainProcessor().getTapManager().exec(operation,idname, desc,text);




				System.out.println("RESULT:"+result);


				return result;
			}
		});


		// operations : load, save, compile
		addCmd("opRulesSet", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				System.out.println("EXEC opRulesSet:"+params);
				int  type= JsonUtil.getIntFromJson(params, "type", 2);
				type= Math.max(1,  Math.min(type, 4));
				String text =JsonUtil.getStringFromJson(params,"text","" );
				String operation= JsonUtil.getStringFromJson(params,"operation","" ).toLowerCase();

				System.out.println("start rules set operation "+operation+" type="+type+" text="+
						startOf(text));

				RulesEditor r= CSLContext.instance.getIDSMainProcessor().createRulesEditor();


				Json result=r.execOpRulesSet(operation, type, text);

				System.out.println("RESULT:"+result);


				return result;
			}
		});


		// operations : load, save, compile
		addCmd("opSystemConfiguration", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				System.out.println("EXEC opSystemConfiguration:"+params);
				String text =JsonUtil.getStringFromJson(params,"text","" );
				String operation= JsonUtil.getStringFromJson(params,"operation","" ).toLowerCase();
				boolean returnResult = JsonUtil.getBooleanFromJson(params, "return_result", false);

				System.out.println("start rules set operation "+operation+" returnResult="+returnResult+" text="+
						startOf(text));

				RulesEditor r= CSLContext.instance.getIDSMainProcessor().createRulesEditor();


				Json result=r.execOpSystemConfiguration(operation, text, returnResult);

				System.out.println("RESULT:"+result);


				return result;
			}
		});


		// operations : create, delete, rename, select, copy
		addCmd("reset_learned_model", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				System.out.println("EXEC start Learning:"+params);

				//CSLContext.instance.getIdsRunner().getIdsParams().setIdleMode();
				CSLContext.instance.getIdsRunner().switchModeToIdle();
				
			
				//CSLContext.instance.getIdsRunner().getIdsParams().setLearningMode();
			
				
				CSLContext.instance.getIDSMainProcessor().resetLearnedModel();
				Json result=Json.object();

				return result;
			}
		});


		// operations : create, delete, rename, select, copy
		addCmd("add_to_dbdevices", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				System.out.println("EXEC add_to_dbdevices:"+params);


//				String filePath=
//						CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir()+File.separator+
//						CSLContext.instance.getIdsRunner().getIdsParams().getLearnedRulesFileName();
//				FileUtils.backupFileWithTimeStamp(filePath, CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDirBackup());
				
				CSLContext.instance.getIDSMainProcessor().backupLearnedModel();

//				String dirfilePathOps=
//						CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir()+File.separator+
//						"internal";

				//FileUtils.backupFileWithTimeStamp(dirfilePathOps+File.separator+"ops", CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDirBackup());
				//IDSTrace.log(IDSTrace.GENERAL, "test");
				
				CSLContext.instance.getIDSMainProcessor().backupFileInModelDir("internal", "ops");
				
				//Json ops=DevicesUtil.updateLearnedModelFromDevicesDB(CSLContext.instance.getIdsRunner().getIdsParams());

				Json ops=DevicesUtil.updateDevicesDBFromLearnedModel(CSLContext.instance.getIDSMainProcessor());

				//FileUtils.saveJsonToFile(dirfilePathOps, "ops", ops);
				
				CSLContext.instance.getIDSMainProcessor().saveJsonInModelDir("internal", "ops", ops);
				
				Json result=Json.object();

				return result;
			}
		});
		
		
		/*
		 * geneate the model used for detection
		 * 
		 * 	1) the learned model (added to the device db)
		 * 	2- the device db (with user modif)
		 * 	3) the alerts model (list of alert with ok or risk level)
		 */
		addCmd("generate_ids_model", new IJsonCmd() {

					@Override
					public Json exec(Json params) {

						System.out.println("EXEC add_to_dbdevices:"+params);


//						String filePath=
//								CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir()+File.separator+
//								CSLContext.instance.getIdsRunner().getIdsParams().getLearnedRulesFileName();
//						FileUtils.backupFileWithTimeStamp(filePath, CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDirBackup());

						CSLContext.instance.getIDSMainProcessor().backupLearnedModel();
						
//						String dirfilePathOps=
//								CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir()+File.separator+
//								"internal";
						
						
						IDSTrace.log(IDSTrace.GENERAL, "test");
						
						

						Json ops=DevicesUtil.updateLearnedModelFromDevicesDB(CSLContext.instance.getIDSMainProcessor());
						
						//FileUtils.backupFileWithTimeStamp(dirfilePathOps+File.separator+"ops", CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDirBackup());

						//Json ops=DevicesUtil.updateDevicesDB(CSLContext.instance.getIdsRunner().getIdsParams());

						//FileUtils.saveJsonToFile(dirfilePathOps, "ops", ops);
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

		addCmd("get_devices_table", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				System.out.println("EXEC get devices table:"+params);

				try {
					Json result =DevicesUtil.exportToJsonTable();

					return result;
				} catch (Exception e) {
					System.out.println(e);
					e.printStackTrace();
				}

				return Json.array();
			}
		});

		
		addCmd("get_devices_json", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				System.out.println("EXEC get devices json:"+params);

				try {
					Json result =DevicesUtil.exportToJson();

					return result;
				} catch (Exception e) {
					System.out.println(e);
					e.printStackTrace();
				}

				return Json.array();
			}
		});


		// operations : create, delete, rename, select, copy
		addCmd("start_learning", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				System.out.println("EXEC start Learning:"+params);

				//CSLContext.instance.getIdsRunner().getIdsParams().setIdleMode();

//				String filePath=
//						CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir()+File.separator+
//						CSLContext.instance.getIdsRunner().getIdsParams().getLearnedRulesFileName();
//				FileUtils.backupFileWithTimeStamp(filePath, CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDirBackup());
				
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

				//CSLContext.instance.getIdsRunner().getIdsParams().setIdleMode();
				CSLContext.instance.getIdsRunner().switchModeToIdle();
				
//				String filePath=
//						CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir()+File.separator+
//						CSLContext.instance.getIdsRunner().getIdsParams().getLearnedRulesFileName();
//
//				FileUtils.reverseToLastBackupFile(filePath);
				
				CSLContext.instance.getIDSMainProcessor().reverseBackupLearnedModel();



				Json result=Json.object();

				return result;
			}
		});
		
		
		addCmd("generate_rules", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				System.out.println("Generate Rules:"+params);

				// IDSLearnedRules z = DevicesUtil.getLearnedModelFromFile(CSLContext.instance.getIdsRunner().getIdsParams());
				 
				 IDSLearnedRules z = CSLContext.instance.getIDSMainProcessor().getLearnedModelFromFile();
				 
					
				 System.out.println("Taps subdir:"+CSLContext.instance.getIDSMainProcessor().getTapManager().getTapsDir());
			     System.out.println("Taps IDs   :"+CSLContext.instance.getIDSMainProcessor().getTapManager().getTapsIDs());
			      
			     String lines=z.toSuricataRules();
			     System.out.println("Generated Suricata Rules");
			     System.out.println(lines);
			     
			     // cslconf/taps/<nomTap>/genrules.rules
			     for (String tapId:CSLContext.instance.getIDSMainProcessor().getTapManager().getTapsIDs()) {
			    	 String filename =CSLContext.instance.getCslConfDir()+File.separator+
			    			 CSLContext.instance.getIDSMainProcessor().getTapManager().getTapsDir()+File.separator+tapId+File.separator+"genrules.rules";
			    	 System.out.println("  --> write rules to "+filename);
			    	 FileUtils.writeFile(filename, lines);
			    	 
			     }
				
			       
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
				
//				j.add(Json.object().set("name", "Models directory")
//						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir()));
//				j.add(Json.object().set("name", "Rules for detection")
//						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getRulesForDetectionFileName()));
//				j.add(Json.object().set("name", "Rules for learning")
//						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getRulesForLearningFileName()));
//				j.add(Json.object().set("name", "Variables")
//						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getVariablesFileName()));
//				j.add(Json.object().set("name", "Learned rules")
//						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getLearnedRulesFileName()));
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
//				CSLContext.instance.getIdsRunner().getIdsParams().setIdleMode();
			
//				String filePath=
//						CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir()+File.separator+
//						CSLContext.instance.getIdsRunner().getIdsParams().getLearnedRulesFileName();
//				FileUtils.backupFileWithTimeStamp(filePath, CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDirBackup());

				//CSLContext.instance.getIDSMainProcessor().backupLearnedModel();
				
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
			//	CSLContext.instance.getIdsRunner().getIdsParams().setIdleMode();
				
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
				
//				j.add(Json.object().set("name", "Models directory")
//						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir()));
//				j.add(Json.object().set("name", "Rules for detection")
//						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getRulesForDetectionFileName()));
//				j.add(Json.object().set("name", "Rules for learning")
//						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getRulesForLearningFileName()));
//				j.add(Json.object().set("name", "Variables")
//						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getVariablesFileName()));
//				j.add(Json.object().set("name", "Learned rules")
//						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getLearnedRulesFileName()));
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


				
//				CSLContext.instance.getIdsRunner().getIdsParams().setDetectOnLineMode();
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

				//CSLContext.instance.getIdsRunner().getIdsParams().setIdleMode();

//				String filePath=
//						CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir()+File.separator+
//						CSLContext.instance.getIdsRunner().getIdsParams().getLearnedRulesFileName();

				//CSLContext.instance.getIdsRunner().getIdsParams().setRecordOnlyMode();
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
				
//				j.add(Json.object().set("name", "Models directory")
//						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir()));
//				j.add(Json.object().set("name", "Rules for detection")
//						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getRulesForDetectionFileName()));
//				j.add(Json.object().set("name", "Rules for learning")
//						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getRulesForLearningFileName()));
//				j.add(Json.object().set("name", "Variables")
//						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getVariablesFileName()));
//				j.add(Json.object().set("name", "Learned rules")
//						.set("value", CSLContext.instance.getIdsRunner().getIdsParams().getLearnedRulesFileName()));
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
		
		// 
		addCmd("op_alert", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				Json j=CSLContext.instance.getCSLAlertManager().execOpAlert(params);
				return j;
			}
		});
		
		

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
