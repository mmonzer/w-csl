package main.services;

import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.ucsl.interfaces.*;
import com.ucsl.json.Json;

public class AlertsService extends Service {

	public AlertsService() {
		this("alerts","ids_conf");
	}

	public AlertsService(String name, String configFileSectionName) {
		super(name,
				"",
				configFileSectionName);
	}


	private void defineServiceEndpoints() {
		addCmd("get_list_active_alerts", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("start exec <get_list_active_alerts>:"+params);

				params.set("op", "get_list_active");
				return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
			}
		},
				new JsonCmdHelp()
				.setDesc("returns list of active alerts")

				.setStatus(JsonCmdHelp.STATUS_TODO)
				);


		addCmd("get_number_active_alerts_by_level", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				params.set("op", "get_number_active_by_level");
				return CSLContext.instance.getCSLAlertManager().execOpAlert(params);
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
				Json j=Json.object();
				j.set("result", "ok");
				j.set("value",1);


				CSLContext.instance.getCSLAlertManager().resetListOfCurrentAlerts();

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
				Json j=Json.object();
				j.set("result", "ok");
				j.set("value",1);



				IAlertDescriptor a3= CSLContext.instance.getIDSMainProcessor().getAlertFactory().
						createAlertDescriptor(3, "ALERT 3", System.currentTimeMillis());
				CSLContext.instance.getCSLAlertManager().sendAlert(a3);

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
				Json j=Json.object();
				j.set("result", "ok");
				j.set("value",1);

				params.set("op", "test1");

				CSLContext.instance.getCSLAlertManager().execOpAlert(params);

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

				return  Json.object();
			}
		},
				new JsonCmdHelp()
				.setDesc("send a lot alerts (more than 1000 alerts)")

				.setStatus(JsonCmdHelp.STATUS_OK)
				);

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

	}

	public String addCmd(String name, IJsonCmd j) {
		return apiCommands.registerCmd(name, j);
	}

	public String addCmd(String name, IJsonCmd j, IJsonCmdHelp jh) {
		return apiCommands.registerCmd(name, j,jh);
	}

	@Override
	public boolean init() {
		defineServiceEndpoints();
		return true;
	}

	@Override
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}
}
