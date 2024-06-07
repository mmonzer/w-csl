package main.services.endpoints;

import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.ucsl.interfaces.IJsonCmdHelp;

public enum AutoCryptEndpoints implements Endpoint  {
    SET_IP("set_ip",
            new JsonCmdHelp()
                    .setDesc("Changes the ip to connect the AutoCrypt module")
                    .setParam("ip", "New ip", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_IP("get_ip",
            new JsonCmdHelp()
                    .setDesc("Gets the ip to connect the AutoCrypt module")
                    .setResult("ip",  JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    SET_PORT("set_port",
            new JsonCmdHelp()
                    .setDesc("Changes the port to connect the AutoCrypt module")
                    .setParam("port", "New port", JsonCmdHelp.INT)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_PORT("get_port",
            new JsonCmdHelp()
                    .setDesc("Gets the port to connect the AutoCrypt module")
                    .setResult("port",  JsonCmdHelp.INT)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_ISSUER_INFO("get_issuer_info",
            new JsonCmdHelp()
                    .setDesc("Gives the information of a given issuer")
                    .setParam("path", "", JsonCmdHelp.STR)
                    .setParam("issuer_id", "", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    UPDATE_ISSUER_INFO("update_issuer_info",
            new JsonCmdHelp()
                    .setDesc("Updates the information of a given issuer")
                    .setParam("path", "", JsonCmdHelp.STR)
                    .setParam("issuer_id", "", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    DELETE_ISSUER_INFO("delete_issuer_info",
            new JsonCmdHelp()
                    .setDesc("Deletes the given issuer")
                    .setParam("path", "", JsonCmdHelp.STR)
                    .setParam("issuer_id", "", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    IMPORT_CERTIFICATE("import_issuer",
            new JsonCmdHelp()
                    .setDesc("Imports a new issuer, usually a certificate")
                    .setParam("path", "", JsonCmdHelp.STR)
                    .setParam("file", "new certificate", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_ISSUERS("get_issuers",
            new JsonCmdHelp()
                    .setDesc("Gets the list of issuers")
                    .setParam("path", "OPT", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_ROLES("get_roles",
            new JsonCmdHelp()
                    .setDesc("Gets the list of roles")
                    .setParam("path", "OPT", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    CREATE_ROLE("create_role",
            new JsonCmdHelp()
                    .setDesc("Creates a new role")
                    .setParam("path", "", JsonCmdHelp.STR)
                    .setParam("name", "Name of the new role", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_ROLE("get_role",
            new JsonCmdHelp()
                    .setDesc("Get the information of the given role")
                    .setParam("path", "", JsonCmdHelp.STR)
                    .setParam("name", "Name of the role", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    DELETE_ROLE("delete_role",
            new JsonCmdHelp()
                    .setDesc("Delete the given role")
                    .setParam("path", "", JsonCmdHelp.STR)
                    .setParam("name", "Name of the role", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    UPDATE_ROLE("update_role",
            new JsonCmdHelp()
                    .setDesc("Updates the information of the given role")
                    .setParam("path", "", JsonCmdHelp.STR)
                    .setParam("name", "Name of the role", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    ACTIVATE_OCSP("activate_ocsp",
            new JsonCmdHelp()
                    .setDesc("Activates OCSP")
                    .setParam("path", "", JsonCmdHelp.STR)
                    .setParam("ocsp-servers", "", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    IS_ALIVE("is_alive",
            new JsonCmdHelp()
                    .setDesc("Check if AutoCrypt api is reachable")
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GENERATE_CERTIFICATE("generate_certificate",
            new JsonCmdHelp()
                    .setDesc("Generates a certificate")
                    .setParam("path", "", JsonCmdHelp.STR)
                    .setParam("role_name", "", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_CERTIFICATES("get_certificates",
            new JsonCmdHelp()
                    .setDesc("Gives the list of certificates")
                    .setParam("path", "OPT", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_CERTIFICATE_INFO("get_certificate_info",
            new JsonCmdHelp()
                    .setDesc("Gives the list of certificates")
                    .setParam("serialNumber", "Number of certificate", JsonCmdHelp.STR)
                    .setParam("path", "", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    REVOKE_CERTIFICATE("revoke_certificate",
            new JsonCmdHelp()
                    .setDesc("Revoke the certificate")
                    .setParam("path", "", JsonCmdHelp.STR)
                    .setParam("serialNumber", "Serial number of the certificate", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GENERATE_ROOT_CA("generate_root_ca",
            new JsonCmdHelp()
                    .setDesc("Generates a root for CA")
                    .setParam("common_name", "", JsonCmdHelp.STR)
                    .setParam("ttl", "", JsonCmdHelp.STR)
                    .setParam("path", "OPT: ", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GENERATE_INTERMEDIATE_CA("generate_inter_ca",
            new JsonCmdHelp()
                    .setDesc("Generate intermediate ca")
                    .setParam("path", "", JsonCmdHelp.STR)
                    .setParam("common_name", "", JsonCmdHelp.STR)
                    .setParam("ttl", "", JsonCmdHelp.STR)
                    .setParam("type", "", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK));

    private final String command;
    private final JsonCmdHelp help;

    public static final String ISSUER_URI = "/api/issuer";
    public static final String ISSUER_URI_ = ISSUER_URI+"/";
    public static final String ISSUER_URI_IMPORT = ISSUER_URI_+"import";

    public static final String ROLE_URI = "/api/role";
    public static final String ROLE_URI_ = ROLE_URI+"/";

    public static final String MISC_URI = "/api/general";
    public static final String MISC_URI_ACTIVATE_OCSP = MISC_URI+"/activate-ocsp";
    public static final String MISC_URI_IS_ALIVE = MISC_URI+"/health-check";

    public static final String CERT_URI = "/api/certificate";
    public static final String CERT_URI_ = CERT_URI+"/";
    public static final String CERT_URI_ISSUE = CERT_URI_+"issue";
    public static final String CERT_URI_REVOKE_ = CERT_URI_+"revoke/";

    public static final String CA_URI = "/api/ca";
    public static final String CA_URI_GENERATE_INTER = CA_URI+"/generate-intermediate";
    public static final String CA_URI_GENERATE_ROOT = CA_URI+"/generate-root";

    /**
     * Constructor for the endpoints of Monitor service
     *
     * @param command command of the request
     * @param help    help of the command for the api help
     */
    AutoCryptEndpoints(String command, IJsonCmdHelp help) {
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
