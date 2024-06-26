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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * OCSP : Online Certificate Status Protocol
 * Role :
 * Path :
 * Issuer :
 */
public class AutoCryptService extends Service implements IStatusProvider {
    private final AutoCrypt manager;
    private ScheduledExecutorService synchronizationSchedule;
    private boolean isRemote = false;
    private static final Logger logger = LoggerFactory.getLogger(AutoCryptService.class);

    /**
     * Default constructor of the AutoCrypt service (not remote)
     */
    public AutoCryptService() {
        this(false);
    }

    /**
     * Default constructor of the AutoCrypt service.
     */
    public AutoCryptService(boolean isRemote) {
        this("autocrypt", "Service for managing the different certificates", "auto_crypt", isRemote);
    }

    /**
     * Generic constructor of the AutoCrypt service.
     */
    public AutoCryptService(String name, String description, String configFileSectionName, boolean isRemote) {
        super(name, description, configFileSectionName);
        this.isRemote = isRemote;
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
        if (!isRemote) {
            manager.reinitApiHandler();
        }

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
        addCmd(AutoCryptEndpoints.VALIDATE_TEMPLATE_CERTIFICATE, this::validateTemplate);
        addCmd(AutoCryptEndpoints.GENERATE_CERTIFICATE, this::generateCertificate);
        addCmd(AutoCryptEndpoints.GET_CERTIFICATES, this::getCertificates);
        addCmd(AutoCryptEndpoints.GET_CERTIFICATE_INFO, this::getCertificateInfo);
        addCmd(AutoCryptEndpoints.DOWNLOAD_CERTIFICATE, this::downloadCertificate);
        addCmd(AutoCryptEndpoints.REVOKE_CERTIFICATE, this::revokeCertificate);
        // ca-controller
        addCmd(AutoCryptEndpoints.GENERATE_ROOT_CA, this::generateRootCA);
        addCmd(AutoCryptEndpoints.GENERATE_INTERMEDIATE_CA, this::generateIntermediateCA);

        // Launch initial sync to dbapi
        synchronizationSchedule = Executors.newScheduledThreadPool(1);
        // TODO : change initial to continuous sync
        synchronizationSchedule.scheduleAtFixedRate(()->{manager.getMethods().initialSynchronizeDb("pki");}, 0, 300, TimeUnit.SECONDS);

        logger.info("Service autocrypt initilialized.");
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
        // Dbapi id, name,descrip
        if (!body.has("name") || !body.get("name").isString()) {
            return errorVariableNotFound("name");
        }
        String name = body.get("name").asString();
        body.delAt("name");
        String description = null;
        if (body.has("description") && body.get("description").isString()) {
            description = body.get("description").asString();
            body.delAt("description");
        }
        if (!body.has("id") || !body.get("id").isNumber()) {
            return errorVariableNotFound("id");
        }
        int id = body.get("id").asInteger();
        body.delAt("id");
        Json params = Json.object();
        // Check params
//        if (!body.has("path") || !body.get("path").isString()) {
//            return errorVariableNotFound("path");
//        }
//        params.at("path", body.get("path").asString());
//        body.delAt("path");

        params.at("path", name);
        if ((!body.has("issuer_ref") || !body.get("issuer_ref").isString()) && (!body.has("issuer_id") || !body.get("issuer_id").isString())) {
            return errorVariableNotFound("issuer_ref/issuer_id");
        }
        String issuerRef;
        if (body.has("issuer_ref")) {
            issuerRef = body.get("issuer_ref").asString();
            body.delAt("issuer_ref");
        } else {issuerRef = body.get("issuer_id").asString();
            body.delAt("issuer_id");
        }


        return manager.getMethods().updateIssuerInfo(id, name, description, issuerRef, body, params).toJson();
    }

    /**
     * Deletes the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json deleteIssuer(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("id") || !body.get("id").isNumber()) {
            return errorVariableNotFound("id");
        }
        int id = body.get("id").asInteger();
        if (!body.has("name") || !body.get("name").isString()) {
            return errorVariableNotFound("name");
        }
        String name =  body.get("name").asString();
        params.at("path", body.get("name").asString());
        if (!body.has("issuer_ref") || !body.get("issuer_ref").isString()) {
            return errorVariableNotFound("issuer_ref");
        }
        String issuerRef = body.get("issuer_ref").asString();
        body.delAt("issuer_ref");

        return manager.getMethods().deleteIssuer(id, name, issuerRef, body, params).toJson();
    }

    /**
     * Imports a new certificate
     *
     * @param body parameters with the path and the file
     */
    public Json importCertificate(Json body) {
        // Dbapi id
        if (!body.has("name") || !body.get("name").isString()) {
            return errorVariableNotFound("name");
        }
        String name = body.get("name").asString();
        body.delAt("name");
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

        return manager.getMethods().importCertificate(name, body, params).toJson();
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
        String description = null;
        if (body.has("description") && body.get("description").isString()) {
            description = body.get("description").asString();
            body.delAt("description");
        }
        if (!body.has("certificate_authority_id") || !body.get("certificate_authority_id").isNumber()) {
            return errorVariableNotFound("certificate_authority_id");
        }
        Integer certificateAuthorityId = body.get("certificate_authority_id").asInteger();
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
        String name = body.get("name").asString();

        return manager.getMethods().createRole(name, description, certificateAuthorityId.toString(), body, params).toJson();
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
        if (!body.has("certificate_authority_id") || !body.get("certificate_authority_id").isNumber()) {
            return errorVariableNotFound("certificate_authority_id");
        }
        Integer certificateAuthorityId = body.get("certificate_authority_id").asInteger();
        String description = null;
        if (body.has("description") && body.get("description").isString()) {
            description = body.get("description").asString();
            body.delAt("description");
        }
        Json params = Json.object();
        // Check params
        if (!body.has("name") || !body.get("name").isString()) {
            return errorVariableNotFound("name");
        }
        String name = body.get("name").asString();
        body.delAt("name");
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");

        return manager.getMethods().updateRole(id, name, description, certificateAuthorityId.toString(), body, params).toJson();
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

        return manager.getMethods().activateOCSP(body, params).toJson();
    }

