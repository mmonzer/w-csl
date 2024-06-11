package main.services;

import com.csl.autocrypt.AutoCrypt;
import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.csl.intercom.status.IStatusProvider;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.endpoints.AutoCryptEndpoints;

/**
 * OCSP : Online Certificate Status Protocol
 * Role :
 * Path :
 * Issuer :
 */
public class AutoCryptService extends Service implements IStatusProvider {
    private final AutoCrypt manager;

    /**
     * Default constructor of the Suricata service.
     */
    public AutoCryptService() {
        this("autocrypt",
                "Service for managing the different certificates",
                "auto_crypt");
    }

    /**
     * Generic constructor of the Suricata service.
     */
    public AutoCryptService(String name, String description, String configFileSectionName) {
        super(name, description, configFileSectionName);
        manager = new AutoCrypt(name);
    }

    /**
     * Initialization of the TAPs commands
     *
     * @param config     the configuration section of the configuration file
     * @param configFile the CSL directory
     * @return true if the initialization happened with no problems, false otherwise.
     */
    @Override
    public boolean init(Json config, String configFile) {
        manager.setModuleIp(config.at("ip").asString());
        manager.setModulePort(config.at("port").asInteger());
        Json globalConfig = config.get("global");
        manager.configureDbApiConnection(JsonUtil.getStringFromJson(globalConfig, "ip_server_remote", "localhost"),
                JsonUtil.getStringFromJson(globalConfig, "api_key", ""));
        manager.reinitApiHandler();

        CSLContext.instance.getStatusNotifier().registerStatusProvider(name, this);

//        // TODO: needs to add persistence of changes
//        // Connexion
//        addCmd(AutoCryptEndpoints.GET_IP, this::getIp);
//        addCmd(AutoCryptEndpoints.SET_IP, this::changeIp);
//        addCmd(AutoCryptEndpoints.GET_PORT, this::getPort);
//        addCmd(AutoCryptEndpoints.SET_PORT, this::changePort);
        // issuer-controller
        addCmd(AutoCryptEndpoints.GET_ISSUERS, this::getIssuers);
        addCmd(AutoCryptEndpoints.GET_ISSUER_INFO, this::getIssuerInfo);
        addCmd(AutoCryptEndpoints.UPDATE_ISSUER_INFO, this::updateIssuerInfo);
        addCmd(AutoCryptEndpoints.DELETE_ISSUER_INFO, this::deleteIssuer);
        addCmd(AutoCryptEndpoints.IMPORT_CERTIFICATE, this::importCertificate);
        // role-controller
        addCmd(AutoCryptEndpoints.GET_ROLES, this::getRoles);
        addCmd(AutoCryptEndpoints.CREATE_ROLE, this::createRole);
        addCmd(AutoCryptEndpoints.GET_ROLE, this::getRole);
        addCmd(AutoCryptEndpoints.DELETE_ROLE, this::deleteRole);
        addCmd(AutoCryptEndpoints.UPDATE_ROLE, this::updateRole);
        // miscellaneous-controller
        addCmd(AutoCryptEndpoints.ACTIVATE_OCSP, this::activateOCSP);
        addCmd(AutoCryptEndpoints.IS_ALIVE, this::getStatus);
        // certificate-controller
        addCmd(AutoCryptEndpoints.GENERATE_CERTIFICATE, this::generateCertificate);
        addCmd(AutoCryptEndpoints.GET_CERTIFICATES, this::getCertificates);
        addCmd(AutoCryptEndpoints.GET_CERTIFICATE_INFO, this::getCertificateInfo);
        addCmd(AutoCryptEndpoints.REVOKE_CERTIFICATE, this::revokeCertificate);
        // ca-controller
        addCmd(AutoCryptEndpoints.GENERATE_ROOT_CA, this::generateRootCA);
        addCmd(AutoCryptEndpoints.GENERATE_INTERMEDIATE_CA, this::generateIntermediateCA);

        return true;
    }

    /**
     * Method that changes the ip to connect the autocrypt module
     *
     * @param body parameters with the ip
     */
    public Json changeIp(Json body) {
        if (!body.has("ip") || !body.get("ip").isString()) {
            return errorVariableNotFound("ip");
        }

        manager.setModuleIp(body.get("ip").asString());
        manager.reinitApiHandler();

        return JsonApiResponse.success().toJson();
    }

    /**
     * Gets the ip to connect the autocrypt module
     *
     * @param body parameters with the ip
     */
    public Json getIp(Json body) {
        Json response = Json.object();
        response.at("ip", manager.getModuleIp());
        return JsonApiResponse.result(response).toJson();
    }

