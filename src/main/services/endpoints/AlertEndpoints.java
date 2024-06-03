package main.services.endpoints;

import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.ucsl.interfaces.IJsonCmdHelp;

/**
 * Endpoints for the service Alerts
 */
public enum AlertEndpoints implements Endpoint {
    GET_LIST_ACTIVE_ALERTS("get_list_active_alerts",
            new JsonCmdHelp()
                    .setDesc("returns list of active alerts")
                    .setStatus(JsonCmdHelp.STATUS_TODO)),
    GET_NUMBER_ACTIVE_ALERT_BY_LEVEL("get_number_active_alerts_by_level",
            new JsonCmdHelp()
                    .setDesc("returns list of active alerts by level")
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_LIST_ACKED_ALERTS("get_list_acked_alerts",
            new JsonCmdHelp()
                    .setDesc("returns list of acked alerts (to be analyzed by manager)")
                    .setStatus(JsonCmdHelp.STATUS_TODO)),
    GET_LIST_MASKED_ALERTS("get_list_masked_alerts",
            new JsonCmdHelp()
                    .setDesc("returns list of masked alerts (for some duration)")
                    .setStatus(JsonCmdHelp.STATUS_TODO)),
    GET_LIST_ADDED_TO_MODEL_ALERTS("get_list_added_to_model_alerts",
            new JsonCmdHelp()
                    .setDesc("returns list alerts added to the model of normal behavior of the system")
                    .setStatus(JsonCmdHelp.STATUS_TODO)),
    GET_LIST_INACTIVE_ALERTS("get_list_inactive_alerts",
            new JsonCmdHelp()
                    .setDesc("returns list of inactive alerts (masked or added to model)")
                    .setStatus(JsonCmdHelp.STATUS_TODO)),
    GET_LIST_ALL_ALERTS("get_list_all_alerts",
            new JsonCmdHelp()
                    .setDesc("returns list of alerts ")
                    .setStatus(JsonCmdHelp.STATUS_TODO)),
    SET_ACKED("set_acked",
            new JsonCmdHelp()
                    .setDesc("set alert as acked")
                    .setParam("uuid", "uuid of alert", JsonCmdHelp.STR)
                    .setParam("value", "true to set, false to unset", JsonCmdHelp.BOOL)
                    .setStatus(JsonCmdHelp.STATUS_TODO)),
    SET_MASKED("set_masked",
            new JsonCmdHelp()
                    .setDesc("set alert as masked")
                    .setParam("uuid", "uuid of alert", JsonCmdHelp.STR)
                    .setParam("time_for_end_of_mask", "time of the end of the mask (ms from January 1, 1970 UTC) ", JsonCmdHelp.LONG)
                    .setParam("value", "true to set, false to unset", JsonCmdHelp.BOOL)
                    .setStatus(JsonCmdHelp.STATUS_TODO)),
    SET_ADDED_TO_MODEL("set_added_to_model",
            new JsonCmdHelp()
                    .setDesc("add alert to model  or remove it")
                    .setParam("uuid", "uuid of alert", JsonCmdHelp.STR)
                    .setParam("value", "true to add, false to remove", JsonCmdHelp.BOOL)
                    .setParam("level", "level of alert in the model", JsonCmdHelp.INT)
                    .setStatus(JsonCmdHelp.STATUS_TODO)),
    STATS("stats",
            new JsonCmdHelp()
                    .setDesc("return the number of active alerts ")
                    .setResult("{ number_of_alerts:{all: ,l0:  .. l4: } }", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_TODO)),
    CLEAR_LIST_OF_ALL_ALERTS("clear_list_of_all_alerts",
            new JsonCmdHelp()
                    .setDesc("clear all alerts (mainly for tests)")

                    .setStatus(JsonCmdHelp.STATUS_OK)),
    TEST_ALERT0("test_alert0",
            new JsonCmdHelp()
                    .setDesc("send test alerts (1 alert)")

                    .setStatus(JsonCmdHelp.STATUS_OK)),
    TEST_ALERT1("test_alert1",
            new JsonCmdHelp()
                    .setDesc("send test alerts (small number of alerts)")

                    .setStatus(JsonCmdHelp.STATUS_OK)),
    TEST_ALERT2("test_alert2",
            new JsonCmdHelp()
                    .setDesc("send a lot alerts (more than 1000 alerts)")

                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GAT_ALERTS_LIST("get_alerts_list",
            new JsonCmdHelp()
                    .setDesc("")
                    .setStatus(JsonCmdHelp.STATUS_TODO).hide()),
    OP_ALERT("op_alert",
            new JsonCmdHelp().setDesc("Operations on alerts").hide()
    );

    private final String command;
    private final JsonCmdHelp help;

    /**
     * Constructor for the endpoints of Alert service
     *
     * @param command command of the request
     * @param help    help of the command for the api help
     */
    AlertEndpoints(String command, IJsonCmdHelp help) {
        this.command = command;
        this.help = (JsonCmdHelp) help;
    }

    public String cmd() {
        return command;
    }

    public JsonCmdHelp help() {
        return help;
    }
}