    /**
     * Validates the template of a certificate
     *
     * @param body parameters with the path, name and issuer_ref
     */
    public Json validateTemplate(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        // Check body
        if (!body.has("issuer_ref") || !body.get("issuer_ref").isString()) {
            return errorVariableNotFound("issuer_ref");
        }
        if (!body.has("name") || !body.get("name").isString()) {
            return errorVariableNotFound("name");
        }

        return manager.getMethods().validateTemplate(body, params).toJson();
    }

    /**
     * Generates a certificates at the given path and role
     *
     * @param body parameters with the path and role
     */
    public Json generateCertificate(Json body) {
        // dbapi name,description
        if (!body.has("name") || !body.get("name").isString()) {
            return errorVariableNotFound("name");
        }
        String name = body.get("name").asString();
        body.delAt("name");
        String description = null;
        if (body.has("description") && body.get("description").isString()) {
            description = body.get("description").asString();
            body.delAt("description");
        }
        if (!body.has("vault_role_id") || !body.get("vault_role_id").isNumber()) {
            return errorVariableNotFound("vault_role_id");
        }
        String vaultRoleId = body.get("vault_role_id").asString();
        body.delAt("vault_role_id");
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        // Check body
        if (!body.has("vault_role_name") || !body.get("vault_role_name").isString()) {
            return errorVariableNotFound("vault_role_name");
        }
        body.set("role_name", body.get("vault_role_name").asString());
        body.delAt("vault_role_name");
        if (!body.has("common_name") || !body.get("common_name").isString()) {
            return errorVariableNotFound("common_name");
        }
        if (!body.has("ttl") || !body.get("ttl").isString()) {
            return errorVariableNotFound("ttl");
        }

        return manager.getMethods().generateCertificate(name, description, vaultRoleId, body, params).toJson();
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
     * Downloads the certificate
     *
     * @param body parameters with the serialNumber
     */
    public Json downloadCertificate(Json body) {
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

        return manager.getMethods().downloadCertificate(serialNumber, params).getResult();
    }

    /**
     * Revokes the given certificate
     *
     * @param body parameters with the path
     */
    public Json revokeCertificate(Json body) {
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

        return manager.getMethods().revokeCertificate(serialNumber, params).toJson();
    }

    /***
     * Checks if a key exists in a json if its value is a String
     * @param obj the json object to check
     * @param key the key inside the json obj
     * @param throwException whether to throw an exception or not
     */
    private boolean propertyExistsAndIsString(Json obj, String key) {
        if (obj.has(key) && obj.get(key).isString()) {
            return true;
        }
        return false;
    }

    /**
     * Generate root CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public Json generateRootCA(Json body) {
        // dbapi identifier
//        if (!body.has("name") || !body.get("name").isString()) {
//            return errorVariableNotFound("name");
//        }
//        String name = body.get("name").asString();
        // region -- Verify required body keys
        if (!this.propertyExistsAndIsString(body, "common_name")) {
            return errorVariableNotFound("common_name");
        }

        if (!this.propertyExistsAndIsString(body, "ttl")) {
            return errorVariableNotFound("ttl");
        }
        // endregion -- Verify required body keys
        String name = body.get("common_name").asString();
        String description = null;

        if (this.propertyExistsAndIsString(body, "description")) {
            description = body.get("description").asString();
            body.delAt("description");
        }

        return manager.getMethods().generateRootCA(name, description, body, null).toJson();
    }

    /**
     * Generate intermediate CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public Json generateIntermediateCA(Json body) {
        String description = null;
        if (body.has("description") && body.get("description").isString()) {
            description = body.get("description").asString();
            body.delAt("description");
        }
        Json params = Json.object();
        // check params
        // check body
        if (!body.has("type") || !body.get("type").isString()) {
            return errorVariableNotFound("type");
        }
        if (!body.has("common_name") || !body.get("common_name").isString()) {
            return errorVariableNotFound("common_name");
        }
        String name = body.get("common_name").asString();
        params.at("path", name);
        if (!body.has("ttl") || !body.get("ttl").isString()) {
            return errorVariableNotFound("ttl");
        }

        return manager.getMethods().generateIntermediateCA(name, description, body, params).toJson();
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
