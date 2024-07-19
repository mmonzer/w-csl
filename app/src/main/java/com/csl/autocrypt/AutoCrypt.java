package com.csl.autocrypt;

import com.csl.autocrypt.services.*;
import com.csl.core.CSLContext;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;
import lombok.Setter;
import main.services.JsonApiResponse;
import main.services.endpoints.AutoCryptEndpoints;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.csl.autocrypt.CSLAutocryptUtils.convertTTLSecondsToStrHours;
import static com.csl.autocrypt.ConvertDapiVault.transformKeysFromVaultToDbapi;
import static com.csl.autocrypt.enums.AutocryptConstants.*;
import static com.csl.autocrypt.outils.JsonHelper.mergerJson;
import static main.services.endpoints.AutoCryptEndpoints.*;

/**
 * API client of the module AutoCrypt
 */
public class AutoCrypt {
    @Setter
    @Getter
    private String moduleIp;
    @Setter
    @Getter
    private int modulePort;
    private final String name;
    @Getter
    private DbapiHandlerForCSLAutoCrypt dbApiHandler = null;
    @Getter
    private ApiHandlerForCSLAutoCrypt autocryptApiHandler = null;
    private boolean shouldSaveToDb = true;
    private static final Logger logger = LoggerFactory.getLogger(AutoCrypt.class);
    private IssuerSynchronizationService issuerSynchronizationService = null;
    private RoleSynchronizationService roleSynchronizationService = null;
    private CertificateSynchronizationService certificateSynchronizationService = null;

    public AutoCrypt(String name) {
        this.name = name;
        Json config = CSLContext.instance.getConfig();
        Json localConfig = config.get("autocrypt");
        moduleIp = JsonUtil.getStringFromJson(localConfig, "ip", "host.docker.internal");
        modulePort = JsonUtil.getIntFromJson(localConfig, "port", 8002);
//        Json globalConfig = config.get("global");
//        dbIp = JsonUtil.getStringFromJson(globalConfig, "ip_server_remote", "host.docker.internal");
//        dbApikey = JsonUtil.getStringFromJson(globalConfig, "api_key", "");
    }

    /**
     * Reinit the handler point
     */
    public void reinitApiHandlers() {
        autocryptApiHandler = new ApiHandlerForCSLAutoCrypt(name, "http://" + moduleIp + ":" + modulePort);
        dbApiHandler = new DbapiHandlerForCSLAutoCrypt();
        autocryptApiHandler.addCleaner(CSLAutocryptUtils::cleanApiResponse);

        issuerSynchronizationService = new IssuerSynchronizationService(dbApiHandler, autocryptApiHandler);
        roleSynchronizationService = new RoleSynchronizationService(dbApiHandler, autocryptApiHandler);
        certificateSynchronizationService = new CertificateSynchronizationService(dbApiHandler, autocryptApiHandler);
    }

    /**
     * Synchronize Dbapi with Autocrypt : (Autocrypt is the source of trust : Autocrypt -> Dbapi only)
     * - Issuers
     * - Roles
     * - Certificates
     */
    public void syncAll() {
        try {
            syncIssuers();
            syncRoles();
            syncCertificates();
        } catch (SynchronizationException ignored) {
        }
    }

    /**
     * Changes the saving to Db
     *
     * @param shouldSaveToDb whether should be saved into dbapi
     */
    public void setSaveToDb(boolean shouldSaveToDb) {
        this.shouldSaveToDb = shouldSaveToDb;
    }

    /**
     * Gets the list of issuers
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getIssuers(Json params) {
        return autocryptApiHandler.getIssuers(params);
    }

    /**
     * Recovers the information of the given issuer
     *
     * @param issuerRef issuer reference
     * @param body      parameters with the path and the issuer id
     */
    public JsonApiResponse getIssuerInfo(String issuerRef, Json body) {
        return autocryptApiHandler.getIssuerInfo(issuerRef, body);
    }

