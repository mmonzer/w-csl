package main.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.ucsl.interfaces.IAlertDescriptor;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.ICSLService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;

public class AlertsService extends Service {
	//private IIDSRunner idsRunner=null;

	/*public void setIDSRunner(IDSRunner idsRunner) {
		// TODO Auto-generated method stub
		this.idsRunner=idsRunner;
	} */

	/**
	 * Default constructor of the Alerts service.
	 */
	public AlertsService() {
		this("alerts",
				"Service that deals with the IDS alerts. Get the alerts and change the different parameters",
	"ids_conf");
	}

	/**
	 * Generic constructor of the Alerts service.
	 */
	public AlertsService(String name, String description, String configFileSectionName) {
		super(name, description, configFileSectionName);
	}

	/**
	 * Initialization of the Alerts service commands
	 * @param jConfig the configuration section of the configuration file
	 * @param cslDir the CSL directory
	 * @return true if the initialization happened with no problems, false otherwise.
	 */
	public boolean init(Json jConfig, String cslDir) {
		
		
		//idsRunner=CSLContext.instance.getIdsRunner();

		
		
		
		
	
		addCmd("get_list_active_alerts", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("start exec <get_list_active_alerts>:"+params);
				
				params.set("op", "get_list_active");
				return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
				
				
				
			//	return Json.object();
			}
		},
				new JsonCmdHelp()
				.setDesc("returns list of active alerts")
					
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		
		
