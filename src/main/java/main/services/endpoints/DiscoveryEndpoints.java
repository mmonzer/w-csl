package main.services.endpoints;

import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.ucsl.interfaces.IJsonCmdHelp;


/**
 * Endpoints for the service Discovery
 */
public enum DiscoveryEndpoints implements Endpoint {
    GET_STATUS("get_status",
            new JsonCmdHelp()
                    .setDesc("Retrieve the status of the service.")
                    .setResult("A status notification: " +
                            "<code>" +
                            "{" +
                            "\"is_http_api_reachable\": true/false" +
                            "\"is_websocket_connected\": true/false" +
                            "}" +
                            "</code>", IJsonCmdHelp.JSON).setStatus(IJsonCmdHelp.STATUS_OK)),
    LIST_ENTITIES("list_entities",
            new JsonCmdHelp().setDesc("Retieve the entities registered in CSL-Scan")
                    .setResult("The list of entities' information as returned by CSL-Scan, in the format" +
                            "<code>{\"success\": true, \"result\": [...]}</code>", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    GET_ENTITY("get_entity",
            new JsonCmdHelp().setDesc("Retrieve a specific entity from CSL-Scan")
                    .setParam("id", "The uuid of the entity to retrieve", IJsonCmdHelp.STR)
                    .setResult("The entity as returned by CSL-Scan", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    DELETE_ENTITY("delete_entity",
            new JsonCmdHelp().setDesc("Remove a specific entity from CSL-Scan")
                    .setParam("id", "The uuid of the entity to delete", IJsonCmdHelp.STR)
                    .setResult("<code>{ \"success\": true/false }</code>", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    ADD_CONNECTION("add_connection",
            new JsonCmdHelp().setDesc("Add a connection to CSL-Scan")
                    .setParam("connection", "The connection to add", IJsonCmdHelp.JSON)
                    .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                            "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    DELETE_CONNECTION("delete_connection",
            new JsonCmdHelp().setDesc("Delete a connection from CSL-Scan")
                    .setParam("id", "The uuid of the connection to delete", IJsonCmdHelp.STR)
                    .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                            "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),

    GET_ALL_CPES("get_all_cpes",
            new JsonCmdHelp().setDesc("Get the CPE Items in CSL-Scan")
                    .setResult("The list of CPE Items, in the format <code>{\"success\": true, \"result\": [...]}", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    GET_ENTITY_CPES("get_entity_cpes",
            new JsonCmdHelp().setDesc("Get an entity's CPE Items")
                    .setParam("id", "The entity's uuid", IJsonCmdHelp.STR)
                    .setResult("The list of CPE Items of the entity, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    GET_CPES_SINCE("get_cpes_since",
            new JsonCmdHelp().setDesc("Retrieve CPE Items that change strictly after a specified date")
                    .setParam("date", "in ISO format, example: 2023-04-13T13:56:56.66 (local date format)", IJsonCmdHelp.STR)
                    .setResult("The list of CPE Items that changed strictly after <code>date</code>, in the format" +
                            "<code>{\"success\": true, \"result\": [...]</code>", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    SCAN_STATUS("scan_status",
            new JsonCmdHelp().setDesc("Get the status of a specific scan")
                    .setParam("id", "The uuid of the scan to inquire", JsonCmdHelp.STR)
                    .setResult("The status of the scan, in the format <code>{ \"success\": true, \"result\": { ... } }</code>", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    START_SCAN("start_scan",
            new JsonCmdHelp().setDesc("Start a scan from CSL-Scan")
                    .setParam("entities", "An array of strings with the uuids of the entities to scan. May be omitted or null, resulting in scanning all entities.", IJsonCmdHelp.JSON)
                    .setResult("<code>{ \"success\": true }</code> if the scan was started successfully", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    STOP_SCAN("stop_scan",
            new JsonCmdHelp().setDesc("Stop a scan in CSL-Scan")
                    .setParam("id", "The uuid of the scan to stop", IJsonCmdHelp.STR)
                    .setResult("<code>{ \"success\": true }</code> if the scan was stopped successfully", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    SYNCHRONIZE_DEVICES("synchronize_devices",
            new JsonCmdHelp().setDesc("Synchronize devices between DB-API and CSL-Scan.")
                    .setResult("<code>{\"success\": true }</code> if the synchronisation went without error," +
                            "<code>{\"success\": false, \"error\", {\"reason\": \"...\", \"failed_devices\": [...]}}</code> otherwise. The failed_devices field is present if devices were actually fetched from DB-API.", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    DROP_ALL_COLLECTIONS("drop_all_collections",
            new JsonCmdHelp().setDesc("Drop all collections in DB-API")
                    .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                            "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    GET_ALL_ENTITY_HTTP_CONNECTIONS("get_entity_http_connections",
            new JsonCmdHelp().setDesc("Get all entity HTTP connections from CSL-Scan")
                    .setResult("The list of entity HTTP connections, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    GET_ALL_ENTITY_HTTP_CONNECTIONS_FULL("get_entity_http_connections_full",
            new JsonCmdHelp().setDesc("Get all entity HTTP connections from CSL-Scan, also showing non-visible stages")
                    .setResult("The list of entity HTTP connections, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    GET_ENTITY_HTTP_CONNECTION("get_entity_http_connection",
            new JsonCmdHelp().setDesc("Get a specific entity HTTP connection from CSL-Scan")
                    .setParam("uuid", "The uuid of the entity HTTP connection to retrieve", IJsonCmdHelp.STR)
                    .setResult("The entity HTTP connection, in the format <code>{ \"success\": true, \"result\": { ... } }</code>", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    GET_ENTITY_HTTP_CONNECTION_FULL("get_entity_http_connection_full",
            new JsonCmdHelp().setDesc("Get a specific entity HTTP connection from CSL-Scan, also showing non-visible stages")
                    .setParam("uuid", "The uuid of the entity HTTP connection to retrieve", IJsonCmdHelp.STR)
                    .setResult("The entity HTTP connection, in the format <code>{ \"success\": true, \"result\": { ... } }</code>", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    DELETE_ENTITY_HTTP_CONNECTION("delete_entity_http_connection",
            new JsonCmdHelp().setDesc("Delete an EntityHttpConnection from CSL-Scan")
                    .setParam("uuid", "The uuid of the EntityHttpConnection to delete", IJsonCmdHelp.STR)
                    .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                            "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    ADD_ENTITY_HTTP_CONNECTION("add_entity_http_connection",
            new JsonCmdHelp().setDesc("Add an EntityHttpConnection to CSL-Scan")
                    .setParam("entity_http_connection", "The EntityHttpConnection to add", IJsonCmdHelp.JSON)
                    .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                            "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    TEST_CONNECTION("test_connection",
            new JsonCmdHelp().setDesc("Test if an existing connection is valid")
                    .setParam("device_uuid", "The uuid of the device to test the connection on", IJsonCmdHelp.STR)
                    .setParam("connection_id", "The id of the connection to test", IJsonCmdHelp.STR)
                    .setResult("<code>{ \"success\": true, \"result\": { \"value\": \"true/false\" }</code> if the operation went without error, " +
                            "where result contains \"true\" (as a String) if the connection is valid," +
                            "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    TEST_NEW_CONNECTION("test_new_connection",
            new JsonCmdHelp().setDesc("Test if a new connection is valid")
                    .setParam("ip_address", "The IP address to test the connection on", IJsonCmdHelp.STR)
                    .setParam("connection", "The connection to test", IJsonCmdHelp.JSON)
                    .setParam("base_connection_id", "The id of the base connection to fetch the password from", IJsonCmdHelp.INT)
                    .setResult("<code>{ \"success\": true, \"result\": { \"value\": \"true/false\" }</code> if the operation went without error, " +
                            "where result contains \"true\" (as a String) if the connection is valid," +
                            "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    FETCH_HTTP_CONNECTION_STAGE("fetch_http_connection_stage",
            new JsonCmdHelp().setDesc("Try to fetch the contents of a stage in the Http Connection API")
                    .setParam("stage", "The stage to fetch", IJsonCmdHelp.JSON)
                    .setParam("ip_address", "The IP address to test", IJsonCmdHelp.STR)
                    .setParam("port", "The port to test", IJsonCmdHelp.INT)
                    .setParam("username", "The username to test. Optional.", IJsonCmdHelp.STR)
                    .setParam("password", "The password to test. Optional.", IJsonCmdHelp.STR)
                    .setResult("<code>{ \"success\": true, \"result\": { \"value\": { \"page\": \"...\", \"status\": int }</code> if the operation went without error, " +
                            "where result contains \"true\" (as a String) if the connection is valid," +
                            "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)),
    GET_PREDEFINED_HTTP_VARIABLES("get_predefined_http_variables",
            new JsonCmdHelp().setDesc("Get the list of predefined HTTP variables")
                    .setResult("The list of predefined HTTP variables, in the format <code>{ \"success\": true, \"result\": [...] }</code>", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    TEST_HTTP_TEMPLATE("test_http_template",
            new JsonCmdHelp().setDesc("Test an HTTP template")
                    .setParam("ip_address", "The IP address to test the connection on - optional", IJsonCmdHelp.STR)
                    .setParam("device_uuid", "The uuid of the device to test the connection on - optional", IJsonCmdHelp.STR)
                    .setParam("connection_id", "The id of the connection to test - optional", IJsonCmdHelp.INT)
                    .setParam("connection", "The connection to test - optional", IJsonCmdHelp.JSON)
                    .setParam("template_uuid", "The uuid of the template to test", IJsonCmdHelp.STR)
                    .setParam("template", "The template to test - optional", IJsonCmdHelp.JSON)
                    .setResult("<code>{ \"success\": true, \"result\": { \"success\": \"true/false\" }</code> if the operation went without error, " +
                            "where result contains <code>{ \"success\": true }</code> if the template is valid," +
                            "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)),
    GET_DISCOVERY_CRON("get_discovery_cron",
            new JsonCmdHelp().setDesc("Get the discovery cron")
                    .setResult("The discovery cron, in the format <code>{ \"success\": true, \"result\": { \"cron\": \"...\" } }</code>", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    SET_DISCOVERY_CRON("set_discovery_cron",
            new JsonCmdHelp().setDesc("Set the discovery cron")
                    .setParam("cron", "The cron to set", IJsonCmdHelp.STR)
                    .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                            "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    IS_DISCOVERY_CRON_ACTIVE("is_discovery_cron_active",
            new JsonCmdHelp().setDesc("Get the status of the discovery cron")
                    .setResult("The status of the discovery cron, in the format <code>{ \"success\": true, \"result\": { \"active\": \"true/false\" } }</code>", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)),
    SET_DISCOVERY_CRON_ACTIVE("set_discovery_cron_active",
            new JsonCmdHelp().setDesc("Set the status of the discovery cron")
                    .setParam("isActive", "The status to set", IJsonCmdHelp.BOOL)
                    .setResult("<code>{ \"success\": true }</code> if the operation went without error," +
                            "<code>{ \"success\": false, \"error\": {\"reason\": \"...\", \"details\": \"...\"} }</code> otherwise.", IJsonCmdHelp.JSON)
                    .setStatus(IJsonCmdHelp.STATUS_OK)
    );

    private final String command;
    private final JsonCmdHelp help;

    /**
     * Constructor for the endpoints of Discovery service
     *
     * @param command command of the request
     * @param help    help of the command for the api help
     */
    DiscoveryEndpoints(String command, IJsonCmdHelp help) {
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