    /**
     * Updates the information of the given issuer in the module and the DB
     *
     * @param name      identifier in the dbapi side
     * @param issuerRef identifier of the issuer (module side)
     * @param params    parameters of the request
     */
    public JsonApiResponse updateIssuerInfo(String name, String description, String issuerRef, Json params, Json bodyBase, Json bodyExtra) {
        logger.info("Updating info from issuer {} at path {} ...", issuerRef, params.get(Common.PATH).asString());

        // Update issuer chez Autocrypt
        logger.debug("Updating info from issuer {} at path {} in autocrypt ...", issuerRef, params.get(Common.PATH).asString());
        JsonApiResponse responseFromModule = autocryptApiHandler.updateIssuerInfo(issuerRef, bodyBase, params);
        if (!responseFromModule.isSuccess()) {
            logger.error("{} ({}/{}) : Updating info from issuer {} at path {} in autocrypt failed", AutoCryptEndpoints.UPDATE_ISSUER_INFO, 2, 3, issuerRef, params.get(Common.PATH).asString());
            return JsonApiResponse.error("Error creating role : " + responseFromModule.getError().toJson());
        }
        logger.info("Updating info from issuer {} at path {} in autocrypt", issuerRef, params.get(Common.PATH).asString());

        // Sync issuers
        try {
            syncIssuers();
        } catch (SynchronizationException e) {
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info("Updated issuer {} at path {}", issuerRef, params.get(Common.PATH).asString());

        return responseFromModule;
    }

    /**
     * Deletes the given issuer from the module and the DB
     *
     * @param issuerRef identifier in the module side
     * @param body      body of the request
     * @param params    parameters with the path
     */
    public JsonApiResponse deleteIssuer(String issuerRef, Json body, Json params) {
        logger.info("Deleting issuer {} at path {} ...", issuerRef, params.get(Common.PATH).asString());

        // Delete issuer in autocrypt
        logger.debug("Deleting issuer {} at path {} in autocrypt ...", issuerRef, params.get(Common.PATH).asString());
        JsonApiResponse responseFromModule = autocryptApiHandler.deleteIssuer(issuerRef, body, params);
        if (!responseFromModule.isSuccess()) {
            logger.error("Deleting issuer {} at path {} in autocrypt failed", issuerRef, params.get(Common.PATH).asString());
            return JsonApiResponse.error("Error creating role : " + responseFromModule.getError().toJson());
        }
        logger.info("Deleting issuer {} at path {} in autocrypt", issuerRef, params.get(Common.PATH).asString());

        // Sync issuers
        try {
            syncIssuers();
        } catch (SynchronizationException e) {
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info("Deleted issuer {} at path {}", issuerRef, params.get(Common.PATH).asString());

        return responseFromModule;
    }

    /**
     * Imports a new issuer
     *
     * @param idName identifier in the dbapi side
     * @param params parameters with the path and the file
     * @param file   file content
     */
    public JsonApiResponse importIssuer(String idName, Json params, String file, boolean isRoot) {
        // Import issuer to Autocrypt
        JsonApiResponse responseFromModule = autocryptApiHandler.importIssuer(params, file);
        if (!responseFromModule.isSuccess() || responseFromModule.getResult().get(Issuer.IMPORTED_ISSUERS).isNull() ||
                responseFromModule.getResult().get(Issuer.IMPORTED_ISSUERS).asJsonList().isEmpty()) {
            return JsonApiResponse.error("Certificate already imported");
        }

        // Get issuer information from autocrypt
        String issuerRef = responseFromModule.getResult().get(Issuer.IMPORTED_ISSUERS).asJsonList().get(0).asString();
        responseFromModule = autocryptApiHandler.getIssuerInfo(issuerRef, params);
        if (!responseFromModule.isSuccess()) {
            return responseFromModule;
        }

        // Save to dbapi
        responseFromModule.getResult().set(Issuer.CA_TYPE, isRoot ? Issuer.ROOT : Issuer.INTERMEDIATE);
        responseFromModule.getResult().set(Issuer.ISSUER_NAME, idName);
        // TODO: deal with serial number and certificate object
        if (isRoot) {
            responseFromModule.getResult().set(Issuer.CA_TYPE, Issuer.ROOT);
        } else {
            responseFromModule.getResult().set(Issuer.CA_TYPE, Issuer.INTERMEDIATE);
        }
        return dbApiHandler.generateCA(issuerRef, idName, isRoot ? Common.PKI : idName, null, null, null,
                mergerJson(responseFromModule.getResult(), params));
    }

    /**
     * Exports the given issuer to JSON
     *
     * @param issuerRef identifier of the issuer
     * @param params    parameters with the path
     * @return the issuer certificate as a file
     */
    public JsonApiResponse exportIssuer(String issuerRef, Json params) {
        JsonApiResponse responseFromModule = autocryptApiHandler.getIssuerInfo(issuerRef, params);
        if (responseFromModule.isSuccess() &&
                responseFromModule.getResult().has(Certificate.CERTIFICATE) && responseFromModule.getResult().get(Certificate.CERTIFICATE).isString()) {
            return JsonApiResponse.result(Json.object(Certificate.CERTIFICATE, responseFromModule.getResult().get(Certificate.CERTIFICATE).asString()));
        }
        return responseFromModule;
    }

    /**
     * Exports the given issuer into file
     *
     * @param issuerRef identifier of the issuer
     * @param params    parameters with the path
     * @return the issuer certificate as a file
     */
    public JsonApiResponse exportIssuerToFile(String issuerRef, Json params) {
        JsonApiResponse responseFromModule = autocryptApiHandler.getIssuerInfo(issuerRef, params);
        if (responseFromModule.isSuccess() &&
                responseFromModule.getResult().has(Certificate.CERTIFICATE) && responseFromModule.getResult().get(Certificate.CERTIFICATE).isString()) {
            Json response = Json.object();
            // TODO : range this formating elsewhere, probably not the right place
            response.at("Content-Type", "application/octet-stream");
            response.at("Content", responseFromModule.getResult().get(Certificate.CERTIFICATE).asString());
            return JsonApiResponse.result(response);
        }
        return responseFromModule;
    }

    /**
     * Gets the list of roles
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getRoles(Json params) {
        return autocryptApiHandler.getRoles(params);
    }

    /**
     * Creates a new role
     *
     * @param name        identifier in the dbapi side
     * @param description description in the dbapi side
     * @param params      parameters with the path
     */
    public JsonApiResponse createRole(String name, String description, String certificateAuthorityId, Json params, Json bodyBase, Json bodyExtra) {
        logger.info("Creating role {} at path {} ...", name, params.get(Common.PATH).asString());

        // Create role in autocrypt
        logger.debug("Creating role {} at path {} in autocrypt ...", name, params.get(Common.PATH).asString());
        JsonApiResponse responseFromModule = autocryptApiHandler.createRole(bodyBase, params);
        if (!responseFromModule.isSuccess()) {
            logger.error("Creating role {} at path {} in autocrypt failed", name, params.get(Common.PATH).asString());
            return JsonApiResponse.error("Error creating role : " + responseFromModule.getError().toJson());
        }
        logger.info("Created role {} at path {} in autocrypt", name, params.get(Common.PATH).asString());

        // Sync roles
        try {
            syncRoles();
        } catch (SynchronizationException e) {
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info("Created role {} at path {}", name, params.get(Common.PATH).asString());

        return responseFromModule;
    }

    /**
     * Gets the information of the given role
     *
     * @param name   name of the role
     * @param params parameters with the path and name of role
     */
    public JsonApiResponse getRole(String name, Json params) {
        return autocryptApiHandler.getRole(name, params);
    }

    /**
     * Deletes the given role
     *
     * @param name   name in the module/dbapi side
     * @param body   body of the request
     * @param params parameters with the path
     */
    public JsonApiResponse deleteRole(String name, Json body, Json params) {
        logger.info("Deleting role {} at path {} ...", name, params.get(Common.PATH).asString());

        // Delete role in autocrypt
        logger.debug("Deleting role {} at path {} in autocrypt ...", name, params.get(Common.PATH).asString());
        JsonApiResponse responseFromModule = autocryptApiHandler.deleteRole(name, body, params);
        if (!responseFromModule.isSuccess()) {
            logger.error("{} ({}/{}) : Deleting role {} at path {} in autocrypt failed", AutoCryptEndpoints.DELETE_ROLE, 1, 2, name, params.get(Common.PATH).asString());
            return responseFromModule;
        }
        logger.info("Deleted role {} at path {} in autocrypt", name, params.get(Common.PATH).asString());

        // sync roles
        try {
            syncRoles();
        } catch (SynchronizationException e) {
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info("Deleted role {} at path {}", name, params.get(Common.PATH).asString());

        return responseFromModule;
    }

    /**
     * Updates the information of the given role
     *
     * @param name        identifier in the module/dbapi side
     * @param description description in the dbapi
     * @param params      parameters with the path
     */
    public JsonApiResponse updateRole(String name, String description, String certificateAuthorityId, Json params, Json bodyBase, Json bodyExtra) {
        logger.info("Updating role {} at path {} ...", name, params.get(Common.PATH).asString());

        // Update role in autocrypt
        logger.debug("Updating role {} at path {} in autocrypt ...", name, params.get(Common.PATH).asString());
        JsonApiResponse responseFromModule = autocryptApiHandler.updateRole(name, bodyBase, params);
        if (!responseFromModule.isSuccess()) {
            logger.error("{} ({}/{}) : Updating role {} at path {} in autocrypt failed", AutoCryptEndpoints.UPDATE_ROLE, 1, 2, name, params.get(Common.PATH).asString());
            return responseFromModule;
        }
        logger.info("Updated role {} at path {} in autocrypt", name, params.get(Common.PATH).asString());

        // sync roles
        try {
            syncRoles();
        } catch (SynchronizationException e) {
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info("Updated role {} at path {}", name, params.get(Common.PATH).asString());

        return responseFromModule;
    }

    /**
     * Activates the Online Certificate Status Protocol (OCSP)
     *
     * @param body   body of the request
     * @param params parameters with the path
     */
    public JsonApiResponse activateOCSP(Json body, Json params) {
        return autocryptApiHandler.activateOCSP(body, params);
    }

    /**
     * Validate the template of a certificate
     *
     * @param body   body of the request
     * @param params parameters with the path and role
     */
    public JsonApiResponse validateTemplate(Json body, Json params) {
        return autocryptApiHandler.validateTemplate(body, params);
    }

    /**
     * Generates a certificates at the given path and role
     *
     * @param name   identifier in the dbapi side
     * @param params parameters with the path and role
     */
    public JsonApiResponse generateCertificate(String name, String description, String vaultRoleId, Json params, Json bodyBase, Json bodyExtra) {
        logger.info("Generating certificate ...");

        // Generate certificate in autocrypt : 1
        logger.debug("generating certificate in Autocrypt ...");
        JsonApiResponse responseFromModule = autocryptApiHandler.generateCertificate(bodyBase, params);
        if (!responseFromModule.isSuccess() ||
                !responseFromModule.getResult().has(Certificate.SERIAL_NUMBER) ||
                !responseFromModule.getResult().get(Certificate.SERIAL_NUMBER).isString()) {
            logger.error("certificate creation in Autocrypt failed ");
            return JsonApiResponse.error("Error creating the certificate : " + responseFromModule.getError().toJson());
        }
        logger.info("certificate ({}) created in Autocrypt",  responseFromModule.getResult().get(Certificate.SERIAL_NUMBER).asString());

        String serialNumber = responseFromModule.getResult().get(Certificate.SERIAL_NUMBER).asString();

        // Sync certificates
        try {
            syncCertificates();
        } catch (SynchronizationException e) {
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info("Successfully generated the certificate {}", serialNumber);

        return responseFromModule;
    }

    /**
     * Gives the list of certificates
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getCertificates(Json params) {
        return autocryptApiHandler.getCertificates(params);
    }

    /**
     * Gives the information of the given certificate
     *
     * @param serialNumber identifier of the certificate
     * @param params       parameters with the serialNumber
     */
    public JsonApiResponse getCertificateInfo(String serialNumber, Json params) {
        JsonApiResponse response = autocryptApiHandler.getCertificateInfo(serialNumber, params);

        if (response.isSuccess()) {
            logger.info("{} ({}/{}) : information of certificate ({}) fetched", AutoCryptEndpoints.GET_CERTIFICATE_INFO, 1, 1, serialNumber);
        } else {
            logger.error("{} ({}/{}) : fetching information of certificate ({}) failed", AutoCryptEndpoints.GET_CERTIFICATE_INFO, 1, 1, serialNumber);
        }
        return response;
    }

    /**
     * Get the given certificate
     *
     * @param serialNumber   identifier of the certificate
     * @param needPrivateKey whether we need the private key
     * @param params         parameters with the serialNumber
     */
    public JsonApiResponse getCertificate(String serialNumber, boolean needPrivateKey, Json params) {
        logger.info("Fetching certificate {} at path {} ...", serialNumber, params.get("path").asString());
        JsonApiResponse response;

        if (needPrivateKey) {
            logger.debug("{} : Fetching certificate {} at path {} with private key ...", AutoCryptEndpoints.GET_CERTIFICATE, serialNumber, params.get("path").asString());
            response = autocryptApiHandler.getCertificateWithPrivateKey(serialNumber, params);
        } else {
            logger.debug("{} : Fetching certificate {} at path {} without private key ...", AutoCryptEndpoints.GET_CERTIFICATE, serialNumber, params.get("path").asString());
            response = autocryptApiHandler.getCertificateWithoutPrivateKey(serialNumber, params);
        }

        if (response.isSuccess()) {
            logger.info("{} : certificate ({}) at path {} fetched {} private key", AutoCryptEndpoints.GET_CERTIFICATE, serialNumber, params.get("path").asString(), needPrivateKey ? "with" : "without");
        } else {
            logger.error("{} : fetching certificate ({}) at path {} {} private key failed", AutoCryptEndpoints.GET_CERTIFICATE, serialNumber, params.get("path").asString(), needPrivateKey ? "with" : "without");
        }

        return response;
    }

    /**
     * Downloads the given certificate
     *
     * @param serialNumber identifier of the certificate
     * @param params       parameters with the serialNumber
     */
    public JsonApiResponse downloadCertificate(String serialNumber, Json params) {
        logger.info("Downloading certificate {} at path {} ...", serialNumber, params.get("path").asString());

        JsonApiResponse responseFromModule = autocryptApiHandler.downloadCertificate(serialNumber, params);
        if (!responseFromModule.isSuccess()) {
            logger.error("{} : Failed to download certificate {} at path {} in Autocrypt", AutoCryptEndpoints.DOWNLOAD_CERTIFICATE, serialNumber, params.get("path").asString());
        }
        logger.info("Downloaded certificate {} at path {}.", serialNumber, params.get("path").asString());

        return responseFromModule;
    }

    /**
     * Revokes the given certificate
     *
     * @param serialNumber identifier of the certificate on the module side
     * @param params       parameters with the path
     */
    public JsonApiResponse revokeCertificate(String serialNumber, Json params) {
        String path = params.get("path").asString();
        logger.info("Revoking certificate {} at path {} ...", serialNumber, path);

        // Revoke from autocrypt
        logger.debug("revoking certificate {} at path {} in Autocrypt ...", serialNumber, path);
        JsonApiResponse responseFromModule = autocryptApiHandler.revokeCertificate(serialNumber, params);
        if (!responseFromModule.isSuccess()) {
            logger.error("{} ({}/{}) : failed to revoked certificate {} at path {} in Autocrypt", AutoCryptEndpoints.REVOKE_CERTIFICATE, 1, 2, serialNumber, path);
            return responseFromModule;
        }
        logger.info("revoked certificate {} at path {} in Autocrypt", serialNumber, path);

        // sync certificates
        try {
            syncCertificates();
        } catch (SynchronizationException e) {
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info("Revoked certificate {} at path {}", serialNumber, path);

        return responseFromModule;
    }

    /**
     * Generate root CA
     *
     * @param name        identifier in the dbapi side
     * @param description description in the dbapi
     * @param params      parameters with  path
     */
    public JsonApiResponse generateRootCA(String name, String description, Json params, Json bodyBase, Json bodyExtra) {
        return generateCA(GENERATE_ROOT_CA, name, description, params, bodyBase, bodyExtra);
    }

    /**
     * Generate intermediate CA
     *
     * @param name        identifier in the dbapi side
     * @param description description in the dbapi
     * @param params      parameters with  path
     */
    public JsonApiResponse generateIntermediateCA(String name, String description, Json params, Json bodyBase, Json bodyExtra) {
        return generateCA(GENERATE_INTERMEDIATE_CA, name, description, params, bodyBase, bodyExtra);
    }

    /**
     * Generic method to generate a intermediate or root CA
     *
     * @param typeCA        either GENERATE_ROOT_CA either GENERATE_INTERMEDIATE_CA
     * @param name          identifier of the CA (issuerName)
     * @param description   description in the dbapi
     * @param params        parameters with  path
     * @param bodyAutocrypt body for creating the CA in CSL-Autocrypt
     * @param bodyDBapi     body for creating the CA in CSL-Dbapi
     * @return if creation was successful, the body for HMI, otherwise, the error message
     */
    private JsonApiResponse generateCA(AutoCryptEndpoints typeCA, String name, String description, Json params, Json bodyAutocrypt, Json bodyDBapi) {
        boolean isRoot = typeCA == AutoCryptEndpoints.GENERATE_ROOT_CA;
        String type = isRoot ? "root" : "intermediate";
        String path = params.get(Common.PATH).asString();
        logger.info("Generating {} CA ...", type);

        // Creating CA in Autocrypt
        logger.debug("Creating {} CA creation in Autocrypt at path {} ...", type, path);
        JsonApiResponse responseFromModule;
        if (isRoot) {
            responseFromModule = autocryptApiHandler.generateRootCA(bodyAutocrypt, params);
        } else {
            responseFromModule = autocryptApiHandler.generateIntermediateCA(bodyAutocrypt, params);
        }
        if (!responseFromModule.isSuccess() ||
                !responseFromModule.getResult().has(Issuer.ISSUER_REF) ||
                !responseFromModule.getResult().get(Issuer.ISSUER_REF).isString()) {
            logger.error("{} ({}/{}) : {} CA creation at path {} in Autocrypt failed", typeCA, 1, 4, type, path);
            return JsonApiResponse.error("Error creating the CA : " + responseFromModule.getError().toJson());
        }
        String issuerRef = responseFromModule.getResult().get(Issuer.ISSUER_REF).asString();
        logger.info("{} CA ({}) created in Autocrypt at path {}", type, issuerRef, path);

        String serialNumber = responseFromModule.getResult().get(Certificate.SERIAL_NUMBER).asString();

        // Sync issuers
        try {
            syncIssuers();
        } catch (SynchronizationException e) {
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info("{} CA was successfully generated with id {} and certificate number {}", type.substring(0, 1).toUpperCase() + type.substring(1), issuerRef, serialNumber);

        return responseFromModule;
    }

    /**
     * Synchronizes the issuers autocrypt -> dbapi
     */
    private void syncIssuers() throws SynchronizationException {
        try {
            if (issuerSynchronizationService != null) {
                issuerSynchronizationService.syncData();
            }
        } catch (NullPointerException e) {
            logger.error("Issuer synchronizer not initialized");
            throw new SynchronizationException("Issuer synchronizer not initialized");
        }
    }

    /**
     * Synchronizes the roles autocrypt -> dbapi
     */
    private void syncRoles() throws SynchronizationException {
        try {
            if (roleSynchronizationService != null) {
                roleSynchronizationService.syncData();
            }
        } catch (NullPointerException e) {
            logger.error("Roles synchronizer not initialized");
            throw new SynchronizationException("Roles synchronizer not initialized");
        }
    }

    /**
     * Synchronizes the certificates autocrypt -> dbapi
     */
    private void syncCertificates() throws SynchronizationException {
        try {
            if (certificateSynchronizationService != null) {
                certificateSynchronizationService.syncData();
            }
        } catch (NullPointerException e) {
            logger.error("Certificates synchronizer not initialized");
            throw new SynchronizationException("Certificates synchronizer not initialized");
        }
    }

    /**
     * Verifies if the module api is reachable
     *
     * @return whether it is reachable
     */
    public Json getStatus() {
//        return Json.object();
        return autocryptApiHandler.getStatus();
    }

    /**
     * Sends the request to the BD if shouldSaveToDb is set to true and if the responseFrom module is true, otherwise
     * returns the response from the module
     *
     * @param method             method to call when sending to DBApi
     * @param responseFromModule response from the module
     * @return new response if condition true, otherwise resend the same responseFromModule
     */
    public JsonApiResponse sendToDbApiIfSaveToDb(IJsonApiResponser method, String name, JsonApiResponse responseFromModule) {
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            Json nextRequestBody = responseFromModule.getResult();
            nextRequestBody = (nextRequestBody != null) ? nextRequestBody : Json.object();
            return method.apply(name, nextRequestBody);
        } else {
            return responseFromModule;
        }
    }

    /**
     * Sends the request to the BD if shouldSaveToDb is set to true and if the responseFrom module is true, otherwise
     * returns the response from the module
     *
     * @param method             method to call when sending to DBApi
     * @param responseFromModule response from the module
     * @return new response if condition true, otherwise resend the same responseFromModule
     */
    public JsonApiResponse sendToDbApiIfSaveToDb(IJsonApiResponserWithDescription method, String name, String description, JsonApiResponse responseFromModule) {
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            Json nextRequestBody = responseFromModule.getResult();
            nextRequestBody = (nextRequestBody != null) ? nextRequestBody : Json.object();
            return method.apply(name, description, nextRequestBody);
        } else {
            return responseFromModule;
        }
    }

    /**
     * Sends the request to the BD if shouldSaveToDb is set to true and if the responseFrom module is true, otherwise
     * returns the response from the module
     *
     * @param method             method to call when sending to DBApi
     * @param responseFromModule response from the module
     * @return new response if condition true, otherwise resend the same responseFromModule
     */
    public JsonApiResponse sendToDbApiIfSaveToDb(IJsonApiResponserWithId method, int id, String name, JsonApiResponse responseFromModule) {
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            Json nextRequestBody = responseFromModule.getResult();
            nextRequestBody = (nextRequestBody != null) ? nextRequestBody : Json.object();
            return method.apply(id, name, nextRequestBody);
        } else {
            return responseFromModule;
        }
    }

    /**
     * Sends the request to the BD if shouldSaveToDb is set to true and if the responseFrom module is true, otherwise
     * returns the response from the module
     *
     * @param method             method to call when sending to DBApi
     * @param responseFromModule response from the module
     * @return new response if condition true, otherwise resend the same responseFromModule
     */
    public JsonApiResponse sendToDbApiIfSaveToDb(IJsonApiResponserWithIdWithDescription method, int id, String name, String description, JsonApiResponse responseFromModule) {
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            Json nextRequestBody = responseFromModule.getResult();
            nextRequestBody = (nextRequestBody != null) ? nextRequestBody : Json.object();
            return method.apply(id, name, description, nextRequestBody);
        } else {
            return responseFromModule;
        }
    }

    /**
     * Sends the request to the BD if shouldSaveToDb is set to true and if the responseFrom module is true, otherwise
     * returns the response from the module
     *
     * @param method             method to call when sending to DBApi
     * @param responseFromModule response from the module
     * @return new response if condition true, otherwise resend the same responseFromModule
     */
    public JsonApiResponse sendToDbApiIfSaveToDb(IJsonApiResponserWithIdStrWithDescription method, String id, String name, String description, JsonApiResponse responseFromModule) {
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            Json nextRequestBody = responseFromModule.getResult();
            nextRequestBody = (nextRequestBody != null) ? nextRequestBody : Json.object();
            return method.apply(id, name, description, nextRequestBody);
        } else {
            return responseFromModule;
        }
    }

    /**
     * Sends the request to the BD if shouldSaveToDb is set to true and if the responseFrom module is true, otherwise
     * returns the response from the module
     *
     * @param method             method to call when sending to DBApi
     * @param responseFromModule response from the module
     * @return new response if condition true, otherwise resend the same responseFromModule
     */
    public JsonApiResponse sendToDbApiIfSaveToDb(IJsonApiResponserWithIdStrWithDescriptionWithPath method, String id, String name, String description, String path, JsonApiResponse responseFromModule) {
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            Json nextRequestBody = responseFromModule.getResult();
            nextRequestBody = (nextRequestBody != null) ? nextRequestBody : Json.object();
            return method.apply(id, name, description, path, nextRequestBody);
        } else {
            return responseFromModule;
        }
    }
}