		addCmd("get_number_active_alerts_by_level", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
			//	System.out.println("start exec <get_list_active_alerts>:"+params);
				
				params.set("op", "get_number_active_by_level");
				return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
				
				
				
			//	return Json.object();
			}
		},
				new JsonCmdHelp()
				.setDesc("returns list of active alerts by level")
					
				.setStatus(JsonCmdHelp.STATUS_OK)
				);
		//get_list_active_alerts_count
		
		addCmd("get_list_acked_alerts", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				params.set("op", "get_list_acked");
				return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
				
			}
		},
				new JsonCmdHelp()
				.setDesc("returns list of acked alerts (to be analyzed by manager)")
					
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		
		
		addCmd("get_list_masked_alerts", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				params.set("op", "get_list_masked");
				return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
			}
		},
				new JsonCmdHelp()
				.setDesc("returns list of masked alerts (for some duration)")
					
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		
		addCmd("get_list_added_to_model_alerts", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				params.set("op", "get_list_added_to_model");
				return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
			}
		},
				new JsonCmdHelp()
				.setDesc("returns list alerts added to the model of normal behavior of the system")
					
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		
		
		addCmd("get_list_inactive_alerts", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				params.set("op", "get_list_inactive");
				return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
				
			}
		},
				new JsonCmdHelp()
				.setDesc("returns list of inactive alerts (masked or added to model)")
					
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		
		
		addCmd("get_list_all_alerts", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				params.set("op", "get_list_all");
				return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
				
			}
		},
				new JsonCmdHelp()
				.setDesc("returns list of alerts ")
					
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		
//		addCmd("get_list_of_alerts_added_to_model", new JsonCmd() {
//
//			@Override
//			public Json exec(Json params) {
//				System.out.println("start exec ");
//				System.out.println("Exec JCmd test_cmd :"+params);
//				System.out.println("Fin exec");
//				Json j=Json.object();
//				j.set("result", "ok");
//				j.set("value",1);
//
//				return Json.object();
//			}
//		},
//				new JsonCmdHelp()
//				.setDesc("returns list of all alerts")
//					
//				.setStatus(JsonCmdHelp.STATUS_TODO)
//				);
//		
		addCmd("set_acked", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				params.set("op", "set_acked");
				return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
			}
		},
				new JsonCmdHelp()
				.setDesc("set alert as acked")
				.setParam("uuid", "uuid of alert", JsonCmdHelp.STR)	
				.setParam("value", "true to set, false to unset", JsonCmdHelp.BOOL)
				
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		
		addCmd("set_masked", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				params.set("op", "set_masked");
				Json a=CSLContext.instance.getCSLAlertManager().execOpAlert(params);
				return a;
			}
		},
				new JsonCmdHelp()
				.setDesc("set alert as masked")
				.setParam("uuid", "uuid of alert", JsonCmdHelp.STR)	
				.setParam("time_for_end_of_mask", "time of the end of the mask (ms from January 1, 1970 UTC) ", JsonCmdHelp.LONG)	
				.setParam("value", "true to set, false to unset", JsonCmdHelp.BOOL)
				
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		
		
		addCmd("set_added_to_model", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				params.set("op", "add_to_model");
				return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
			}
		},
				new JsonCmdHelp()
				.setDesc("add alert to model  or remove it")
				.setParam("uuid", "uuid of alert", JsonCmdHelp.STR)	
				.setParam("value", "true to add, false to remove", JsonCmdHelp.BOOL)
				.setParam("level", "level of alert in the model", JsonCmdHelp.INT)
				
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		
		
		addCmd("stats", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
//				System.out.println("start exec ");
//				System.out.println("Exec JCmd test_cmd :"+params);
//				System.out.println("Fin exec");
				return CSLContext.instance.getCSLAlertManager().getAlertStats();
				

			}
		},
				new JsonCmdHelp()
				.setDesc("return the number of active alerts ")
				.setResult("{ number_of_alerts:{all: ,l0:  .. l4: } }", JsonCmdHelp.JSON)
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		
		
		addCmd("clear_list_of_all_alerts", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
//				System.out.println("start exec ");
//				System.out.println("Exec JCmd test_cmd :"+params);
//				System.out.println("Fin exec");
				Json j=Json.object();
				j.set("result", "ok");
				j.set("value",1);

				
				CSLContext.instance.getCSLAlertManager().resetListOfCurrentAlerts();
				
				//CSLAlertManager.instance.sendAlert("HIGH","test alert","xxx=testval");
				return Json.object().set("info", "ok");
			}
		},
				new JsonCmdHelp()
				.setDesc("clear all alerts (mainly for tests)")
					
				.setStatus(JsonCmdHelp.STATUS_OK)
				);
		
		
		
		addCmd("test_alert0", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
//				System.out.println("start exec ");
//				System.out.println("Exec JCmd test_cmd :"+params);
//				System.out.println("Fin exec");
				Json j=Json.object();
				j.set("result", "ok");
				j.set("value",1);

			
				
				IAlertDescriptor a3= CSLContext.instance.getIDSMainProcessor().getAlertFactory().
						createAlertDescriptor(3, "ALERT 3", System.currentTimeMillis());
				CSLContext.instance.getCSLAlertManager().sendAlert(a3);
			
				//CSLAlertManager.instance.sendAlert("HIGH","test alert","xxx=testval");
				
				Json list=Json.array();
				return list.add(a3.toJson());
			}
		},
				new JsonCmdHelp()
				.setDesc("send test alerts (1 alert)")
					
				.setStatus(JsonCmdHelp.STATUS_OK)
				);
		
		
		
		
		addCmd("test_alert1", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
//				System.out.println("start exec ");
//				System.out.println("Exec JCmd test_cmd :"+params);
//				System.out.println("Fin exec");
				Json j=Json.object();
				j.set("result", "ok");
				j.set("value",1);

			
				
				params.set("op", "test1");
				
				CSLContext.instance.getCSLAlertManager().execOpAlert(params);
				
				
				
				//CSLAlertManager.instance.sendAlert("HIGH","test alert","xxx=testval");
				return  Json.object();
			}
		},
				new JsonCmdHelp()
				.setDesc("send test alerts (small number of alerts)")
					
				.setStatus(JsonCmdHelp.STATUS_OK)
				);
		
		
		addCmd("test_alert2", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("start exec ");
				System.out.println("Exec JCmd test_cmd :"+params);
				System.out.println("Fin exec");
				
				params.set("op", "test2");
			
				CSLContext.instance.getCSLAlertManager().execOpAlert(params);
				
				
				
				//CSLAlertManager.instance.sendAlert("HIGH","test alert","xxx=testval");
				return  Json.object();
			}
		},
				new JsonCmdHelp()
				.setDesc("send a lot alerts (more than 1000 alerts)")
					
				.setStatus(JsonCmdHelp.STATUS_OK)
				);

		
		
		
		
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


	

		
		// Gestion des alertes
		addCmd("get_alerts_list", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				Json j=CSLContext.instance.getCSLAlertManager().getListOfCurrentAlertsAsJson();
				return j;
			}
		},
				new JsonCmdHelp()
				.setDesc("")
				.setStatus(JsonCmdHelp.STATUS_TODO).hide()
				);
		
		// 
		addCmd("op_alert", new IJsonCmd() {

			@Override
			public Json exec(Json params) {

				Json j=CSLContext.instance.getCSLAlertManager().execOpAlert(params);
				return j;
			}
		},
				new JsonCmdHelp().setDesc("Operations on alerts").hide()
				);
		
		

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
//			IDSTrace.log(IDSTrace.WEB_DATABASE,
//					"File Contents="+result);

		} else {
			j.set("contents",Json.object());
			j.set("error", "Nof file with name:"+fullname);
//			IDSTrace.log(IDSTrace.WEB_DATABASE,
//					"File Load error="+j.toString());

		}

		return j;
	}

	static public String startOf(String s) {
		int MAX=50;
		if (s.length()<=MAX) return s;
		else return s.substring(0,MAX-1)+"...";
	}



}