    /**
     * Method that changes the port to connect the autocrypt module
     *
     * @param body parameters with the port
     */
    public Json changePort(Json body) {
        if (!body.has("port") || !body.get("port").isNumber()) {
            return errorVariableNotFound("port");
        }

        manager.setModulePort(body.get("port").asInteger());
        manager.reinitApiHandler();

        return JsonApiResponse.success().toJson();
    }

    /**
     * Gets the port to connect the autocrypt module
     *
     * @param body parameters with the port
     */
    public Json getPort(Json body) {
        Json response = Json.object();
        response.at("port", manager.getModulePort());
        return JsonApiResponse.result(response).toJson();
    }

    /**
     * Method that gets the list of issuers
     *
     * @param body parameters with the path
     */
    public Json getIssuers(Json body) {
        return manager.getMethods().getIssuers(body).toJson();
    }

    /**
     * Method that recovers the information of the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json getIssuerInfo(Json body) {
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        if (!body.has("issuer_ref") || !body.get("issuer_ref").isString()) {
            return errorVariableNotFound("issuer_ref");
        }
        String issuerRef = body.get("issuer_ref").asString();
        body.delAt("issuer_ref");

        return manager.getMethods().getIssuerInfo(issuerRef, body).toJson();
    }

    /**
     * Updates the information of the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json updateIssuerInfo(Json body) {
        // Dbapi id
        if (!body.has("id") || !body.get("id").isNumber()) {
            return errorVariableNotFound("id");
        }
        int id = body.get("id").asInteger();
        body.delAt("id");
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path").asString());
        body.delAt("path");
        if (!body.has("issuer_ref") || !body.get("issuer_ref").isString()) {
            return errorVariableNotFound("issuer_ref");
        }
        String issuerRef = body.get("issuer_ref").asString();
        body.delAt("issuer_ref");

        return manager.getMethods().updateIssuerInfo(id, issuerRef, body, params).toJson();
    }

    /**
     * Deletes the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json deleteIssuer(Json body) {
        // Dbapi id
        if (!body.has("id") || !body.get("id").isNumber()) {
            return errorVariableNotFound("id");
        }
        int id = body.get("id").asInteger();
        body.delAt("id");
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path").asString());
        body.delAt("path");
        if (!body.has("issuer_ref") || !body.get("issuer_ref").isString()) {
            return errorVariableNotFound("issuer_ref");
        }
        String issuerRef = body.get("issuer_ref").asString();
        body.delAt("issuer_ref");

        return manager.getMethods().deleteIssuer(id,
                issuerRef,
                body,
                params
        ).toJson();
    }

    /**
     * Imports a new certificate
     *
     * @param body parameters with the path and the file
     */
    public Json importCertificate(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        // Check file (body)
        if (!body.has("file") || !body.get("file").isString()) {
            return JsonApiResponse.error("File was not correctly uploaded").toJson();
        }

        return manager.getMethods().importCertificate(body, params).toJson();
    }

    /**
     * Gets the list of roles
     *
     * @param body parameters with the path
     */
    public Json getRoles(Json body) {
        return manager.getMethods().getRoles(body).toJson();
    }

    /**
     * Creates a new role
     *
     * @param body parameters with the path
     */
    public Json createRole(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        // Check body
        if (!body.has("name") || !body.get("name").isString()) {
            return errorVariableNotFound("name");
        }

        return manager.getMethods().createRole(
                body,
                params
        ).toJson();
    }

    /**
     * Gets the information of the given role
     *
     * @param body parameters with the path and name of role
     */
    public Json getRole(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path").asString());
        if (!body.has("name") || !body.get("name").isString()) {
            return errorVariableNotFound("name");
        }
        String name = body.get("name").asString();
        body.delAt("name");

        return manager.getMethods().getRole(name, params).toJson();
    }

    /**
     * Deletes the given role
     *
     * @param body parameters with the path and name of role
     */
    public Json deleteRole(Json body) {
        // Dbapi id
        if (!body.has("id") || !body.get("id").isNumber()) {
            return errorVariableNotFound("id");
        }
        int id = body.get("id").asInteger();
        body.delAt("id");
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        if (!body.has("name") || !body.get("name").isString()) {
            return errorVariableNotFound("name");
        }
        String name = body.get("name").asString();
        body.delAt("name");

        return manager.getMethods().deleteRole(id, name, body, params).toJson();
    }

