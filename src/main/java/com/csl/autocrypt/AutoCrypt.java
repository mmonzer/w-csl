package com.csl.autocrypt;

import com.csl.autocrypt.enums.AutocryptConstants;
import com.csl.autocrypt.services.*;
import com.csl.core.Config;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.csl.logger.CSLApplicativeLogger;
import com.csl.logger.LoggerActions;
import com.csl.logger.LoggerInterfaces;
import com.ucsl.json.Json;
import lombok.Getter;
import lombok.Setter;
import main.services.JsonApiResponse;
import main.services.endpoints.AutoCryptEndpoints;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.csl.autocrypt.ConvertDapiVault.transformKeysFromDbapiToVault;
import static com.csl.autocrypt.enums.AutocryptConstants.*;
import static com.csl.web.HTTPConstants.CONTENT_DISPOSITION;
import static com.ucsl.json.JsonUtil.mergerJson;
import static main.services.endpoints.AutoCryptEndpoints.*;

/**
 * API client of the module AutoCrypt
 */
public class AutoCrypt {
    public static final String FAILED_TO_CREATE_ROLE_AT_PATH = "Failed to create role {} at path {}";
    public static final String SYNCHRONIZED_ROLES = "synchronized roles";
    private static final Logger log = LoggerFactory.getLogger(AutoCrypt.class);
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

    private static final CSLApplicativeLogger logger = CSLApplicativeLogger.getLogger(AutoCrypt.class);

    private IssuerSynchronizationService issuerSynchronizationService = null;
    private RoleSynchronizationService roleSynchronizationService = null;
    private CertificateSynchronizationService certificateSynchronizationService = null;

    public AutoCrypt(String name) {
        this.name = name;
        moduleIp = Config.INSTANCE.autocrypt.getIp();
        modulePort = Config.INSTANCE.autocrypt.getPort();
    }

    /**
     * Reinit the handler point
     */
    public void reinitApiHandlers() {
        autocryptApiHandler = new ApiHandlerForCSLAutoCrypt(name, moduleIp,  modulePort, false);
        dbApiHandler = new DbapiHandlerForCSLAutoCrypt();
        autocryptApiHandler.addOutputReformer(CSLAutocryptUtils::cleanApiResponse);

        issuerSynchronizationService = new IssuerSynchronizationService(dbApiHandler, autocryptApiHandler);
        roleSynchronizationService = new RoleSynchronizationService(dbApiHandler, autocryptApiHandler);
        certificateSynchronizationService = new CertificateSynchronizationService(dbApiHandler, autocryptApiHandler);
    }

    // region endpoint methods

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
     * @param issuerRef identifier of the issuer (module side)
     * @param params    parameters of the request
     */
    public JsonApiResponse updateIssuerInfo(String issuerRef, Json params, Json body) {
        logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Updating info from issuer {} with params {} and body {}", issuerRef, params, body);

        // Update issuer chez Autocrypt
        logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_AUTOCRYPT_API,"Updating info from issuer {} at path {} in autocrypt ...", issuerRef, params.get(Common.PATH).asString());
        JsonApiResponse responseFromModule = autocryptApiHandler.updateIssuerInfo(issuerRef, body, params);
        if (!responseFromModule.isSuccess()) {
            logger.error(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Updating info from issuer {} at path {} in autocrypt failed", AutoCryptEndpoints.UPDATE_ISSUER_INFO, issuerRef, params.get(Common.PATH).asString());
            return JsonApiResponse.error("Error updating issuer : " + responseFromModule.getError().toJson());
        }
        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_AUTOCRYPT_API,"Updated info from issuer {} at path {} in autocrypt", issuerRef, params.get(Common.PATH).asString());

