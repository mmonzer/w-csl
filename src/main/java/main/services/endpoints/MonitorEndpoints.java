package main.services.endpoints;

import com.csl.intercom.jsoncmd.JsonCmdHelp;

/**
 * Endpoints for the service Monitor
 */
public enum MonitorEndpoints implements Endpoint {
    STATS_DEVICES("stats_devices",
            new JsonCmdHelp()
                    .setDesc("return the number of devices ")
                    .setResult("{all:, running: ", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_TODO)),

    STATS_TAPS("stats_taps",
            new JsonCmdHelp()
                    .setDesc("return the number of taps ")
                    .setResult("{all:n, running: ", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_TODO)),

    SET_INTERFACES_MONITOR_TAP("set_interfaces_monitor_tap",
            new JsonCmdHelp()
                    .setDesc("Set the new monitoring interfaces of the given tap")
                    .setParam("name", "name of the tap", JsonCmdHelp.STR)
                    .setParam("interfaces", "list of the new interfaces", JsonCmdHelp.STR)
                    .setResult("list of the new interfaces", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_TODO)),

    GET_INTERFACES_MONITOR_TAP("get_interfaces_monitor_tap",
            new JsonCmdHelp()
                    .setDesc("Ge the monitoring interfaces of the given tap")
                    .setParam("name", "name of the tap", JsonCmdHelp.STR)
                    .setResult("list of the monitored interfaces in the given tap", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_TODO)
    );

    private final String command;
    private final JsonCmdHelp help;

    /**
     * Constructor for the endpoints of Monitor service
     *
     * @param command command of the request
     * @param help    help of the command for the api help
     */
    MonitorEndpoints(String command, JsonCmdHelp help) {
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
