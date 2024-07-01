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

import static com.csl.autocrypt.outils.JsonHelper.*;

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

        createEndpoints();

        // Launch initial sync to dbapi
        synchronizationSchedule = Executors.newScheduledThreadPool(1);
        // TODO : change initial to continuous sync
        synchronizationSchedule.scheduleAtFixedRate(()->{manager.getMethods().initialSynchronizeDb("pki");}, 0, 300, TimeUnit.SECONDS);

        logger.info("Service autocrypt initilialized.");
        return true;
    }

    /**
     * Creates the endpoints of this service
     */
    private void createEndpoints() {
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
        addCmd(AutoCryptEndpoints.IMPORT_ISSUER_INTERMEDIATE, this::importIssuerIntermediate);
        addCmd(AutoCryptEndpoints.IMPORT_ISSUER_ROOT, this::importIssuerRoot);
        addCmd(AutoCryptEndpoints.EXPORT_ISSUER, this::exportIssuer);
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

        // region -- Verify required body keys and extract key values

        getValueString(body, "path");
        String issuerRef = extractValueString(body, "issuer_ref");

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().getIssuerInfo(issuerRef, body).toJson();
    }

    /**
     * Updates the information of the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json updateIssuerInfo(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, "path");
        String name = extractValueString(body, "name");
        String description = getValueStringOrNull(body, "description");
        Integer id = extractValueInteger(body, "id");

        if ((!body.has("issuer_ref") || !body.get("issuer_ref").isString()) && (!body.has("issuer_id") || !body.get("issuer_id").isString())) {
            return errorVariableNotFound("issuer_ref/issuer_id");
        }
        String issuerRef = getValueStringOrNull(body, "issuer_ref");
        body.delAt("issuer_ref");
        if (issuerRef==null) {
            System.out.println("NEED CHANGE : to issuer_ref");
            issuerRef = getValueStringOrNull(body, "issuer_id");
            body.delAt("issuer_id");
        }

        // endregion -- Verify required body keys and extract key values


        return manager.getMethods().updateIssuerInfo(id, name, description, issuerRef, body, params).toJson();
    }

    /**
     * Deletes the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json deleteIssuer(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String issuerRef = extractValueString(body, "issuer_ref");
        String name = getValueString(body, "name");
        transferValueString(body, params, "path");

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().deleteIssuer(name, issuerRef, body, params).toJson();
    }

    /**
     * Imports a new certificate
     *
     * @param body parameters with the path and the file
     */
    public Json importIssuerIntermediate(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String name = getValueString(body, "name");
        transferValueString(body, params, "path");
        if (!body.has("file") || !body.get("file").isString()) {
            return JsonApiResponse.error("File was not correctly uploaded").toJson();
        }
        String file = body.get("file").asString();

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().importIssuer(name, params, file, false).toJson();
    }

    /**
     * Imports a new certificate
     *
     * @param body parameters with the path and the file
     */
    public Json importIssuerRoot(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String name = extractValueString(body, "name");
        params.at("path", "pki");
        body.delAt("path");
        if (!body.has("file") || !body.get("file").isString()) {
            return JsonApiResponse.error("File was not correctly uploaded").toJson();
        }
        String file = body.get("file").asString();

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().importIssuer(name, params, file, true).toJson();
    }

    /**
     * Exports the given certificate at the given path
     *
     * @param body parameters with the path and the issuer_ref
     */
    public Json exportIssuer(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String issuerRef = getValueString(body, "issuer_ref");
        transferValueString(body, params, "path");

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().exportIssuer(issuerRef, params).getResult();
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
        // Check body
//        if (!body.has("name") || !body.get("name").isString()) {
//            return errorVariableNotFound("name");
//        }
//        String name = body.get("name").asString().replace(" ","-");
//        body.set("name", name);

        // region -- Verify required body keys and extract key values

        if (body.has("key_type") && body.get("key_type").isString()) {
            body.set("key_type", body.get("key_type").asString().toLowerCase());
        }

        Json params = Json.object();
        String name = getValueString(body, "name");
        body.set("name", body.get("name").asString().replace(" ","-"));
        String description = getValueStringOrNull(body, "description");
        transferValueString(body, params, "path");
        Integer certificateAuthorityId = getValueInteger(body, "certificate_authority_id");

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().createRole(name, description, certificateAuthorityId.toString(), body, params).toJson();
    }

    /**
     * Gets the information of the given role
     *
     * @param body parameters with the path and name of role
     */
    public Json getRole(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String name = getValueString(body, "name");
        transferValueString(body, params, "path");

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().getRole(name, params).toJson();
    }

    /**
     * Deletes the given role
     *
     * @param body parameters with the path and name of role
     */
    public Json deleteRole(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String name = extractValueString(body, "name");
        Integer id = extractValueInteger(body, "id");
        transferValueString(body, params, "path");

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().deleteRole(id, name, body, params).toJson();
    }

    /**
     * Updates the information of the given role
     *
     * @param body parameters with the path and name of role, others?
     */
    public Json updateRole(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String name = extractValueString(body, "name");
        Integer id = extractValueInteger(body, "id");
        Integer certificateAuthorityId = getValueInteger(body, "certificate_authority_id");
        String description = getValueStringOrNull(body, "description");
        transferValueString(body, params, "path");

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().updateRole(id, name, description, certificateAuthorityId.toString(), body, params).toJson();
    }

    /**
     * Activates the Online Certificate Status Protocol (OCSP)
     *
     * @param body parameters with the path and name of role, others?
     */
    public Json activateOCSP(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, "path");
        transferValueString(body, params, "ocsp_servers");

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().activateOCSP(body, params).toJson();
    }

    /**
     * Validates the template of a certificate
     *
     * @param body parameters with the path, name and issuer_ref
     */
    public Json validateTemplate(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, "path");
        getValueString(body,"issuer_ref");
        getValueString(body,"name");

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().validateTemplate(body, params).toJson();
    }

    /**
     * Generates a certificates at the given path and role
     *
     * @param body parameters with the path and role
     */
    public Json generateCertificate(Json body) {

        // region -- Verify required body keys and extract key values

        String name = extractValueString(body, "name");
        String description = getValueStringOrNull(body, "description");
        Integer vaultRoleId = extractValueInteger(body, "vault_role_id");
        Json params = Json.object();

        transferValueString(body, params, "path");

        String roleName = extractValueString(body,"vault_role_name");
        body.set("role_name", roleName);
        getValueString(body, "common_name");
        getValueString(body, "ttl");

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().generateCertificate(name, description, vaultRoleId.toString(), body, params).toJson();
    }

    /**
     * Gives the list of certificates
     *
     * @param body parameters with the path
     */
    public Json getCertificates(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, "path");

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().getCertificates(params).toJson();
    }

    /**
     * Gives the information of the given certificate
     *
     * @param body parameters with the serialNumber
     */
    public Json getCertificateInfo(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, "path");
        String serialNumber = transferValueString(body, params, "serial_number");
        return manager.getMethods().getCertificateInfo(serialNumber, params).toJson();
    }

    /**
     * Downloads the certificate
     *
     * @param body parameters with the serialNumber
     */
    public Json downloadCertificate(Json body) throws IllegalArgumentException {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, "path");
        String serialNumber = transferValueString(body, params, "serial_number");

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().downloadCertificate(serialNumber, params).getResult();
    }

    /**
     * Revokes the given certificate
     *
     * @param body parameters with the path
     */
    public Json revokeCertificate(Json body) throws IllegalArgumentException {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, "path");
        getValueString(body, "ttl");
        String serialNumber = transferValueString(body, params, "serial_number");

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().revokeCertificate(serialNumber, params).toJson();
    }

    /**
     * Generate root CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public Json generateRootCA(Json body) throws IllegalArgumentException {

        // region -- Verify required body keys and extract key values

        String name = getValueString(body, "common_name");
        getValueString(body, "ttl");

        // endregion -- Verify required body keys and extract key values

        String description = getValueStringOrNull(body, "description");

        return manager.getMethods().generateRootCA(name, description, body, null).toJson();
    }

    /**
     * Generate intermediate CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public Json generateIntermediateCA(Json body) throws IllegalArgumentException {
        String description = getValueStringOrNull(body, "description");

        Json params = Json.object();

        // region -- Verify required body keys and extract key values

        getValueString(body, "type");
        getValueString(body, "ttl");
        getValueString(body, "common_name");
        String name = getValueString(body, "name");

        // endregion -- Verify required body keys and extract key values

        params.at("path", name);

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

    /**
     * Checks if a key exists in a json if its value is a String
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    private boolean propertyExistsAndIsString(Json obj, String key) {
        if (obj.has(key) && obj.get(key).isString()) {
            return true;
        }
        return false;
    }
}
