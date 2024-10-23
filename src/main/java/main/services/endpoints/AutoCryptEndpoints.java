package main.services.endpoints;

import com.csl.autocrypt.enums.AutocryptConstants;
import com.csl.intercom.jsoncmd.JsonCmdHelp;

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
                    .setParam(AutocryptConstants.Common.PATH, "", JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Issuer.ISSUER_ID, "", JsonCmdHelp.STR)
                    .setResult("Information of the issuer sent by CSL_Autocrypt", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    UPDATE_ISSUER_INFO("update_issuer_info",
            new JsonCmdHelp()
                    .setDesc("Updates the information of a given issuer")
                    .setParam("id", Constants.ID_FROM_DBAPI, JsonCmdHelp.INT)
                    .setParam(AutocryptConstants.Common.NAME, Constants.NAME_IN_THE_DBAPI, JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.PATH, "", JsonCmdHelp.STR)
                    .setParam("issuer_id", "", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    DELETE_ISSUER_INFO("delete_issuer_info",
            new JsonCmdHelp()
                    .setDesc("Deletes the given issuer")
                    .setParam("id", Constants.ID_FROM_DBAPI, JsonCmdHelp.INT)
                    .setParam(AutocryptConstants.Common.NAME, Constants.NAME_IN_THE_DBAPI, JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.PATH, "", JsonCmdHelp.STR)
                    .setParam("issuer_id", "", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    IMPORT_ISSUER_INTERMEDIATE("import_issuer_intermediate",
            new JsonCmdHelp()
                    .setDesc("Imports a new issuer, usually a certificate")
                    .setParam(AutocryptConstants.Common.NAME, Constants.NAME_IN_THE_DBAPI, JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.PATH, "", JsonCmdHelp.STR)
                    .setParam("file", "new certificate", JsonCmdHelp.STR)
                    .setResult("the information about the new certificate", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    IMPORT_ISSUER_ROOT("import_issuer_root",
            new JsonCmdHelp()
                    .setDesc("Imports a new issuer, usually a certificate")
                    .setParam(AutocryptConstants.Common.NAME, Constants.NAME_IN_THE_DBAPI, JsonCmdHelp.STR)
                    .setParam("file", "new certificate", JsonCmdHelp.STR)
                    .setResult("the information about the new certificate", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    EXPORT_ISSUER("export_issuer",
            new JsonCmdHelp()
                    .setDesc("Exports the given issuer at the given path")
                    .setParam(AutocryptConstants.Common.PATH, "path of the issuer", JsonCmdHelp.STR)
                    .setParam("issuer_ref", "identifier of the issuer", JsonCmdHelp.STR)
                    .setResult("the certificate of the given issuer", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_ISSUERS("get_issuers",
            new JsonCmdHelp()
                    .setDesc("Gets the list of issuers")
                    .setParam(AutocryptConstants.Common.PATH, "OPT", JsonCmdHelp.STR)
                    .setResult("Lis of the issuers of CSL_Autocrypt", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_ROLES("get_roles",
            new JsonCmdHelp()
                    .setDesc("Gets the list of roles")
                    .setParam(AutocryptConstants.Common.PATH, "OPT", JsonCmdHelp.STR)
                    .setResult("Lis of the issuers of CSL_Autocrypt", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    CREATE_ROLE("create_role",
            new JsonCmdHelp()
                    .setDesc("Creates a new role")
                    .setParam(AutocryptConstants.Common.PATH, "", JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.NAME, "Name of the new role", JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.DESCRIPTION, "OPT", JsonCmdHelp.STR)
                    .setParam("certificate_authority_id", "index of the db", JsonCmdHelp.INT)
                    .setResult("If the creation was successful, the new information of the new role. Otherwise the error", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_ROLE("get_role",
            new JsonCmdHelp()
                    .setDesc("Get the information of the given role")
                    .setParam(AutocryptConstants.Common.PATH, "", JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.NAME, Constants.NAME_OF_THE_ROLE, JsonCmdHelp.STR)
                    .setResult("Information of the role sent by CSL_Autocrypt", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    DELETE_ROLE("delete_role",
            new JsonCmdHelp()
                    .setDesc("Delete the given role")
                    .setParam("id", Constants.ID_FROM_DBAPI, JsonCmdHelp.INT)
                    .setParam(AutocryptConstants.Common.PATH, "", JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.NAME, Constants.NAME_OF_THE_ROLE, JsonCmdHelp.STR)
                    .setResult("Whether the delete was successful", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    UPDATE_ROLE("update_role",
            new JsonCmdHelp()
                    .setDesc("Updates the information of the given role")
                    .setParam("id", Constants.ID_FROM_DBAPI, JsonCmdHelp.INT)
                    .setParam(AutocryptConstants.Common.DESCRIPTION, "OPT", JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.NAME, Constants.NAME_OF_THE_ROLE, JsonCmdHelp.STR)
                    .setParam("certificate_authority_id", Constants.ID_FROM_DBAPI, JsonCmdHelp.INT)
                    .setParam(AutocryptConstants.Common.PATH, "path for the role", JsonCmdHelp.STR)
                    .setResult("If the update was successful, the new information. Otherwise the error", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    ACTIVATE_OCSP("activate_ocsp",
            new JsonCmdHelp()
                    .setDesc("Activates OCSP")
                    .setParam(AutocryptConstants.Common.PATH, "", JsonCmdHelp.STR)
                    .setParam("ocsp-servers", "", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    IS_ALIVE("is_alive",
            new JsonCmdHelp()
                    .setDesc("Check if AutoCrypt api is reachable")
                    .setResult("Whether the api is reachable or not", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    VALIDATE_TEMPLATE_CERTIFICATE("validate_template",
            new JsonCmdHelp()
                    .setDesc("Validates the template of a certificate")
                    .setParam(AutocryptConstants.Common.PATH, "", JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.NAME, "", JsonCmdHelp.STR)
                    .setParam("issuer_ref", "", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GENERATE_CERTIFICATE("generate_certificate",
            new JsonCmdHelp()
                    .setDesc("Generates a certificate")
                    .setParam(AutocryptConstants.Common.NAME, "name of cert", JsonCmdHelp.STR)
                    .setParam("vault_role_id", "id of the dbapi for the role", JsonCmdHelp.INT)
                    .setParam(AutocryptConstants.Common.TTL, "", JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.COMMON_NAME, "", JsonCmdHelp.STR)
                    .setParam("role_name", "name of the role creating the certificate", JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.PATH, "path to create the certificate", JsonCmdHelp.STR)
                    .setResult("The information of the new certificate", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_CERTIFICATES("get_certificates",
            new JsonCmdHelp()
                    .setDesc("Gives the list of certificates")
                    .setParam(AutocryptConstants.Common.PATH, "OPT", JsonCmdHelp.STR)
                    .setResult("List of the certificates managed by CSL_Autocrypt", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_CERTIFICATE_INFO("get_certificate_info",
            new JsonCmdHelp()
                    .setDesc("Gives the information of the given certificate")
                    .setParam(AutocryptConstants.Certificate.SERIAL_NUMBER, Constants.NUMBER_OF_CERTIFICATE, JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.PATH, "", JsonCmdHelp.STR)
                    .setResult("Information of the given certificates as sent by CSL_Autocrypt", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    DOWNLOAD_CERTIFICATE("download_certificate",
            new JsonCmdHelp()
                    .setDesc("Downloads the given certificate")
                    .setParam(AutocryptConstants.Certificate.SERIAL_NUMBER, Constants.NUMBER_OF_CERTIFICATE, JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.PATH, "", JsonCmdHelp.STR)
                    .setResult("Information of the given certificates as sent by CSL_Autocrypt", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GET_CERTIFICATE("get_certificate",
            new JsonCmdHelp()
                    .setDesc("Gets the given certificate, with or without the private key")
                    .setParam(AutocryptConstants.Certificate.SERIAL_NUMBER, Constants.NUMBER_OF_CERTIFICATE, JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.PATH, "", JsonCmdHelp.STR)
                    .setParam("with_private_key", "OPT: whether the private key is needed. Default is false.", JsonCmdHelp.STR)
                    .setResult("{certificate: \"...\", ???:\"...\"}", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    REVOKE_CERTIFICATE("revoke_certificate",
            new JsonCmdHelp()
                    .setDesc("Revoke the certificate")
                    .setParam(AutocryptConstants.Common.PATH, "Where the certificate is store", JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Certificate.SERIAL_NUMBER, "Serial number of the certificate", JsonCmdHelp.STR)
                    .setResult("Whether the certificates was successfully revoked", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    DEPLOY_CAMERA_CERTIFICATE("deploy_certificate",
            new JsonCmdHelp()
                    .setDesc("Deploy the certificate to the given camera")
                    .setParam("device_ip", "ip of the camera", JsonCmdHelp.STR)
                    .setParam("certificate_path", "path of the certificate", JsonCmdHelp.STR)
                    .setParam("certificate_serial_number", "serial number of the certificate", JsonCmdHelp.STR)
                    .setParam("username", "user name for login into the camera", JsonCmdHelp.STR)
                    .setParam("password", "password for login into the camera", JsonCmdHelp.STR)
                    .setParam("vendor", "vendor of the camera", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    DELETE_REVOKED_CERTIFICATES("delete_revoked_certificates",
            new JsonCmdHelp()
                    .setDesc("Delete all revoked certificate")
                    .setResult("Whether the certificates were successfully deleted", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GENERATE_ROOT_CA("generate_root_ca",
            new JsonCmdHelp()
                    .setDesc("Generates a root for CA")
//                    .setParam(AutocryptConstants.Common.NAME, "name in the dbapi", JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.COMMON_NAME, "Friendly name. Il devient name for dbapi", JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.TTL, "", JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.PATH, "OPT: ", JsonCmdHelp.STR)
                    .setResult("The information of the new CA", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK)),
    GENERATE_INTERMEDIATE_CA("generate_inter_ca",
            new JsonCmdHelp()
                    .setDesc("Generate intermediate ca")
                    .setParam(AutocryptConstants.Common.COMMON_NAME, "Friendly name. Il devient path et name for dbapi", JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.TTL, "", JsonCmdHelp.STR)
                    .setParam("type", "", JsonCmdHelp.STR)
                    .setParam(AutocryptConstants.Common.DESCRIPTION, "OPT", JsonCmdHelp.STR)
                    .setResult("The information of the new CA", JsonCmdHelp.STR)
                    .setStatus(JsonCmdHelp.STATUS_OK));

    private final String command;
    private final JsonCmdHelp help;

    /**
     * Constructor for the endpoints of Monitor service
     *
     * @param command command of the request
     * @param help    help of the command for the api help
     */
    AutoCryptEndpoints(String command, JsonCmdHelp help) {
        this.command = command;
        this.help = help;
    }

    public String cmd() {
        return command;
    }

    public JsonCmdHelp help() {
        return help;
    }

    private static class Constants {
        public static final String NAME_OF_THE_ROLE = "Name of the role";
        public static final String ID_FROM_DBAPI = "id from dbapi";
        public static final String NUMBER_OF_CERTIFICATE = "Number of certificate";
        public static final String NAME_IN_THE_DBAPI = "name in the dbapi";
    }
}
