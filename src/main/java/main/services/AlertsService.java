package main.services;

import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.ucsl.interfaces.*;
import com.ucsl.json.Json;

public class AlertsService extends Service {

	public static final String UUID = "uuid";
	public static final String VALUE = "value";

	public AlertsService() {
		this("alerts","ids_conf");
	}

	public AlertsService(String name, String configFileSectionName) {
		super(name,
				"",
				configFileSectionName);
	}

	private void defineServiceEndpoints() {
		addCmd("get_list_active_alerts", params -> {
            System.out.println("start exec <get_list_active_alerts>:"+params);

            params.set("op", "get_list_active");
            return CSLContext.getInstance().getCSLAlertManager().execOpAlert(params);
        },
				new JsonCmdHelp()
				.setDesc("returns list of active alerts")

				.setStatus(JsonCmdHelp.STATUS_TODO)
				);


		addCmd("get_number_active_alerts_by_level", params -> {
            params.set("op", "get_number_active_by_level");
            return CSLContext.getInstance().getCSLAlertManager().execOpAlert(params);
        },
				new JsonCmdHelp()
				.setDesc("returns list of active alerts by level")

				.setStatus(JsonCmdHelp.STATUS_OK)
				);
		//get_list_active_alerts_count

		addCmd("get_list_acked_alerts", params -> {
            params.set("op", "get_list_acked");
            return CSLContext.getInstance().getCSLAlertManager().execOpAlert(params);

        },
				new JsonCmdHelp()
				.setDesc("returns list of acked alerts (to be analyzed by manager)")

				.setStatus(JsonCmdHelp.STATUS_TODO)
				);


		addCmd("get_list_masked_alerts", params -> {
            params.set("op", "get_list_masked");
            return CSLContext.getInstance().getCSLAlertManager().execOpAlert(params);
        },
				new JsonCmdHelp()
				.setDesc("returns list of masked alerts (for some duration)")

				.setStatus(JsonCmdHelp.STATUS_TODO)
				);

		addCmd("get_list_added_to_model_alerts", params -> {
            params.set("op", "get_list_added_to_model");
            return CSLContext.getInstance().getCSLAlertManager().execOpAlert(params);
        },
				new JsonCmdHelp()
				.setDesc("returns list alerts added to the model of normal behavior of the system")

				.setStatus(JsonCmdHelp.STATUS_TODO)
				);


		addCmd("get_list_inactive_alerts", params -> {
            params.set("op", "get_list_inactive");
            return CSLContext.getInstance().getCSLAlertManager().execOpAlert(params);

        },
				new JsonCmdHelp()
				.setDesc("returns list of inactive alerts (masked or added to model)")

				.setStatus(JsonCmdHelp.STATUS_TODO)
				);


		addCmd("get_list_all_alerts", params -> {
            params.set("op", "get_list_all");
            return CSLContext.getInstance().getCSLAlertManager().execOpAlert(params);

        },
				new JsonCmdHelp()
				.setDesc("returns list of alerts ")

				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		addCmd("set_acked", params -> {
            params.set("op", "set_acked");
            return CSLContext.getInstance().getCSLAlertManager().execOpAlert(params);
        },
				new JsonCmdHelp()
				.setDesc("set alert as acked")
				.setParam(UUID, "uuid of alert", JsonCmdHelp.STR)
				.setParam(VALUE, "true to set, false to unset", JsonCmdHelp.BOOL)

				.setStatus(JsonCmdHelp.STATUS_TODO)
				);

		addCmd("set_masked", params -> {
            params.set("op", "set_masked");
            Json a=CSLContext.getInstance().getCSLAlertManager().execOpAlert(params);
            return a;
        },
				new JsonCmdHelp()
				.setDesc("set alert as masked")
				.setParam(UUID, "uuid of alert", JsonCmdHelp.STR)
				.setParam("time_for_end_of_mask", "time of the end of the mask (ms from January 1, 1970 UTC) ", JsonCmdHelp.LONG)
				.setParam(VALUE, "true to set, false to unset", JsonCmdHelp.BOOL)

				.setStatus(JsonCmdHelp.STATUS_TODO)
				);


		addCmd("set_added_to_model", params -> {
            params.set("op", "add_to_model");
            return CSLContext.getInstance().getCSLAlertManager().execOpAlert(params);
        },
				new JsonCmdHelp()
				.setDesc("add alert to model  or remove it")
				.setParam(UUID, "uuid of alert", JsonCmdHelp.STR)
				.setParam(VALUE, "true to add, false to remove", JsonCmdHelp.BOOL)
				.setParam("level", "level of alert in the model", JsonCmdHelp.INT)

				.setStatus(JsonCmdHelp.STATUS_TODO)
				);


		addCmd("stats", params -> CSLContext.getInstance().getCSLAlertManager().getAlertStats(),
				new JsonCmdHelp()
				.setDesc("return the number of active alerts ")
				.setResult("{ number_of_alerts:{all: ,l0:  .. l4: } }", JsonCmdHelp.JSON)
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);


		addCmd("clear_list_of_all_alerts", params -> {
            Json j=Json.object();
            j.set("result", "ok");
            j.set(VALUE,1);


            CSLContext.getInstance().getCSLAlertManager().resetListOfCurrentAlerts();

            return Json.object().set("info", "ok");
        },
				new JsonCmdHelp()
				.setDesc("clear all alerts (mainly for tests)")

				.setStatus(JsonCmdHelp.STATUS_OK)
				);




		addCmd("test_alert1", params -> {
            Json j=Json.object();
            j.set("result", "ok");
            j.set(VALUE,1);

            params.set("op", "test1");

            CSLContext.getInstance().getCSLAlertManager().execOpAlert(params);

            return  Json.object();
        },
				new JsonCmdHelp()
				.setDesc("send test alerts (small number of alerts)")

				.setStatus(JsonCmdHelp.STATUS_OK)
				);


		addCmd("test_alert2", params -> {
            System.out.println("start exec ");
            System.out.println("Exec JCmd test_cmd :"+params);
            System.out.println("Fin exec");

            params.set("op", "test2");

            CSLContext.getInstance().getCSLAlertManager().execOpAlert(params);

            return  Json.object();
        },
				new JsonCmdHelp()
				.setDesc("send a lot alerts (more than 1000 alerts)")

				.setStatus(JsonCmdHelp.STATUS_OK)
				);

		// Gestion des alertes
		addCmd("get_alerts_list", params -> CSLContext.getInstance().getCSLAlertManager().getListOfCurrentAlertsAsJson(),
				new JsonCmdHelp()
				.setDesc("")
				.setStatus(JsonCmdHelp.STATUS_TODO).hide()
				);

		//
		addCmd("op_alert", params -> CSLContext.getInstance().getCSLAlertManager().execOpAlert(params),
				new JsonCmdHelp().setDesc("Operations on alerts").hide()
				);

	}

	@Override
	public boolean init() {
		defineServiceEndpoints();
		return true;
	}
}
