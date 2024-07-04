package main.services;

import com.csl.autocrypt.AutoCrypt;
import com.csl.autocrypt.Dto.IssuerDto;
import com.csl.core.CSLContext;
import com.csl.intercom.status.IStatusProvider;
import com.ucsl.json.Json;
import main.services.endpoints.AutoCryptEndpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.csl.autocrypt.outils.JsonHelper.*;
import static com.csl.autocrypt.enums.AutocryptConstants.*;

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
        synchronizationSchedule.scheduleAtFixedRate(() -> {
            manager.getMethods().initialSynchronizeDb("pki");
        }, 0, 300, TimeUnit.SECONDS);

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
        addCmd(AutoCryptEndpoints.DELETE_REVOKED_CERTIFICATES, this::deleteRevokedCertificates);
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


        manager.setModuleIp(getValueString(body, IP));
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
        response.at(IP, manager.getModuleIp());
        return JsonApiResponse.result(response).toJson();
    }

    /**
     * Method that changes the port to connect the autocrypt module
     *
     * @param body parameters with the port
     */
    public Json changePort(Json body) {
        manager.setModulePort(getValueInteger(body, PORT));
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
        response.at(PORT, manager.getModulePort());
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

        getValueString(body, PATH);
        String issuerRef = extractValueString(body, ISSUER_REF);

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().getIssuerInfo(issuerRef, body).toJson();
    }

    /**
     * Updates the information of the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json updateIssuerInfo_dto(Json body) {

        // region -- Verify required body keys and extract key values

        drop(body, ID, VAULT_ID);
        IssuerDto issuer = IssuerDto.getIssuerFrom(body);
        issuer.check(IssuerDto.ISSUER_NAME, IssuerDto.PATH, IssuerDto.ISSUER_REF);

        // endregion -- Verify required body keys and extract key values


        return manager.getMethods().updateIssuerInfo(issuer).toJson();
    }

    /**
     * Updates the information of the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json updateIssuerInfo(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String name = getValueString(body, ISSUER_NAME);
        transferValueStringOrNull(body, params, PATH);
        String description = extractValueStringOrNull(body, DESCRIPTION);
        String issuerRef = extractValueString(body, ISSUER_REF);
        Json bodyBase = Json.read(body.toString());
        Json bodyExtra = Json.object();
        transferValueStringOrNull(bodyBase, bodyExtra, TTL_UNIT);
        transferValueStringOrNull(bodyBase, bodyExtra, VAULT_ID);
        transferValueIntegerOrNull(bodyBase, bodyExtra, ID);
        transferValueStringOrNull(bodyBase, bodyExtra, TTL);
        if (body.has(ISSUER_REF)) {bodyExtra.set(ISSUER_REF, body.get(ISSUER_REF).asString());}

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().updateIssuerInfo(name, description, issuerRef, params, bodyBase, bodyExtra).toJson();
    }

    /**
     * Deletes the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json deleteIssuer_dto(Json body) {

        // region -- Verify required body keys and extract key values
        drop(body, ID);
        IssuerDto issuer = IssuerDto.getIssuerFrom(body);
        issuer.check(IssuerDto.ISSUER_REF, IssuerDto.PATH);

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().deleteIssuer(issuer).toJson();
    }

    /**
     * Deletes the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json deleteIssuer(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String issuerRef = extractValueString(body, ISSUER_REF);
        transferValueString(body, params, PATH);

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().deleteIssuer(issuerRef, body, params).toJson();
    }

    /**
     * Imports a new certificate
     *
     * @param body parameters with the path and the file
     */
    public Json importIssuerIntermediate(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String name = getValueString(body, NAME);
        transferValueString(body, params, PATH);
        if (!body.has(FILE) || !body.get(FILE).isString()) {
            return JsonApiResponse.error("File was not correctly uploaded").toJson();
        }
        String file = body.get(FILE).asString();

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
        String name = extractValueString(body, NAME);
        params.at(PATH, PKI);
        drop(body, PATH);
        if (!body.has(FILE) || !body.get(FILE).isString()) {
            return JsonApiResponse.error("File was not correctly uploaded").toJson();
        }
        String file = body.get(FILE).asString();

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
        String issuerRef = getValueString(body, ISSUER_REF);
        transferValueString(body, params, PATH);

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

        // region -- Verify required body keys and extract key values

        if (body.has(KEY_TYPE) && body.get(KEY_TYPE).isString()) {
            body.set(KEY_TYPE, body.get(KEY_TYPE).asString().toLowerCase());
        }

        Json params = Json.object();
        String name = getValueString(body, NAME);
        body.set(NAME, name.replace(" ", "-"));
        String description = extractValueStringOrNull(body, DESCRIPTION);
        transferValueString(body, params, PATH);
        Integer certificateAuthorityId = getValueInteger(body, CERTIFICATE_AUTHORITY_ID);
        Json bodyBase = Json.read(body.toString());
        Json bodyExtra = Json.object();
        if (body.has("organization_unit")) {
            bodyBase.set("ou", body.get("organization_unit").asString());
            bodyExtra.set("organization_unit", body.get("organization_unit").asString());
            drop(bodyBase, "organization_unit");
        }
        if (body.has("state")) {
            bodyBase.set("province", body.get("state").asString());
            bodyExtra.set("state", body.get("state").asString());
            drop(bodyBase, "state");
        }

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().createRole(name, description, certificateAuthorityId.toString(), params, bodyBase, bodyExtra).toJson();
    }

    /**
     * Gets the information of the given role
     *
     * @param body parameters with the path and name of role
     */
    public Json getRole(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String name = getValueString(body, NAME);
        transferValueString(body, params, PATH);

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
        String name = extractValueString(body, NAME);
        drop(body, ID);
        transferValueString(body, params, PATH);

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().deleteRole(name, body, params).toJson();
    }

    /**
     * Updates the information of the given role
     *
     * @param body parameters with the path and name of role, others?
     */
    public Json updateRole(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String name = extractValueString(body, NAME);
        Integer certificateAuthorityId = getValueInteger(body, CERTIFICATE_AUTHORITY_ID);
        String description = extractValueStringOrNull(body, DESCRIPTION);
        transferValueString(body, params, PATH);
        drop(body, ID, CERTIFICATE_AUTHORITY, VAULT_ID, CREATED_AT, UPDATED_AT);
        drop(body, "ip_sans", "uri_sans", "not_before_duration", "not_after", "allow_token_displayname");
        Json bodyBase = Json.read(body.toString());
        Json bodyExtra = Json.object();
//        transferValueStringOrNull(bodyBase, bodyExtra, TTL_UNIT);
        transferValueStringOrNull(bodyBase, bodyExtra, CERTIFICATE_AUTHORITY_ID);
        transferValueStringOrNull(bodyBase, bodyExtra, TTL_UNIT);
        if (body.has(TTL)) {bodyExtra.set(TTL, body.get(TTL).asString());}
        if (body.has("organization_unit")) {
            bodyBase.set("ou", body.get("organization_unit").asString());
            bodyExtra.set("organization_unit", body.get("organization_unit").asString());
            drop(bodyBase, "organization_unit");
        }
        if (body.has("state")) {
            bodyBase.set("province", body.get("state").asString());
            bodyExtra.set("state", body.get("state").asString());
            drop(bodyBase, "state");
        }

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().updateRole(name, description, certificateAuthorityId.toString(), params, bodyBase, bodyExtra).toJson();
    }

    /**
     * Activates the Online Certificate Status Protocol (OCSP)
     *
     * @param body parameters with the path and name of role, others?
     */
    public Json activateOCSP(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, PATH);
        transferValueString(body, params, OCSP_SERVERS);

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
        transferValueString(body, params, PATH);
        getValueString(body, ISSUER_REF);
        getValueString(body, NAME);

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

        String name = extractValueString(body, NAME);
        String description = extractValueStringOrNull(body, DESCRIPTION);
        Integer vaultRoleId = extractValueInteger(body, VAULT_ROLE_ID);
        Json params = Json.object();

        transferValueString(body, params, PATH);

        String roleName = extractValueString(body, VAULT_ROLE_NAME);
        body.set(ROLE_NAME, roleName);
        getValueString(body, COMMON_NAME);
        String ttl = getValueString(body, TTL);
        Json bodyBase = Json.read(body.toString());
        Json bodyExtra = Json.object();
        if (body.has(COMMON_NAME)) {bodyExtra.set(COMMON_NAME, body.get(COMMON_NAME).asString());}
        transferValueStringOrNull(bodyBase, bodyExtra, COUNTRY);
        transferValueStringOrNull(bodyBase, bodyExtra, "state");
        transferValueStringOrNull(bodyBase, bodyExtra, "organization_unit");
        transferValueStringOrNull(bodyBase, bodyExtra, ORGANIZATION);
        transferValueStringOrNull(bodyBase, bodyExtra, LOCALITY);
        transferValueStringOrNull(bodyBase, bodyExtra, TTL_UNIT);
        bodyExtra.set(TTL, ttl);

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().generateCertificate(name, description, vaultRoleId.toString(), params, bodyBase, bodyExtra).toJson();
    }

    /**
     * Gives the list of certificates
     *
     * @param body parameters with the path
     */
    public Json getCertificates(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, PATH);

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
        transferValueString(body, params, PATH);
        String serialNumber = transferValueString(body, params, SERIAL_NUMBER);
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
        transferValueString(body, params, PATH);
        String serialNumber = transferValueString(body, params, SERIAL_NUMBER);

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
        transferValueString(body, params, PATH);
