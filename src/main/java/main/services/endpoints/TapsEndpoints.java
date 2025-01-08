package main.services.endpoints;

import com.csl.intercom.jsoncmd.JsonCmdHelp;

/**
 * Endpoints for the service Tap
 */
public enum TapsEndpoints implements Endpoint {
    TEST_STREAMING("testStreaming", Endpoint.EMPTY()),
    NEW_TAP("new_tap", new JsonCmdHelp()
            .setDesc("Creation of a new tap handler or modifies it if already present")
            .setParam("name", "name of the tap", JsonCmdHelp.STR)
            .setParam("ip", "[OPT] ip of the tap. Par default 127.0.0.1", JsonCmdHelp.STR)
            .setParam("port", "[OPT] port of the tap. Par default 8888", JsonCmdHelp.STR)
            .setResult("The new tap configuration", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    NEW_TAP_DEPRECATED("newTap", new JsonCmdHelp()
            .setDesc("DEPRECATED : Creation of a new tap handler or modifies it if already present")
            .setParam("name", "name of the tap", JsonCmdHelp.STR)
            .setParam("ip", "[OPT] ip of the tap. Par default 127.0.0.1", JsonCmdHelp.STR)
            .setParam("port", "[OPT] port of the tap. Par default 8888", JsonCmdHelp.STR)
            .setResult("The new tap configuration", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    TAP_NUMBER("tap_number", Endpoint.EMPTY()),
    TAP_NUMBER_DEPRECATED("tapNumber", Endpoint.EMPTY_DEPRECATED()),
    DELETE_TAP("delete_tap", new JsonCmdHelp()
            .setDesc("Suppress a tap handler")
            .setParam("name", "name of the tap", JsonCmdHelp.STR)
            .setResult("The rest of tap handlers", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    DELETE_TAP_DEPRECATED("deleteTap", new JsonCmdHelp()
            .setDesc("DEPRECATED : Suppress a tap handler")
            .setParam("name", "name of the tap", JsonCmdHelp.STR)
            .setResult("The rest of tap handlers", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    SET_IP("set_ip", new JsonCmdHelp()
            .setDesc("Changes the ip to connect a given tap.")
            .setParam("name", "name of the tap to be connected at such ip. ", JsonCmdHelp.STR)
            .setParam("ip", "ip to connect the tap", JsonCmdHelp.STR)
            .setResult("If the change was successful", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    SET_IP_DEPRECATED("setIp", new JsonCmdHelp()
            .setDesc("DEPRECATED : Changes the ip to connect a given tap.")
            .setParam("name", "name of the tap to be connected at such ip. ", JsonCmdHelp.STR)
            .setParam("ip", "ip to connect the tap", JsonCmdHelp.STR)
            .setResult("If the change was successful", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    SET_PORT("set_port", new JsonCmdHelp()
            .setDesc("Changes the port to connect a given tap.")
            .setParam("name", "name of the tap to be connected at such port. ", JsonCmdHelp.STR)
            .setParam("port", "port to connect the tap", JsonCmdHelp.STR)
            .setResult("If the change was successful", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    SET_PORT_DEPRECATED("setPort", new JsonCmdHelp()
            .setDesc("DEPRECATED : Changes the port to connect a given tap.")
            .setParam("name", "name of the tap to be connected at such port. ", JsonCmdHelp.STR)
            .setParam("port", "port to connect the tap", JsonCmdHelp.STR)
            .setResult("If the change was successful", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    SET_USERNAME_PASSWORD("set_username_password", Endpoint.EMPTY()),
    SET_USERNAME_PASSWORD_DEPRECATED("setUsernamePassword", Endpoint.EMPTY_DEPRECATED()),
    SET_NETWORK_NAME("set_network_name", Endpoint.EMPTY()),
    SET_NETWORK_NAME_DEPRECATED("setNetworkName", Endpoint.EMPTY_DEPRECATED()),
    GET_CONF_FROM_TAP("get_conf_from_tap", new JsonCmdHelp()
            .setDesc("Changes the tap configuration: name, concentration ip and port, interfaces, shouldLog...")
            .setParam("name", "name of the tap ", JsonCmdHelp.STR)
            .setResult("The configuration of the tap", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_CONF_FROM_TAP_DEPRECATED("getConfFromTap", new JsonCmdHelp()
            .setDesc("DEPRECATED : Gets the tap configuration: name, concentration ip and port, interfaces, shouldLog...")
            .setParam("name", "name of the tap to be connected at such port. ", JsonCmdHelp.STR)
            .setResult("The configuration of the tap", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    SEND_CONF_TO_TAP("send_conf_to_tap", new JsonCmdHelp()
            .setDesc("Changes the tap configuration: name, concentration ip and port, interfaces, shouldLog...")
            .setParam("name", "name of the tap ", JsonCmdHelp.STR)
            .setResult("The configuration of the tap", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    SEND_CONF_TO_TAP_DEPRECATED("sendConfToTap", new JsonCmdHelp().setDesc("DEPRECATED : changes the tap configuration: name, concentration ip and port, interfaces, shouldLog...")
            .setParam("name", "name of the tap ", JsonCmdHelp.STR)
            .setResult("The configuration of the tap", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_TAP_CNX_CONF("get_tap_conf", new JsonCmdHelp()
            .setDesc("Gets the given configuration to connect the tap : name, id, api ip/port, ...")
            .setParam("name", "name of the given tap.", JsonCmdHelp.STR)
            .setResult("The configuration of the tap", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_TAP_CNX_CONF_DEPRECATED("getTapConf", new JsonCmdHelp().setDesc("DEPRECATED : gets the given configuration to connect the tap : name, id, api ip/port, ...")),
    GET_ALL_TAP_CNX_CONF("get_taps_configuration", new JsonCmdHelp()
            .setDesc("Gets the configuration to connect each taps: name, id, api ip/port, ...")
            .setResult("The configuration of all tap", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_ALL_TAP_CNX_CONF_DEPRECATED("getTapsConfiguration", new JsonCmdHelp()
            .setDesc("DEPRECATED : gets the configuration to connect each taps: name, id, api ip/port, ...")
            .setResult("The configuration of all tap", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_TAP_STATE("get_tap_state", Endpoint.EMPTY()),
    GET_TAP_STATE_DEPRECATED("getTapState", Endpoint.EMPTY_DEPRECATED()),
    START_TAP("start_tap", new JsonCmdHelp()
            .setDesc("Start the given tap")
            .setParam("name", "name of the tap that must start", JsonCmdHelp.STR)
            //	.setResult("nothing", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    START_TAP_DEPRECATED("startTap", new JsonCmdHelp()
            .setDesc("DEPRECATED : Start tap")
            .setParam("name", "name of the tap (id) ", JsonCmdHelp.STR)
            //	.setResult("nothing", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    STOP_TAP("stop_tap", new JsonCmdHelp()
            .setDesc("Stop the given tap")
            .setParam("name", "name of the tap that must stop", JsonCmdHelp.STR)
            //	.setResult("nothing", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    STOP_TAP_DEPRECATED("stopTap", Endpoint.EMPTY_DEPRECATED()),
    RESTART_TAP("restart_tap", new JsonCmdHelp()
            .setDesc("Restarts the given tap")
            .setParam("name", "name of the tap that must restart", JsonCmdHelp.STR)
            //	.setResult("nothing", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    RESTART_TAP_DEPRECATED("restartTap", new JsonCmdHelp().setDesc("DEPRECATED")),
    GET_POSSIBLE_INTERFACES_TAP("get_possible_interfaces_tap", new JsonCmdHelp()
            .setDesc("Gets the list of interfaces of the given tap")
            .setParam("name", "name of the tap that must restart", JsonCmdHelp.STR)
            .setResult("list of possible interfaces", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    SET_INTERFACES_IDS_TAP("set_interfaces_ids_tap", new JsonCmdHelp()
            .setDesc("Changes the interfaces sniffed by the IDS of the given tap")
            .setParam("name", "name of the tap that must restart", JsonCmdHelp.STR)
            .setParam("interfaces", "list of the interfaces to sniff (old ones are erased)", JsonCmdHelp.STR)
            .setResult("list of new ids interfaces", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_INTERFACES_IDS_TAP("get_interfaces_ids_tap", new JsonCmdHelp()
            .setDesc("Get the interfaces sniffed by the IDS of the given tap")
            .setParam("name", "name of the tap that must restart", JsonCmdHelp.STR)
            .setResult("list of the ids interfaces", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    REPLAY("replay", new JsonCmdHelp()
            .setDesc("Start replay of a pcap")
            .setParam("name", "name of the tap (id) ", JsonCmdHelp.STR)
            .setParam("pcap_file", "name of file to replay (must be in /csl) ", JsonCmdHelp.STR)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    START_IDS("start_suricata", new JsonCmdHelp()
            .setDesc("Start/restart Suricata of the given tap")
            .setParam("name", "name of the tap, where suricata should be started.", JsonCmdHelp.STR)
            .setResult("If the (re)starting was successful", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    START_IDS_DEPRECATED("startSuricata", new JsonCmdHelp()
            .setDesc("DEPRECATED : Start/restart Suricata")
            .setResult("If the (re)starting was successful", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    STOP_IDS("stop_suricata", new JsonCmdHelp()
            .setDesc("Stop Suricata of the given tap")
            .setParam("name", "name of the tap, where suricata should be stop. ", JsonCmdHelp.STR)
            .setResult("If the stopping was successful", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    STOP_IDS_DEPRECATED("stopSuricata", new JsonCmdHelp()
            .setDesc("DEPRECATED : Stop Suricata of the given tap")
            .setParam("name", "name of the tap, where suricata should be stop. ", JsonCmdHelp.STR)
            .setResult("If the stopping was successful", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    START_MONITOR("start_monitor", new JsonCmdHelp()
            .setDesc("Start/restart Monitor in the given tap")
            .setParam("name", "name of the tap, where monitor should be started.", JsonCmdHelp.STR)
            .setResult("If the (re)starting was successful", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    STOP_MONITOR("stop_monitor", new JsonCmdHelp()
            .setDesc("Stop Monitor in the given tap.")
            .setParam("name", "name of the tap, where monitor should be stop. ", JsonCmdHelp.STR)
            .setResult("If the stopping was successful", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    RELOAD_ALL_TAPS_RULES("reload_all_taps_rules", new JsonCmdHelp()
            .setDesc("Stop Suricata of the given tap")
            .setParam("name", "name of the tap, where suricata should be stop. ", JsonCmdHelp.STR)
            .setResult("If the stopping was successful", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    RELOAD_ALL_TAPS_RULES_DEPRECATED("reloadAllTapsRules",
            new JsonCmdHelp().setDesc("DEPRECATED : Reloads the rules of all the taps")
                    .setResult("The summary of the update", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    RELOAD_RULES("reload_rules", new JsonCmdHelp().setDesc("Reloads the rules of the given tap")
            .setParam("name", "name of the tap, where the rules should be reloaded. ", JsonCmdHelp.STR)
            .setResult("The entity as returned by CSL-Probe", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    RELOAD_RULES_DEPRECATED("reloadRules",
            new JsonCmdHelp().setDesc("DEPRECATED : Reloads the rules of the given tap")
                    .setParam("name", "name of the tap, where the rules should be reloaded. ", JsonCmdHelp.STR)
                    .setResult("The entity as returned by CSL-Probe", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_PROCESS_JSON("get_process_json",
            new JsonCmdHelp().setDesc("FUTURE : get variables of the industrial process")
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_PROCESS_JSON_DEPRECATED("getProcessJson", Endpoint.EMPTY_DEPRECATED()),
    GET_NETWORK_JSON("get_network_json", Endpoint.EMPTY()),
    GET_NETWORK_JSON_DEPRECATED("getNetworkJson", Endpoint.EMPTY_DEPRECATED()),
    SET_PROCESS_JSON("set_process_json",
            new JsonCmdHelp().setDesc("FUTURE : set variables of the industrial process")
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    SET_PROCESS_JSON_DEPRECATED("setProcessJson", Endpoint.EMPTY_DEPRECATED()),
    SET_NETWORK_JSON("set_network_json", Endpoint.EMPTY()),
    SET_NETWORK_JSON_DEPRECATED("setNetworkJson", Endpoint.EMPTY_DEPRECATED()),
    GET_SURICATA_STATUS("get_suricata_status",
            new JsonCmdHelp().setDesc("Get the Suricata status of the given tap")
                    .setParam("name", "Name of the tap where we query the configuration", JsonCmdHelp.JSON)
                    .setResult("The entity as returned by CSL-Probe", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_SURICATA_STATUS_DEPRECATED("getSuricataStatus", new JsonCmdHelp().setDesc("DEPRECATED : Get the Suricata status of the given tap")
            .setParam("name", "Name of the tap where we query the configuration", JsonCmdHelp.JSON)
            .setResult("The entity as returned by CSL-Probe", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_SURICATA_CONF("get_suricata_conf", new JsonCmdHelp().setDesc("Get the Suricata configuration file (yaml) of the given tap")
            .setParam("name", "Name of the tap where we query the configuration", JsonCmdHelp.JSON)
            .setResult("teh file content of the configuration file", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_SURICATA_CONF_DEPRECATED("getSuricataConf",
            new JsonCmdHelp().setDesc("DEPRECATED : Get the Suricata configuration file of the given tap")
                    .setParam("name", "Name of the tap where we query the configuration", JsonCmdHelp.JSON)
                    .setResult("teh file content of the configuration file", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    SET_SURICATA_CONF("set_suricata_conf",
            new JsonCmdHelp().setDesc("Uploads the new configuration file to the IDS and reloads it. The request part of the file is called file: " +
                            "{\"cmd\":cmdtodo, \"file\":file, \"params\": {...}}")
                    .setParam("name", "The name of the tap", JsonCmdHelp.JSON)
                    .setResult("The new properties values and the tap's name", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    SET_SURICATA_CONF_DEPRECATED("setSuricataConf",
            new JsonCmdHelp().setDesc("DEPRECATED : Uploads the new configuration file to the IDS and reloads it." +
                            "{\"cmd\":cmdtodo, \"file\":file, \"params\": {...}}")
                    .setParam("name", "The name of the tap", JsonCmdHelp.JSON)
                    .setResult("The new properties values and the tap's name", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    RESET_SURICATA_CONF("reset_suricata_conf",
            new JsonCmdHelp().setDesc("Reset the configuration file of IDS")
                    .setParam("name", "The name of the tap", JsonCmdHelp.JSON)
                    .setResult("Whether the reset was successful", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_BASE_RULES("get_base_rules", new JsonCmdHelp().setDesc("Retrieve the base rules of the given tap")
            .setParam("name", "The name of the tap", JsonCmdHelp.JSON)
            .setResult("An object with the list of base rules of the given tap", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_BASE_RULES_DEPRECATED("getBaseRules", new JsonCmdHelp().setDesc("DEPRECATED : Retrieve the base rules of Suricata")
            .setParam("name", "The name of the tap", JsonCmdHelp.JSON)
            .setResult("An object with the sid as key and the rule as value", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_GENERATED_RULES("get_gen_rules", new JsonCmdHelp().setDesc("Retrieve the generated rules of the given tap")
            .setParam("name", "The name of the tap", JsonCmdHelp.STR)
            .setResult("An object with the list of generated rules of the given tap", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_GENERATED_RULES_DEPRECATED("getGenRules", new JsonCmdHelp().setDesc("DEPRECATED : Retrieve the generated rules of Suricata")
            .setResult("An object with the sid as key and the rule as value", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    MODIFY_GENERATED_RULES("modify_gen_rules",
            new JsonCmdHelp().setDesc("Modify the generated rules of the given tap")
                    .setParam("name", "Name of the tap", JsonCmdHelp.STR)
                    .setParam("rules", "A list of strings, each string is a rule", JsonCmdHelp.JSON)
                    .setResult("An object with the sid as keys and the rules as values", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    MODIFY_GENERATED_RULES_DEPRECATED("modifyGenRules",
            new JsonCmdHelp().setDesc("DEPRECATED : Modify the generated rules of Suricata")
                    .setParam("rules", "A list of strings, each string is a rule", JsonCmdHelp.JSON)
                    .setResult("An object with the sid as keys and the rules as values", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    MODIFY_BASE_RULES("modify_base_rules",
            new JsonCmdHelp().setDesc("Modify the base rules of the given tap")
                    .setParam("name", "Name of the tap", JsonCmdHelp.STR)
                    .setParam("rules", "A list of strings, each string is a rule", JsonCmdHelp.JSON)
                    .setResult("An object with the sid as keys and the rules as values", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    MODIFY_BASE_RULES_DEPRECATED("modifyBaseRules",
            new JsonCmdHelp().setDesc("DEPRECATED : Modify the base rules of Suricata")
                    .setParam("name", "Name of the tap", JsonCmdHelp.STR)
                    .setParam("rules", "A list of strings, each string is a rule", JsonCmdHelp.JSON)
                    .setResult("An object with the sid as keys and the rules as values", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    REPLACE_BASE_RULES("replace_base_rules", new JsonCmdHelp().setDesc("Replace the base rules of the given tap")
            .setParam("name", "Name of the tap", JsonCmdHelp.STR)
            .setParam("rules", "A list of strings, each string is a rule", JsonCmdHelp.JSON)
            .setResult("An object with the sid as keys and the rules as values", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    REPLACE_GENERATED_RULES("replace_gen_rules", new JsonCmdHelp().setDesc("Replace the generated rules of the given tap")
            .setParam("name", "Name of the tap", JsonCmdHelp.STR)
            .setParam("rules", "A list of strings, each string is a rule", JsonCmdHelp.JSON)
            .setResult("An object with the sid as keys and the rules as values", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    ADD_BASE_RULES("set_base_rules", new JsonCmdHelp().setDesc("Set the base rules of the given tap")
            .setParam("name", "Name of the tap", JsonCmdHelp.STR)
            .setParam("rules", "A list of strings, each string is a rule", JsonCmdHelp.JSON)
            .setResult("The entity as returned by CSL-Probe, with the new updated rules", JsonCmdHelp.JSON)
            .setStatus(JsonCmdHelp.STATUS_OK)),
    ADD_BASE_RULES_DEPRECATED("setBaseRules",
            new JsonCmdHelp().setDesc("DEPRECATED : Set the base rules of Suricata")
                    .setParam("rules", "A list of strings, each string is a rule", JsonCmdHelp.JSON)
                    .setResult("The entity as returned by CSL-Probe, with the new updated rules", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    ADD_GENERATED_RULES("set_generated_rules",
            new JsonCmdHelp().setDesc("Set the generated rules of the given tap")
                    .setParam("name", "Name of the tap", JsonCmdHelp.STR)
                    .setParam("rules", "A list of strings, each string is a rule", JsonCmdHelp.JSON)
                    .setResult("The entity as returned by CSL-Probe, with the new updated rules", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    ADD_GENERATED_RULES_DEPRECATED("setGeneratedRules",
            new JsonCmdHelp().setDesc("DEPRECATED : Set the generated rules of the given tap")
                    .setParam("rules", "A list of strings, each string is a rule", JsonCmdHelp.JSON)
                    .setResult("The entity as returned by CSL-Probe, with the new updated rules", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    UPDATE_ALL_TAPS_RULES("update_all_taps_suricata_rules",
            new JsonCmdHelp().setDesc("Update all generated suricata rules in all taps")
                    .setParam("rules", "A list of strings, each string is a rule", JsonCmdHelp.JSON)
                    .setResult("The entity as returned by CSL-Probe, with the new updated rules", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    UPDATE_ALL_TAPS_RULES_DEPRECATED("updateAllTapsSuricataRules", Endpoint.EMPTY_DEPRECATED()),
    GET_SURICATA_LOGS("get_suricata_logs",
            new JsonCmdHelp().setDesc("Get the IDS logs of the given tap")
                    .setParam("name", "name of the tap", JsonCmdHelp.JSON)
                    .setResult("The most recent logs of the IDS of the given tap", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_SURICATA_LOGS_DEPRECATED("getSuricataLogs",
            new JsonCmdHelp().setDesc("Clear the IDS logs of the given tap")
                    .setParam("name", "name of the tap", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    CLEAR_SURICATA_LOGS("clear_suricata_logs",
            new JsonCmdHelp().setDesc("Update all generated suricata rules in all taps")
                    .setParam("rules", "A list of strings, each string is a rule", JsonCmdHelp.JSON)
                    .setResult("The entity as returned by CSL-Probe, with the new updated rules", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    CLEAR_SURICATA_LOGS_DEPRECATED("clearSuricataLogs",
            new JsonCmdHelp().setDesc("DEPRECATED : Clear the IDS logs of the given tap")
                    .setParam("name", "name of the tap", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    SEND_INCLUDES("send_includes",
            new JsonCmdHelp().setDesc("FUTURE : Send a list of rules files to tap")
                    .setParam("name", "Name of the tap when the rules will be add.", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    SEND_INCLUDES_DEPRECATED("sendIncludes", Endpoint.EMPTY_DEPRECATED()),
    SET_INCLUDES("set_include",
            new JsonCmdHelp().setDesc("FUTURE : set a list of rules files to tap")
                    .setParam("name", "Name of the tap when the rules will be add.", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    SET_INCLUDES_DEPRECATED("setInclude", Endpoint.EMPTY_DEPRECATED()),
    GET_INCLUDES("get_includes",
            new JsonCmdHelp().setDesc("FUTURE : get a list of rules files to tap")
                    .setParam("name", "Name of the tap when the rules will be add.", JsonCmdHelp.JSON)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_INCLUDES_DEPRECATED("getIncludes", Endpoint.EMPTY_DEPRECATED()),
    TEST("test", Endpoint.EMPTY_DEPRECATED());

    private final String command;
    private final JsonCmdHelp help;

    /**
     * Constructor for the endpoints of Tap service
     *
     * @param command command of the request
     * @param help    help of the command for the api help
     */
    TapsEndpoints(String command, JsonCmdHelp help) {
        this.command = command;
        this.help = help;
    }

    public String cmd() {
        return command;
    }

    public JsonCmdHelp help() {
        return help;
    }
}