    /**
     * Updates the information of the given role
     *
     * @param body parameters with the path and name of role, others?
     */
    public Json updateRole(Json body) {
        // Dbapi id
        if (!body.has("id") || !body.get("id").isNumber()) {
            return errorVariableNotFound("id");
        }
        int id = body.get("id").asInteger();
        body.delAt("id");
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        if (!body.has("name") || !body.get("name").isString()) {
            return errorVariableNotFound("name");
        }
        String name = body.get("name").asString();
        body.delAt("name");

        return manager.getMethods().updateRole(id, name, body, params).toJson();
    }

    /**
     * Activates the Online Certificate Status Protocol (OCSP)
     *
     * @param body parameters with the path and name of role, others?
     */
    public Json activateOCSP(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        if (!body.has("ocsp_servers") || !body.get("ocsp_servers").isString()) {
            return errorVariableNotFound("ocsp_servers");
        }
        params.at("ocsp_servers", body.get("ocsp_servers"));
        body.delAt("ocsp_servers");

        return manager.getMethods().activateOCSP(
                body,
                params
        ).toJson();
    }

    /**
     * Generates a certificates at the given path and role
     *
     * @param body parameters with the path and role
     */
    public Json generateCertificate(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        // Check body
        if (!body.has("role_name") || !body.get("role_name").isString()) {
            return errorVariableNotFound("role_name");
        }

        return manager.getMethods().generateCertificate(
                body,
                params
        ).toJson();
    }

    /**
     * Gives the list of certificates
     *
     * @param body parameters with the path
     */
    public Json getCertificates(Json body) {
        Json params = Json.object();
        // Check params
        if (body.has("path") && body.get("path").isString()) {
            params.at("path", body.get("path").asString());
            body.delAt("path");
        }

        return manager.getMethods().getCertificates(params).toJson();
    }

    /**
     * Gives the information of the given certificate
     *
     * @param body parameters with the serialNumber
     */
    public Json getCertificateInfo(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path").asString());
        body.delAt("path");
        if (!body.has("serial_number") || !body.get("serial_number").isString()) {
            return errorVariableNotFound("serial_number");
        }
        String serialNumber = body.get("serial_number").asString();
        body.delAt("serial_number");

        return manager.getMethods().getCertificateInfo(serialNumber, params).toJson();
    }

    /**
     * Revokes the given certificate
     *
     * @param body parameters with the path
     */
    public Json revokeCertificate(Json body) {
        // Dbapi id
        if (!body.has("id") || !body.get("id").isNumber()) {
            return errorVariableNotFound("id");
        }
        int id = body.get("id").asInteger();
        body.delAt("id");
        // check params
        Json params = Json.object();
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        if (!body.has("serial_number") || !body.get("serial_number").isString()) {
            return errorVariableNotFound("serial_number");
        }
        String serialNumber = body.get("serial_number").asString();
        body.delAt("serial_number");

        return manager.getMethods().revokeCertificate(id, serialNumber, params).toJson();
    }

    /**
     * Generate root CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public Json generateRootCA(Json body) {
        // check params
        Json params = Json.object();
        if (!body.has("common_name") || !body.get("common_name").isString()) {
            return errorVariableNotFound("common_name");
        }
        params.at("common_name", body.get("common_name"));
        body.delAt("common_name");
        if (!body.has("ttl") || !body.get("ttl").isString()) {
            return errorVariableNotFound("ttl");
        }
        params.at("ttl", body.get("ttl"));
        body.delAt("ttl");

        if (body.has("path") && body.get("path").isString()) {
            params.at("path", body.get("path"));
            body.delAt("path");
        }

        return manager.getMethods().generateRootCA(
                body,
                params
        ).toJson();
    }

    /**
     * Generate intermediate CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public Json generateIntermediateCA(Json body) {
        Json params = Json.object();
        // check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        // check body
        if (!body.has("type") || !body.get("type").isString()) {
            return errorVariableNotFound("type");
        }
        if (!body.has("common_name") || !body.get("common_name").isString()) {
            return errorVariableNotFound("common_name");
        }
        if (!body.has("ttl") || !body.get("ttl").isString()) {
            return errorVariableNotFound("ttl");
        }

        return manager.getMethods().generateIntermediateCA(body, params).toJson();
    }

    /**
     * Verifies if the module api is reachable
     *
     * @return whether it is reachable
     */
    @Override
    public Json getStatus() {
        return manager.getMethods().getStatus();
    }

    /**
     * Verifies if the module api is reachable
     *
     * @param body default body of the POST request
     * @return whether it is reachable
     */
    public Json getStatus(Json body) {
        return JsonApiResponse.result(getStatus()).toJson();
    }

    /**
     * Returns the manager of the module
     *
     * @return manager of the module
     */
    public AutoCrypt getManager() {
        return manager;
    }
}