//        getValueString(body, TTL);
        String serialNumber = transferValueString(body, params, SERIAL_NUMBER);

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().revokeCertificate(serialNumber, params).toJson();
    }

    /**
     * Delete all the revoked certificates
     *
     * @param body unused
     */
    public Json deleteRevokedCertificates(Json body) throws IllegalArgumentException {
        return manager.getMethods().deleteRevokedCertificates().toJson();
    }

    /**
     * Generate root CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public Json generateRootCA_dto(Json body) throws IllegalArgumentException {
        body.set(PATH, PKI);
        IssuerDto ca = IssuerDto.getIssuerFrom(body);
        ca.check(IssuerDto.PATH, IssuerDto.TTL, IssuerDto.COMMON_NAME, IssuerDto.ISSUER_NAME, IssuerDto.TYPE);

        return manager.getMethods().generateRootCA(ca).toJson();
    }

    /**
     * Generate root CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public Json generateRootCA(Json body) throws IllegalArgumentException {

        // region -- Verify required body keys and extract key values

        String name = getValueStringOrNull(body, COMMON_NAME);
        String description = extractValueStringOrNull(body, DESCRIPTION);
        Json bodyBase = Json.read(body.toString());
        Json bodyExtra = Json.object();
        transferValueStringOrNull(bodyBase, bodyExtra, TTL_UNIT);
        transferValueStringOrNull(bodyBase, bodyExtra, CA_TYPE);
        if (body.has(TTL)) {bodyExtra.set(TTL, body.get(TTL).asString());}

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().generateRootCA(name, description, null, bodyBase, bodyExtra).toJson();
    }

    /**
     * Generate intermediate CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public Json generateIntermediateCA(Json body) throws IllegalArgumentException {

        // region -- Verify required body keys and extract key values

        String description = extractValueStringOrNull(body, DESCRIPTION);
        String name = getValueString(body, COMMON_NAME);
        Json params = Json.object();
        params.at(PATH, name);
        Json bodyBase = Json.read(body.toString());
        Json bodyExtra = Json.object();
        transferValueStringOrNull(bodyBase, bodyExtra, TTL_UNIT);
        transferValueStringOrNull(bodyBase, bodyExtra, CA_TYPE);
        if (body.has(TTL)) {bodyExtra.set(TTL, body.get(TTL).asString());}

        // endregion -- Verify required body keys and extract key values

        return manager.getMethods().generateIntermediateCA(name, description, params, bodyBase, bodyExtra).toJson();
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
     *
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
