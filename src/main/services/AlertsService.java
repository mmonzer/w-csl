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
import main.services.endpoints.AlertEndpoints;

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
     *
     * @param jConfig the configuration section of the configuration file
     * @param cslDir  the CSL directory
     * @return true if the initialization happened with no problems, false otherwise.
     */
    public boolean init(Json jConfig, String cslDir) {


        //idsRunner=CSLContext.instance.getIdsRunner();


        addCmd(AlertEndpoints.GET_LIST_ACTIVE_ALERTS, new IJsonCmd() {

            @Override
            public Json exec(Json params) {
                System.out.println("start exec <get_list_active_alerts>:" + params);

                params.set("op", "get_list_active");
                return CSLContext.instance.getCSLAlertManager().execOpAlert(params);


                //	return Json.object();
            }
        });
        addCmd(AlertEndpoints.GET_NUMBER_ACTIVE_ALERT_BY_LEVEL, new IJsonCmd() {

            @Override
            public Json exec(Json params) {
                //	System.out.println("start exec <get_list_active_alerts>:"+params);

                params.set("op", "get_number_active_by_level");
                return CSLContext.instance.getCSLAlertManager().execOpAlert(params);


                //	return Json.object();
            }
        });
        addCmd(AlertEndpoints.GET_LIST_ACKED_ALERTS, new IJsonCmd() {

            @Override
            public Json exec(Json params) {
                params.set("op", "get_list_acked");
                return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
            }
        });
        addCmd(AlertEndpoints.GET_LIST_MASKED_ALERTS, new IJsonCmd() {

            @Override
            public Json exec(Json params) {
                params.set("op", "get_list_masked");
                return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
            }
        });
        addCmd(AlertEndpoints.GET_LIST_ADDED_TO_MODEL_ALERTS, new IJsonCmd() {

            @Override
            public Json exec(Json params) {
                params.set("op", "get_list_added_to_model");
                return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
            }
        });
        addCmd(AlertEndpoints.GET_LIST_INACTIVE_ALERTS, new IJsonCmd() {

            @Override
            public Json exec(Json params) {
                params.set("op", "get_list_inactive");
                return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
            }
        });
        addCmd(AlertEndpoints.GET_LIST_ALL_ALERTS, new IJsonCmd() {

            @Override
            public Json exec(Json params) {
                params.set("op", "get_list_all");
                return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
            }
        });
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
        addCmd(AlertEndpoints.SET_ACKED, new IJsonCmd() {

            @Override
            public Json exec(Json params) {
                params.set("op", "set_acked");
                return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
            }
        });
        addCmd(AlertEndpoints.SET_MASKED, new IJsonCmd() {

            @Override
            public Json exec(Json params) {
                params.set("op", "set_masked");
                Json a = CSLContext.instance.getCSLAlertManager().execOpAlert(params);
                return a;
            }
        });
        addCmd(AlertEndpoints.SET_ADDED_TO_MODEL, new IJsonCmd() {

            @Override
            public Json exec(Json params) {
                params.set("op", "add_to_model");
                return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
            }
        });
        addCmd(AlertEndpoints.STATS, new IJsonCmd() {

            @Override
            public Json exec(Json params) {
//				System.out.println("start exec ");
//				System.out.println("Exec JCmd test_cmd :"+params);
//				System.out.println("Fin exec");
                return CSLContext.instance.getCSLAlertManager().getAlertStats();
            }
        });
        addCmd(AlertEndpoints.CLEAR_LIST_OF_ALL_ALERTS, new IJsonCmd() {

            @Override
            public Json exec(Json params) {
//				System.out.println("start exec ");
//				System.out.println("Exec JCmd test_cmd :"+params);
//				System.out.println("Fin exec");
                Json j = Json.object();
                j.set("result", "ok");
                j.set("value", 1);


                CSLContext.instance.getCSLAlertManager().resetListOfCurrentAlerts();

                //CSLAlertManager.instance.sendAlert("HIGH","test alert","xxx=testval");
                return Json.object().set("info", "ok");
            }
        });
        addCmd(AlertEndpoints.TEST_ALERT0, new IJsonCmd() {

            @Override
            public Json exec(Json params) {
//				System.out.println("start exec ");
//				System.out.println("Exec JCmd test_cmd :"+params);
//				System.out.println("Fin exec");
                Json j = Json.object();
                j.set("result", "ok");
                j.set("value", 1);


                IAlertDescriptor a3 = CSLContext.instance.getIDSMainProcessor().getAlertFactory().
                        createAlertDescriptor(3, "ALERT 3", System.currentTimeMillis());
                CSLContext.instance.getCSLAlertManager().sendAlert(a3);

                //CSLAlertManager.instance.sendAlert("HIGH","test alert","xxx=testval");

                Json list = Json.array();
                return list.add(a3.toJson());
            }
        });
        addCmd(AlertEndpoints.TEST_ALERT1, new IJsonCmd() {

            @Override
            public Json exec(Json params) {
//				System.out.println("start exec ");
//				System.out.println("Exec JCmd test_cmd :"+params);
//				System.out.println("Fin exec");
                Json j = Json.object();
                j.set("result", "ok");
                j.set("value", 1);


                params.set("op", "test1");

                CSLContext.instance.getCSLAlertManager().execOpAlert(params);


                //CSLAlertManager.instance.sendAlert("HIGH","test alert","xxx=testval");
                return Json.object();
            }
        });
        addCmd(AlertEndpoints.TEST_ALERT2, new IJsonCmd() {

            @Override
            public Json exec(Json params) {
                System.out.println("start exec ");
                System.out.println("Exec JCmd test_cmd :" + params);
                System.out.println("Fin exec");

                params.set("op", "test2");

                CSLContext.instance.getCSLAlertManager().execOpAlert(params);


                //CSLAlertManager.instance.sendAlert("HIGH","test alert","xxx=testval");
                return Json.object();
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


        // Gestion des alertes
        addCmd(AlertEndpoints.GAT_ALERTS_LIST, new IJsonCmd() {

            @Override
            public Json exec(Json params) {

                Json j = CSLContext.instance.getCSLAlertManager().getListOfCurrentAlertsAsJson();
                return j;
            }
        });
        addCmd(AlertEndpoints.OP_ALERT, new IJsonCmd() {

            @Override
            public Json exec(Json params) {

                Json j = CSLContext.instance.getCSLAlertManager().execOpAlert(params);
                return j;
            }
        });


        return true;  // ok to start
    }

    static private String readAnyFile(String path) {


        String content = "";

        try {
            content = new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            e.printStackTrace();
            return "{\"Error\":\"File not found:" + e.getMessage() + "\"}";
        }

        return content;
    }

    static private Json loadAnyFileAsJson(String fullname) {


        String result = "";
        Json j = Json.object();

        if (fullname != null) {
            result = readAnyFile(fullname);
            Json z = Json.read(result);
            j.set("contents", z);
//			IDSTrace.log(IDSTrace.WEB_DATABASE,
//					"File Contents="+result);

        } else {
            j.set("contents", Json.object());
            j.set("error", "Nof file with name:" + fullname);
//			IDSTrace.log(IDSTrace.WEB_DATABASE,
//					"File Load error="+j.toString());

        }

        return j;
    }

    static public String startOf(String s) {
        int MAX = 50;
        if (s.length() <= MAX) return s;
        else return s.substring(0, MAX - 1) + "...";
    }
}