        // Sync issuers
        try {
            syncIssuers();
            logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API ,"synchronized issuers");
        } catch (SynchronizationException e) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER, FAILED_TO_CREATE_ROLE_AT_PATH, name, params.get(Common.PATH).asString());
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Updated issuer {} at path {}", issuerRef, params.get(Common.PATH).asString());

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
        logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Deleting issuer {} with params {} and body {}", issuerRef, params, body);

        // Delete issuer in autocrypt
        logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_AUTOCRYPT_API,"Deleting issuer {} at path {} in autocrypt ...", issuerRef, params.get(Common.PATH).asString());
        JsonApiResponse responseFromModule = autocryptApiHandler.deleteIssuer(issuerRef, body, params);
        if (!responseFromModule.isSuccess()) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Deleting issuer {} at path {} in autocrypt failed", issuerRef, params.get(Common.PATH).asString());
            return JsonApiResponse.error("Error deleting issuer : " + responseFromModule.getError().toJson());
        }
        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_AUTOCRYPT_API,"Deleted issuer {} at path {} in autocrypt", issuerRef, params.get(Common.PATH).asString());

        // Sync issuers
        try {
            syncIssuers();
            syncCertificates();
            logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API ,"synchronized issuers and certificates");
        } catch (SynchronizationException e) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER, FAILED_TO_CREATE_ROLE_AT_PATH, name, params.get(Common.PATH).asString());
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Deleted issuer {} at path {}", issuerRef, params.get(Common.PATH).asString());

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
            response.at(HttpHeader.CONTENT_TYPE.toString(), CONTENT_DISPOSITION);
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
     * @param params      parameters with the path
     * @param body      body of the creation
     */
    public JsonApiResponse createRole(String name, Json params, Json body) {
        logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Creating role {} at path {} ...", name, params.get(Common.PATH).asString());
        logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Creating role {} with params {} and body {} ", name, params, body);

        transformKeysFromDbapiToVault(body, Common.ORGANIZATION_UNIT, Common.STATE);

        // Create role in autocrypt
        logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_AUTOCRYPT_API,"Creating role {} at path {} in autocrypt ...", name, params.get(Common.PATH).asString());
        JsonApiResponse responseFromModule = autocryptApiHandler.createRole(body, params);
        if (!responseFromModule.isSuccess()) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Creating role {} at path {} in autocrypt failed", name, params.get(Common.PATH).asString());
            return JsonApiResponse.error("Error creating role : " + responseFromModule.getError().toJson());
        }
        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_AUTOCRYPT_API,"Created role {} at path {} in autocrypt", name, params.get(Common.PATH).asString());

        // Sync roles
        try {
            syncRoles();
            logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API , SYNCHRONIZED_ROLES);
        } catch (SynchronizationException e) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER, FAILED_TO_CREATE_ROLE_AT_PATH, name, params.get(Common.PATH).asString());
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Created role {} at path {}", name, params.get(Common.PATH).asString());

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
        logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Deleting role {} with params {} and body {}", name, params, body);

        // Delete role in autocrypt
        logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_AUTOCRYPT_API,"Deleting role {} at path {} in autocrypt ...", name, params.get(Common.PATH).asString());
        JsonApiResponse responseFromModule = autocryptApiHandler.deleteRole(name, body, params);
        if (!responseFromModule.isSuccess()) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Deleting role {} at path {} in autocrypt failed", name, params.get(Common.PATH).asString());
            return responseFromModule;
        }
        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_AUTOCRYPT_API,"Deleted role {} at path {} in autocrypt", name, params.get(Common.PATH).asString());

        // sync roles
        try {
            syncRoles();
            logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API , SYNCHRONIZED_ROLES);
        } catch (SynchronizationException e) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to delete role {} at path {}", name, params.get(Common.PATH).asString());
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Deleted role {} at path {}", name, params.get(Common.PATH).asString());

        return responseFromModule;
    }

    /**
     * Updates the information of the given role
     *
     * @param name        identifier in the module/dbapi side
     * @param params      parameters with the path
     * @param body      body with the field to update
     */
    public JsonApiResponse updateRole(String name, Json params, Json body) {
        logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Updating role {} with params {} and body {}", name, params, body);
        transformKeysFromDbapiToVault(body, Common.ORGANIZATION_UNIT, Common.STATE);

        // Update role in autocrypt
        logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_AUTOCRYPT_API,"Updating role {} at path {} in autocrypt ...", name, params.get(Common.PATH).asString());
        JsonApiResponse responseFromModule = autocryptApiHandler.updateRole(name, body, params);
        if (!responseFromModule.isSuccess()) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Updating role {} at path {} in autocrypt failed", name, params.get(Common.PATH).asString());
            return responseFromModule;
        }
        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_AUTOCRYPT_API,"Updated role {} at path {} in autocrypt", name, params.get(Common.PATH).asString());

        // sync roles
        try {
            syncRoles();
            logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API , SYNCHRONIZED_ROLES);
        } catch (SynchronizationException e) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to update role {} at path {}", name, params.get(Common.PATH).asString());
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Updated role {} at path {}", name, params.get(Common.PATH).asString());

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
     * @param params parameters with the path and role
     * @param body body with the parameters of the certificate
     */
    public JsonApiResponse generateCertificate(Json params, Json body) {
        logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Generating certificate with params {} and body {}", params, body);

        // Generate certificate in autocrypt
        logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_AUTOCRYPT_API,"Generating certificate with params {} and body {}", params, body);
        JsonApiResponse responseFromModule = autocryptApiHandler.generateCertificate(body, params);
        if (!responseFromModule.isSuccess() ||
                !responseFromModule.getResult().has(Certificate.SERIAL_NUMBER) ||
                !responseFromModule.getResult().get(Certificate.SERIAL_NUMBER).isString()) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"certificate creation in Autocrypt failed ");
            return JsonApiResponse.error("Error creating the certificate : " + responseFromModule.getError().toJson());
        }
        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_AUTOCRYPT_API,"certificate ({}) created in Autocrypt",  responseFromModule.getResult().get(Certificate.SERIAL_NUMBER).asString());

        String serialNumber = responseFromModule.getResult().get(Certificate.SERIAL_NUMBER).asString();

        // Sync certificates
        try {
            syncCertificates();
            logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API ,"synchronized certificates");
        } catch (SynchronizationException e) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to generate the certificate {} at path {}", serialNumber, params.get(Common.PATH).asString());
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Successfully generated the certificate {} at path {}", serialNumber, params.get(Common.PATH).asString());

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
        logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"fetching information of certificate ({}) ...", serialNumber);

        logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_AUTOCRYPT_API,"fetching information of certificate ({}) ...", serialNumber);
        JsonApiResponse response = autocryptApiHandler.getCertificateInfo(serialNumber, params);
        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_AUTOCRYPT_API,"fetched information of certificate ({})", serialNumber);

        if (response.isSuccess()) {
            logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"fetched information of certificate ({})", serialNumber);
        } else {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"fetched information of certificate ({}) failed", serialNumber);
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
        logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Fetching certificate {} at path {} ...", serialNumber, params.get("path").asString());
        JsonApiResponse response;

        if (needPrivateKey) {
            logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_AUTOCRYPT_API,"Fetching certificate {} at path {} with private key ...", serialNumber, params.get("path").asString());
            response = autocryptApiHandler.getCertificateWithPrivateKey(serialNumber, params);
        } else {
            logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_AUTOCRYPT_API,"Fetching certificate {} at path {} without private key ...", serialNumber, params.get("path").asString());
            response = autocryptApiHandler.getCertificateWithoutPrivateKey(serialNumber, params);
        }
        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_AUTOCRYPT_API,"Fetched certificate {} at path {} with private key ...", serialNumber, params.get("path").asString());


        if (response.isSuccess()) {
            logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"certificate ({}) at path {} fetched {} private key", serialNumber, params.get("path").asString(), needPrivateKey ? "with" : "without");
        } else {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"fetching certificate ({}) at path {} {} private key failed", serialNumber, params.get("path").asString(), needPrivateKey ? "with" : "without");
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
        logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Downloading certificate {} at path {} and params {}", serialNumber, params.get("path").asString(), params);

        logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_AUTOCRYPT_API,"Downloading certificate {} at path {} and params {}", serialNumber, params.get("path").asString(), params);
        JsonApiResponse responseFromModule = autocryptApiHandler.downloadCertificate(serialNumber, params);
        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_AUTOCRYPT_API,"Downloaded certificate {} at path {} and params {}", serialNumber, params.get("path").asString(), params);

        if (!responseFromModule.isSuccess()) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to download certificate {} at path {} in Autocrypt", serialNumber, params.get("path").asString());
        } else {
            logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Downloaded certificate {} at path {}.", serialNumber, params.get("path").asString());
        }
        return responseFromModule;
    }

    /**
     * Revokes the given certificate
     *
     * @param serialNumber identifier of the certificate on the module side
     * @param params       parameters with the path
     */
    public JsonApiResponse revokeCertificate(String serialNumber, Json params) {
        logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Revoking certificate {} at path {} and params {}", serialNumber, params.get("path").asString(), params);

        String path = params.get("path").asString();

        // Revoke from autocrypt
        logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_AUTOCRYPT_API,"Revoking certificate {} at path {} ...", serialNumber, path);
        JsonApiResponse responseFromModule = autocryptApiHandler.revokeCertificate(serialNumber, params);
        if (!responseFromModule.isSuccess()) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"failed to revoked certificate {} at path {} in Autocrypt", serialNumber, path);
            return responseFromModule;
        }
        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"revoked certificate {} at path {} in Autocrypt", serialNumber, path);

        // sync certificates
        try {
            syncCertificates();
            logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API,"synchronized certificates", serialNumber, path);
        } catch (SynchronizationException e) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Failed to revoke certificate {} at path {}", serialNumber, path);
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"Revoked certificate {} at path {}", serialNumber, path);

        return responseFromModule;
    }

    /**
     * Revokes the given certificate
     *
     * @param body body of the request : with certificate_serial_number, ip, username, password and vendor
     * @param params       parameters with the path
     */
    public JsonApiResponse deployCertificate(Json body, Json params) {
        logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER,"Deploying certificate with params {} and body {}", params, params);

        String path = params.get("path").asString();

        // deploying certificate
        logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_AUTOCRYPT_API,"deploying certificate {} ({}) at device {} ...", body.get(Device.CERTIFICATE_SERIAL_NUMBER), path, body.get(Device.IP));
        JsonApiResponse responseFromModule = autocryptApiHandler.deployCertificate(body, params);
        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_AUTOCRYPT_API,"deployed certificate {} ({}) at device {} with username {} ...", body.get(Device.CERTIFICATE_SERIAL_NUMBER), path, body.get(Device.IP), body.get(Device.USERNAME));

        if (!responseFromModule.isSuccess()) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"failed to deploy certificate {} ({}) at device {}", body.get(Device.CERTIFICATE_SERIAL_NUMBER), path, body.get(Device.IP));
            return responseFromModule;
        }
        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"successfully deployed certificate {} ({}) at device {}", body.get(Device.CERTIFICATE_SERIAL_NUMBER), path, body.get(Device.IP));

        return responseFromModule;
    }

    /**
     * Sign CSR (Certificate Signing Request)
     *
     * @params body: body of the request: csr as string, role name. commonname, name, ttl, ttl unit, etc.
     */
    public JsonApiResponse signCSR(Json body, Json params) {
        logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER, "Signing CSR with body {}", body);
        // Sign CSR in autocrypt
        String path = params.get("path").asString();
        logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_AUTOCRYPT_API, "Signing CSR with body {} at path {}", body);
        JsonApiResponse responseFromModule = autocryptApiHandler.signCSR(body, params);
        if (!responseFromModule.isSuccess() ||
                !responseFromModule.getResult().has(Certificate.SERIAL_NUMBER) ||
                !responseFromModule.getResult().get(Certificate.SERIAL_NUMBER).isString()) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER, "CSR signing in Autocrypt failed");
            return JsonApiResponse.error("Error signing the CSR : " + responseFromModule.getError().toJson());
        }
        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_AUTOCRYPT_API, "CSR signed in Autocrypt with response {}", responseFromModule);
        String serialNumber = responseFromModule.getResult().get(Certificate.SERIAL_NUMBER).asString();
        // Sync certificates
        try {
            syncCertificates();
            logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API ,"synchronized certificates", serialNumber);
        } catch (SynchronizationException e) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER, "Failed to sign CSR with id {} and certificate number {}", serialNumber);
            return JsonApiResponse.error(e.getMessage());
        }
        return responseFromModule;
    }
    /**
     * Generate root CA
     *
     * @param params      parameters with  path
     * @param body      body with the parameters of the CA
     */
    public JsonApiResponse generateRootCA(Json params, Json body) {
        return generateCA(GENERATE_ROOT_CA, params, body);
    }

    /**
     * Generate intermediate CA
     *
     * @param params      parameters with  path
     * @param body      body with the parameters of the CA
     */
    public JsonApiResponse generateIntermediateCA(Json params, Json body) {
        return generateCA(GENERATE_INTERMEDIATE_CA, params, body);
    }

    /**
     * Generic method to generate an intermediate or root CA
     *
     * @param typeCA        either GENERATE_ROOT_CA either GENERATE_INTERMEDIATE_CA
     * @param params        parameters with  path
     * @param body body for creating the CA in CSL-Autocrypt
     * @return if creation was successful, the body for HMI, otherwise, the error message
     */
    private JsonApiResponse generateCA(AutoCryptEndpoints typeCA, Json params, Json body) {

        boolean isRoot = typeCA == AutoCryptEndpoints.GENERATE_ROOT_CA;
        String type = isRoot ? "root" : "intermediate";
        logger.info(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER, "Generating {} CA ...", type);
        logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_SERVER, "Generating {} CA with params {} and body {}", typeCA, params, body);
        transformKeysFromDbapiToVault(body, Common.ORGANIZATION_UNIT, Common.STATE);

        // Creating CA in Autocrypt
        logger.trace(LoggerActions.REQUEST, LoggerInterfaces.CSL_AUTOCRYPT_API, "Creating {} CA creation in Autocrypt with params {} and body {} ...", type, params, body);
        JsonApiResponse responseFromModule;
        if (isRoot) {
            responseFromModule = autocryptApiHandler.generateRootCA(body, params);
        } else {
            responseFromModule = autocryptApiHandler.generateIntermediateCA(body, params);
        }
        if (!responseFromModule.isSuccess() ||
                !responseFromModule.getResult().has(Issuer.ISSUER_REF) ||
                !responseFromModule.getResult().get(Issuer.ISSUER_REF).isString()) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER,"{} CA creation in Autocrypt failed", typeCA, type);
            return JsonApiResponse.error("Error creating the CA : " + responseFromModule.getError().toJson());
        }
        logger.debug(LoggerActions.RESPONSE, LoggerInterfaces.CSL_AUTOCRYPT_API, "Created {} CA creation in Autocrypt with response {}", type, responseFromModule);
        String issuerRef = responseFromModule.getResult().get(Issuer.ISSUER_REF).asString();
        String path = responseFromModule.getResult().get(AutocryptConstants.Common.PATH).asString();
        String serialNumber = responseFromModule.getResult().get(Certificate.SERIAL_NUMBER).asString();

        // Sync issuers
        try {
            syncIssuers();
            syncCertificates();
            logger.debug(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API ,"synchronized issuers and certificates", type, issuerRef, path, serialNumber);
        } catch (SynchronizationException e) {
            logger.error(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER, "Failed to generate {} CA with id {} and certificate number {}", type.substring(0, 1).toUpperCase() + type.substring(1), issuerRef, serialNumber);
            return JsonApiResponse.error(e.getMessage());
        }

        logger.info(LoggerActions.RESPONSE, LoggerInterfaces.CSL_SERVER, "{} CA was successfully generated with id {} and certificate number {}", type.substring(0, 1).toUpperCase() + type.substring(1), issuerRef, serialNumber);

        return responseFromModule;
    }

    // endregion endpoint methods

    // region synchronizing methods

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
            logger.info(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API, "CSL-Autocrypt synchronized with CSL-Dbapi at schedule");
        } catch (SynchronizationException ignored) {
        }
    }

    /**
     * Synchronizes the issuers autocrypt -> dbapi
     */
    private void syncIssuers() throws SynchronizationException {
        try {
            if (issuerSynchronizationService != null) {
                issuerSynchronizationService.syncData();
                logger.trace(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API, "Synchronization of issuers with Dbapi successful");
            }
        } catch (NullPointerException e) {
            logger.warn(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API, "Issuer synchronizer not initialized");
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
                logger.trace(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API, "Synchronization of roles with Dbapi successful");
            }
        } catch (NullPointerException e) {
            logger.error(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API, "Roles synchronizer not initialized");
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
                logger.trace(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API, "Synchronization of certificates with Dbapi successful");
            }
        } catch (NullPointerException e) {
            logger.error(LoggerActions.SYNC, LoggerInterfaces.CSL_AUTOCRYPT_API, "Certificates synchronizer not initialized");
            throw new SynchronizationException("Certificates synchronizer not initialized");
        }
    }

    // endregion synchronizing methods

    /**
     * Verifies if the module api is reachable
     *
     * @return whether it is reachable
     */
    public Json getStatus() {
        return autocryptApiHandler.getStatus();
    }
}
