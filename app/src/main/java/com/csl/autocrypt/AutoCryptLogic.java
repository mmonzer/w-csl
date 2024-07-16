package com.csl.autocrypt;

import com.ucsl.json.Json;
import main.services.AutoCryptService;
import main.services.JsonApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.csl.autocrypt.ConvertDapiVault.transformToDbapi;
import static com.csl.autocrypt.enums.AutocryptConstants.*;
import static com.csl.autocrypt.outils.JsonHelper.*;
import static main.services.endpoints.AutoCryptEndpoints.*;

import main.services.endpoints.AutoCryptEndpoints;

public class AutoCryptLogic {
    private final ApiHandlerForCSLAutoCrypt moduleHandler;
    private final DbapiHandlerForCSLAutoCrypt dbHandler;
    private boolean shouldSaveToDb = true;
    private static final Logger logger = LoggerFactory.getLogger(AutoCryptService.class);

    public AutoCryptLogic(ApiHandlerForCSLAutoCrypt module, DbapiHandlerForCSLAutoCrypt db) {
        this.moduleHandler = module;
        this.dbHandler = db;
    }

    /**
     * Gets the list of issuers
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getIssuers(Json params) {
        return moduleHandler.getIssuers(params);
    }

    /**
     * Recovers the information of the given issuer
     *
     * @param issuerRef issuer reference
     * @param body      parameters with the path and the issuer id
     */
    public JsonApiResponse getIssuerInfo(String issuerRef, Json body) {
        return moduleHandler.getIssuerInfo(issuerRef, body);
    }

    /**
     * Updates the information of the given issuer in the module and the DB
     *
     * @param name      identifier in the dbapi side
     * @param issuerRef identifier of the issuer (module side)
     * @param params    parameters of the request
     */
    public JsonApiResponse updateIssuerInfo(String name, String description, String issuerRef, Json params, Json bodyBase, Json bodyExtra) {
        logger.info("Updating info from issuer {} at path {} ...", issuerRef, params.get(PATH).asString());

        // Get all information from issuer dbapi : 1
        logger.debug("{} ({}/{}) : Fetching info from issuer {} at path {} in dbapi ...", UPDATE_ISSUER_INFO,3, issuerRef, params.get(PATH).asString());
        JsonApiResponse oldValuesFromDbapi = dbHandler.getInfoIssuerFromDbapi(issuerRef);
        if (!oldValuesFromDbapi.isSuccess()) {
            logger.error("{} ({}/{}) : Fetching info from issuer {} at path {} in dbapi failed", AutoCryptEndpoints.UPDATE_ISSUER_INFO,3, issuerRef, params.get(PATH).asString());
            return JsonApiResponse.error("Error creating role : " + oldValuesFromDbapi.getError().toJson());
        }
        logger.info("{} ({}/{}) : Fetching info from issuer {} at path {} in dbapi", AutoCryptEndpoints.UPDATE_ISSUER_INFO,3, issuerRef, params.get(PATH).asString());

        // Update issuer in autocrypt : 2
        logger.debug("{} ({}/{}) : Updating info from issuer {} at path {} in autocrypt ...", AutoCryptEndpoints.UPDATE_ISSUER_INFO, 2, 3, issuerRef, params.get(PATH).asString());
        JsonApiResponse responseFromModule = moduleHandler.updateIssuerInfo(issuerRef, bodyBase, params);
        if (!responseFromModule.isSuccess()) {
            logger.error("{} ({}/{}) : Updating info from issuer {} at path {} in autocrypt failed", AutoCryptEndpoints.UPDATE_ISSUER_INFO,2, 3, issuerRef, params.get(PATH).asString());
            return JsonApiResponse.error("Error creating role : " + responseFromModule.getError().toJson());
        }
        logger.info("{} ({}/{}) : Updating info from issuer {} at path {} in autocrypt", AutoCryptEndpoints.UPDATE_ISSUER_INFO, 2, 3, issuerRef, params.get(PATH).asString());

        // Update issuer in autocrypt : 3
        logger.debug("{} ({}/{}) : Updating info from issuer {} at path {} in autocrypt ...", AutoCryptEndpoints.UPDATE_ISSUER_INFO, 3, 3, issuerRef, params.get(PATH).asString());
        JsonApiResponse responseFromDbapi = dbHandler.updateIssuerInfo(name, issuerRef, description, params.get(PATH).asString(),
                mergerJson(responseFromModule.getResult(),
                        mergerJson(bodyExtra, oldValuesFromDbapi.getResult())
                )
        );
        if (!responseFromDbapi.isSuccess()) {
            logger.error("{} ({}/{}) : Updating info issuer {} at path {} in dbapi failed", AutoCryptEndpoints.UPDATE_ISSUER_INFO, 3, 3, issuerRef, params.get(PATH).asString());
            return responseFromDbapi;
        }
        logger.info("{} ({}/{}) : Updating info issuer {} at path {} in dbapi", AutoCryptEndpoints.UPDATE_ISSUER_INFO, 3, 3, issuerRef, params.get(PATH).asString());

        logger.info("Updated issuer {} at path {}", issuerRef, params.get(PATH).asString());

        return responseFromDbapi;

    }

    /**
     * Deletes the given issuer from the module and the DB
     *
     * @param issuerRef identifier in the module side
     * @param body      body of the request
     * @param params    parameters with the path
     */
    public JsonApiResponse deleteIssuer(String issuerRef, Json body, Json params) {
        logger.info("Deleting issuer {} at path {} ...", issuerRef, params.get(PATH).asString());

        // Delete issuer in autocrypt : 1
        logger.debug("{} ({}/{}) : Deleting issuer {} at path {} in autocrypt ...", AutoCryptEndpoints.DELETE_ISSUER_INFO, 1, 2, issuerRef, params.get(PATH).asString());
        JsonApiResponse responseFromModule = moduleHandler.deleteIssuer(issuerRef, body, params);
        if (!responseFromModule.isSuccess()) {
            logger.error("{} ({}/{}) : Deleting issuer {} at path {} in autocrypt failed", AutoCryptEndpoints.DELETE_ISSUER_INFO, 1, 2, issuerRef, params.get(PATH).asString());
            return JsonApiResponse.error("Error creating role : " + responseFromModule.getError().toJson());
        }
        logger.info("{} ({}/{}) : Deleting issuer {} at path {} in autocrypt", AutoCryptEndpoints.DELETE_ISSUER_INFO, 1, 2, issuerRef, params.get(PATH).asString());

        // delete issuer in  dbapi : 2
        logger.debug("{} ({}/{}) : Deleting issuer {} at path {} in dbapi ...", AutoCryptEndpoints.DELETE_ISSUER_INFO, 2, 2, issuerRef, params.get(PATH).asString());
        JsonApiResponse responseFromDbapi = dbHandler.deleteIssuer(issuerRef, responseFromModule.getResult());
        if (!responseFromDbapi.isSuccess()) {
            logger.error("{} ({}/{}) : Deleting issuer {} at path {} in dbapi failed", AutoCryptEndpoints.DELETE_ISSUER_INFO, 2, 2, issuerRef, params.get(PATH).asString());
            return responseFromDbapi;
        }
        logger.info("{} ({}/{}) : Deleting issuer {} at path {} in dbapi", AutoCryptEndpoints.DELETE_ISSUER_INFO, 2, 2, issuerRef, params.get(PATH).asString());

        logger.info("Deleted issuer {} at path {}", issuerRef, params.get(PATH).asString());

        return responseFromDbapi;
    }

    /**
     * Deletes the given issuer from the module and the DB in cascade (roles and certs)
     *
     * @param id        identifier in the dbapi side
     * @param name      name in the dbapi side
     * @param issuerRef identifier in the module side
     * @param body      body of the request
     * @param params    parameters with the path
     */
    public JsonApiResponse deleteIssuerCascade(int id, String name, String issuerRef, Json body, Json params) {
        // TODO : rewrite code properly
        // delete all issuers from path
        JsonApiResponse response = deleteIssuersOfPath(name, issuerRef, body, params);
        if (!response.isSuccess()) {
            return response;
        }
        // delete roles of the issuer (one intermediate issuer per path)
        response = deleteRolesOfPath(id, name, params);
        if (!response.isSuccess()) {
            return response;
        }
        // delete certificates of the issuer (one intermediate issuer per path)
        return revokeCertificatesOfPath(params);
    }

    /**
     * Revokes all the certificates from a given Vault path
     *
     * @param name      name of the issuer.
     * @param issuerRef reference of the issuer.
     * @param body      body for the request
     * @param params    parameters for the request ( has the path at least)
     */
    private JsonApiResponse deleteIssuersOfPath(String name, String issuerRef, Json body, Json params) {
        // Delete the issuer from module
        JsonApiResponse responseFromModule = moduleHandler.deleteIssuer(issuerRef, body, params);
        if (!responseFromModule.isSuccess()) {
            return JsonApiResponse.error("Error deleting the main issuer from Vault");
        }
        // Delete the issuer from dbapi
        JsonApiResponse responseFromDbapi = sendToDbApiIfSaveToDb(dbHandler::deleteIssuer, issuerRef, responseFromModule);
        if (!responseFromDbapi.isSuccess()) {
            return JsonApiResponse.error("Error deleting the main issuer from DBapi");
        }
        JsonApiResponse issuersToDelete = moduleHandler.getIssuers(params);
        // Delete the other issuers in the path, except for pki (only roots)
        if (issuersToDelete.isSuccess() && params.has(PATH) &&
                params.get(PATH).isString() && !params.get(PATH).asString().equals(PKI)) {
            JsonApiResponse responseFromOtherIssuers;
            for (Json issuer : issuersToDelete.getResult()) {
                responseFromOtherIssuers = moduleHandler.revokeCertificate(issuer.asString(), params);
                if (!responseFromOtherIssuers.isSuccess()) {
                    return JsonApiResponse.error("Error deleting the issuer (" + issuer + ") from module");
                }
            }
        }
        return JsonApiResponse.success();
    }

    /**
     * Deletes all the roles from a given Vault path
     *
     * @param caId   identifier of the CA in dbapi
     * @param name   name of the role
     * @param params parameters for the request ( has the path at least)
     * @return whether the deletion was successful
     */
    private JsonApiResponse deleteRolesOfPath(int caId, String name, Json params) {
        JsonApiResponse rolesToDeleteModule = moduleHandler.getRoles(params);
        JsonApiResponse rolesToDeleteDbapi = dbHandler.listRoles();
        JsonApiResponse response;
        if (rolesToDeleteModule.isSuccess() && rolesToDeleteDbapi.isSuccess()) {
            for (Json roleDbapi : rolesToDeleteDbapi.getResult()) {
                if (roleDbapi.has(CERTIFICATE_AUTHORITY_ID) && roleDbapi.get(CERTIFICATE_AUTHORITY_ID).isNumber() && roleDbapi.get(CERTIFICATE_AUTHORITY_ID).asInteger() == caId &&
                        roleDbapi.has(NAME) && roleDbapi.get(NAME).isString() && roleDbapi.get(NAME).asString().equals(name)) {
                    response = dbHandler.deleteRole(roleDbapi.get(ID).asString(), null, null);
                    if (!response.isSuccess()) {
                        return JsonApiResponse.error("Error when deleting '" + name + "' from dbapi");
                    }
                }
            }
            for (Json roleDbapi : rolesToDeleteModule.getResult()) {
                response = moduleHandler.deleteRole(roleDbapi.asString(), null, params);
                if (!response.isSuccess()) {
                    return JsonApiResponse.error("Error when deleting '" + name + "' from module");
                }
            }
        }
        return JsonApiResponse.success();
    }

    /**
     * Revokes all the certificates from a given Vault path
     *
     * @param params parameters for the request ( has the path at least)
     * @return whether the delete was successful
     */
    private JsonApiResponse revokeCertificatesOfPath(Json params) {
        JsonApiResponse certificatesToRevoke = moduleHandler.getCertificates(params);
        JsonApiResponse response;
        for (Json cert : certificatesToRevoke.getResult()) {
            response = moduleHandler.revokeCertificate(cert.asString(), params);
            if (response.isSuccess()) {
                dbHandler.revokeCertificate(cert.asString(), params);
            }
            if (!response.isSuccess()) {
                return JsonApiResponse.error("Error while deleting the certificate '" + cert);
            }
        }
        return JsonApiResponse.success();
    }

    /**
     * Imports a new issuer
     *
     * @param idName identifier in the dbapi side
     * @param params parameters with the path and the file
     * @param file   file content
     */
    public JsonApiResponse importIssuer(String idName, Json params, String file, boolean isRoot) {
        JsonApiResponse responseFromModule = moduleHandler.importIssuer(params, file);
        if (responseFromModule.isSuccess() && !responseFromModule.getResult().get(IMPORTED_ISSUERS).isNull() && !responseFromModule.getResult().get(IMPORTED_ISSUERS).asJsonList().isEmpty()) {
            String issuerRef = responseFromModule.getResult().get(IMPORTED_ISSUERS).asJsonList().get(0).asString();
            responseFromModule = moduleHandler.getIssuerInfo(issuerRef, params);
            responseFromModule.getResult().set(CA_TYPE, isRoot ? ROOT : INTERMEDIATE);
            responseFromModule.getResult().set(ISSUER_NAME, idName);
            // TODO: deal with serial number and certificate object
            if (isRoot) {
                responseFromModule.getResult().set(CA_TYPE, ROOT);
            } else {
                responseFromModule.getResult().set(CA_TYPE, INTERMEDIATE);
            }
            return dbHandler.generateCA(issuerRef, idName, isRoot ? PKI : idName, null, null, null,
                    mergerJson(responseFromModule.getResult(), params));
        } else if (responseFromModule.isSuccess() && (responseFromModule.getResult().get(IMPORTED_ISSUERS).isNull() || responseFromModule.getResult().get(IMPORTED_ISSUERS).asJsonList().isEmpty())) {
            return JsonApiResponse.error("Certificate already imported");
        }
        return responseFromModule;
    }

    /**
     * Exports the given issuer to JSON
     *
     * @param issuerRef identifier of the issuer
     * @param params    parameters with the path
     * @return the issuer certificate as a file
     */
    public JsonApiResponse exportIssuer(String issuerRef, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.getIssuerInfo(issuerRef, params);
        if (responseFromModule.isSuccess() &&
                responseFromModule.getResult().has(CERTIFICATE) && responseFromModule.getResult().get(CERTIFICATE).isString()) {
            Json response = Json.object(CERTIFICATE, responseFromModule.getResult().get(CERTIFICATE).asString());
            return JsonApiResponse.result(response);
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
        JsonApiResponse responseFromModule = moduleHandler.getIssuerInfo(issuerRef, params);
        if (responseFromModule.isSuccess() &&
                responseFromModule.getResult().has(CERTIFICATE) && responseFromModule.getResult().get(CERTIFICATE).isString()) {
            Json response = Json.object();
            // TODO : range this formating elsewhere, probably not the right place
            response.at("Content-Type", "application/octet-stream");
            response.at("Content", responseFromModule.getResult().get(CERTIFICATE).asString());
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
        return moduleHandler.getRoles(params);
    }

    /**
     * Creates a new role
     *
     * @param name      identifier in the dbapi side
     * @param description description in the dbapi side
     * @param params      parameters with the path
     */
    public JsonApiResponse createRole(String name, String description, String certificateAuthorityId, Json params, Json bodyBase, Json bodyExtra) {
        logger.info("Creating role {} at path {} ...", name, params.get(PATH).asString());

        // Delete role in autocrypt : 1
        logger.debug("{} ({}/{}) : Creating role {} at path {} in autocrypt ...", AutoCryptEndpoints.CREATE_ROLE, 1, 2, name, params.get(PATH).asString());
        JsonApiResponse responseFromModule = moduleHandler.createRole(bodyBase, params);
        if (!responseFromModule.isSuccess()) {
            logger.error("{} ({}/{}) : Creating role {} at path {} in autocrypt failed", AutoCryptEndpoints.CREATE_ROLE, 1, 2, name, params.get(PATH).asString());
            return JsonApiResponse.error("Error creating role : " + responseFromModule.getError().toJson());
        }
        logger.info("{} ({}/{}) : Created role {} at path {} in autocrypt", AutoCryptEndpoints.CREATE_ROLE, 1, 2, name, params.get(PATH).asString());

        Json result = responseFromModule.getResult();
        convertRoleToDbapi(result);
        result.set(CERTIFICATE_AUTHORITY_ID, certificateAuthorityId);
        result.set(TTL, result.get(TTL).asInteger() / 3600 + "h");
        result = mergerJson(result, bodyExtra);

        // save creation into dbapi : 2
        logger.debug("{} ({}/{}) : Creating role {} at path {} in dbapi ...", AutoCryptEndpoints.CREATE_ROLE, 2, 2, name, params.get(PATH).asString());
        JsonApiResponse responseFromDbapi = dbHandler.createRole(name, description, certificateAuthorityId, result);
        if (!responseFromDbapi.isSuccess()) {
            logger.error("{} ({}/{}) : Creating role {} at path {} in dbapi failed", AutoCryptEndpoints.CREATE_ROLE, 2, 2, name, params.get(PATH).asString());
            return responseFromDbapi;
        }
        logger.info("{} ({}/{}) : Created role {} at path {} in dbapi", CREATE_ROLE, 2, 2, name, params.get(PATH).asString());

        logger.info("Created role {} at path {}", name, params.get(PATH).asString());

        return responseFromDbapi;

    }

    private static void convertRoleToDbapi(Json obj) {
        transformToDbapi(obj, OU, PROVINCE);
        jsonListToStringListAtJson(obj, COUNTRY);
        jsonListToStringListAtJson(obj, ORGANIZATION_UNIT);
        jsonListToStringListAtJson(obj, ORGANIZATION);
        jsonListToStringListAtJson(obj, STATE);
        jsonListToStringListAtJson(obj, LOCALITY);
        jsonListToStringListAtJson(obj, STREET_ADDRESS);
        jsonListToStringListAtJson(obj, POSTAL_CODE);
    }

    /**
     * Gets the information of the given role
     *
     * @param name   name of the role
     * @param params parameters with the path and name of role
     */
    public JsonApiResponse getRole(String name, Json params) {
        return moduleHandler.getRole(name, params);
    }

    /**
     * Deletes the given role
     *
     * @param name   name in the module/dbapi side
     * @param body   body of the request
     * @param params parameters with the path
     */
    public JsonApiResponse deleteRole(String name, Json body, Json params) {
        logger.info("Deleting role {} at path {} ...", name, params.get(PATH).asString());

        // Delete role in autocrypt : 1
        logger.debug("{} ({}/{}) : Deleting role {} at path {} in autocrypt ...", AutoCryptEndpoints.DELETE_ROLE, 1, 2, name, params.get(PATH).asString());
        JsonApiResponse responseFromModule = moduleHandler.deleteRole(name, body, params);
        if (!responseFromModule.isSuccess()) {
            logger.error("{} ({}/{}) : Deleting role {} at path {} in autocrypt failed", AutoCryptEndpoints.DELETE_ROLE, 1, 2, name, params.get(PATH).asString());
            return responseFromModule;
        }
        logger.info("{} ({}/{}) : Deleted role {} at path {} in autocrypt", AutoCryptEndpoints.DELETE_ROLE, 1, 2, name, params.get(PATH).asString());

        // save delete into dbapi : 2
        logger.debug("{} ({}/{}) : Deleting role {} at path {} in dbapi ...", AutoCryptEndpoints.DELETE_ROLE, 2, 2, name, params.get(PATH).asString());
        JsonApiResponse responseFromDbapi = dbHandler.deleteRole(name, params.get(PATH).asString(), responseFromModule.getResult());
        if (!responseFromDbapi.isSuccess()) {
            logger.error("{} ({}/{}) : Deleting role {} at path {} in dbapi failed", AutoCryptEndpoints.DELETE_ROLE, 2, 2, name, params.get(PATH).asString());
            return responseFromDbapi;
        }
        logger.info("{} ({}/{}) : Deleted role {} at path {} in dbapi", AutoCryptEndpoints.DELETE_ROLE, 2, 2, name, params.get(PATH).asString());

        logger.info("Deleted role {} at path {}", name, params.get(PATH).asString());

        return responseFromDbapi;
    }

    /**
     * Updates the information of the given role
     *
     * @param name        identifier in the module/dbapi side
     * @param description description in the dbapi
     * @param params      parameters with the path
     */
    public JsonApiResponse updateRole(String name, String description, String certificateAuthorityId, Json params, Json bodyBase, Json bodyExtra) {
        logger.info("Updating role {} at path {} ...", name, params.get(PATH).asString());

        // Update role in autocrypt : 1
        logger.debug("{} ({}/{}) : Updating role {} at path {} in autocrypt ...", AutoCryptEndpoints.UPDATE_ROLE, 1, 2, name, params.get(PATH).asString());
        JsonApiResponse responseFromModule = moduleHandler.updateRole(name, bodyBase, params);
        if (!responseFromModule.isSuccess()) {
            logger.error("{} ({}/{}) : Updating role {} at path {} in autocrypt failed", AutoCryptEndpoints.UPDATE_ROLE, 1, 2, name, params.get(PATH).asString());
            return responseFromModule;
        }
        logger.info("{} ({}/{}) : Updated role {} at path {} in autocrypt", AutoCryptEndpoints.UPDATE_ROLE, 1, 2, name, params.get(PATH).asString());

        convertRoleToDbapi(responseFromModule.getResult());
        responseFromModule.getResult().set(TTL, responseFromModule.getResult().get(TTL).asInteger() / 3600 + "h");

        // save update into dbapi : 2
        logger.debug("{} ({}/{}) : Updating role {} at path {} in dbapi ...", AutoCryptEndpoints.UPDATE_ROLE, 2, 2, name, params.get(PATH).asString());
        JsonApiResponse responseFromDbapi = dbHandler.updateRole(name, description, certificateAuthorityId, params.get(PATH).asString(),
                mergerJson(responseFromModule.getResult(), bodyExtra));
        if (!responseFromDbapi.isSuccess()) {
            logger.error("{} ({}/{}) : Updating role {} at path {} in dbapi failed", AutoCryptEndpoints.UPDATE_ROLE, 2, 2, name, params.get(PATH).asString());
            return responseFromDbapi;
        }
        logger.info("{} ({}/{}) : Updated role {} at path {} in dbapi", AutoCryptEndpoints.UPDATE_ROLE, 2, 2, name, params.get(PATH).asString());

        logger.info("Updated role {} at path {}", name, params.get(PATH).asString());

        return responseFromDbapi;
    }

    /**
     * Activates the Online Certificate Status Protocol (OCSP)
     *
     * @param body   body of the request
     * @param params parameters with the path
     */
    public JsonApiResponse activateOCSP(Json body, Json params) {
        return moduleHandler.activateOCSP(body, params);
    }

    /**
     * Validate the template of a certificate
     *
     * @param body   body of the request
     * @param params parameters with the path and role
     */
    public JsonApiResponse validateTemplate(Json body, Json params) {
        return moduleHandler.validateTemplate(body, params);
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
        logger.debug("{} ({}/{}) : generating certificate in Autocrypt ...", AutoCryptEndpoints.GENERATE_CERTIFICATE, 1, 2);
        JsonApiResponse responseFromModule = moduleHandler.generateCertificate(bodyBase, params);
        if (!responseFromModule.isSuccess() ||
                !responseFromModule.getResult().has(SERIAL_NUMBER) ||
                !responseFromModule.getResult().get(SERIAL_NUMBER).isString()) {
            logger.error("{} ({}/{}) : certificate creation in Autocrypt failed ", AutoCryptEndpoints.GENERATE_CERTIFICATE, 1, 2);
            return JsonApiResponse.error("Error creating the certificate : " + responseFromModule.getError().toJson());
        }
        logger.info("{} ({}/{}) : certificate ({}) created in Autocrypt", AutoCryptEndpoints.GENERATE_CERTIFICATE, 1, 2, responseFromModule.getResult().get(SERIAL_NUMBER).asString());

        String serialNumber = responseFromModule.getResult().get(SERIAL_NUMBER).asString();

        // saving certificate in dbapi : 2
        logger.debug("{} ({}/{}) : saving certificate in Dbapi ...", AutoCryptEndpoints.GENERATE_CERTIFICATE, 2, 2);
        JsonApiResponse responseFromDbapi = dbHandler.generateCertificate(
                serialNumber,
                name,
                vaultRoleId,
                description,
                params.get(PATH).asString(),
                mergerJson(responseFromModule.getResult(), bodyExtra));
        if (responseFromDbapi.isSuccess()) {
            logger.info("{} ({}/{}) : certificate ({}) saved in Dbapi", AutoCryptEndpoints.GENERATE_CERTIFICATE, 2, 2, serialNumber);
        } else {
            logger.error("{} ({}/{}) : saving certificate ({}) in Dbapi failed", AutoCryptEndpoints.GENERATE_CERTIFICATE, 2, 2, serialNumber);
            return responseFromDbapi;
        }

        logger.info("Successfully generated the certificate {}", serialNumber);

        return responseFromDbapi;
    }

    /**
     * Gives the list of certificates
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getCertificates(Json params) {
        return moduleHandler.getCertificates(params);
    }

    /**
     * Gives the information of the given certificate
     *
     * @param serialNumber identifier of the certificate
     * @param params       parameters with the serialNumber
     */
    public JsonApiResponse getCertificateInfo(String serialNumber, Json params) {
        JsonApiResponse response = moduleHandler.getCertificateInfo(serialNumber, params);

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
            response = moduleHandler.getCertificateWithPrivateKey(serialNumber, params);
        } else {
            logger.debug("{} : Fetching certificate {} at path {} without private key ...", AutoCryptEndpoints.GET_CERTIFICATE, serialNumber, params.get("path").asString());
            response = moduleHandler.getCertificateWithoutPrivateKey(serialNumber, params);
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

        JsonApiResponse responseFromModule = moduleHandler.downloadCertificate(serialNumber, params);
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

        // Revoke from autocrypt : 1
        logger.debug("{} ({}/{}) : revoking certificate {} at path {} in Autocrypt ...", AutoCryptEndpoints.REVOKE_CERTIFICATE, 1, 2, serialNumber, path);
        JsonApiResponse responseFromModule = moduleHandler.revokeCertificate(serialNumber, params);
        if (!responseFromModule.isSuccess()) {
            logger.error("{} ({}/{}) : failed to revoked certificate {} at path {} in Autocrypt", AutoCryptEndpoints.REVOKE_CERTIFICATE, 1, 2, serialNumber, path);
            return responseFromModule;
        }
        logger.info("{} ({}/{}) : revoked certificate {} at path {} in Autocrypt", AutoCryptEndpoints.REVOKE_CERTIFICATE, 1, 2, serialNumber, path);

        // Revoke certificate at dbapi : 2
        logger.debug("{} ({}/{}) : revoking certificate {} at path {} in Dbapi ...", AutoCryptEndpoints.REVOKE_CERTIFICATE, 2, 2, serialNumber, path);
        JsonApiResponse responseFromDbapi = dbHandler.revokeCertificate(serialNumber, responseFromModule.getResult());
        if (!responseFromDbapi.isSuccess()) {
            logger.error("{} ({}/{}) : failed to revoked certificate {} at path {} in Dbapi", AutoCryptEndpoints.REVOKE_CERTIFICATE, 2, 2, serialNumber, path);
        }
        logger.info("{} ({}/{}) : revoked certificate {} at path {} in Dbapi", AutoCryptEndpoints.REVOKE_CERTIFICATE, 2, 2, serialNumber, path);

        logger.info("Revoked certificate {} at path {}", serialNumber, path);

        return responseFromDbapi;
    }

    /**
     * Delete all the revoked certificates
     */
    public JsonApiResponse deleteRevokedCertificates() {
        JsonApiResponse responseFromModule = moduleHandler.deleteRevokedCertificates();
        if (responseFromModule.isSuccess()) {
            return dbHandler.deleteRevokedCertificates();
        } else {
            return responseFromModule;
        }
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
     * @param name        identifier of the CA (issuerName)
     * @param description description in the dbapi
     * @param params      parameters with  path
     * @param bodyAutocrypt  body for creating the CA in CSL-Autocrypt
     * @param bodyDBapi      body for creating the CA in CSL-Dbapi
     * @return if creation was successful, the body for HMI, otherwise, the error message
     */
    private JsonApiResponse generateCA(AutoCryptEndpoints typeCA, String name, String description, Json params, Json bodyAutocrypt, Json bodyDBapi) {
        boolean isRoot = typeCA==AutoCryptEndpoints.GENERATE_ROOT_CA;
        String type = isRoot?"root":"intermediate";
        String path = params.get(PATH).asString();
        logger.info("Generating {} CA ...", type);

        // Creating CA in Autocrypt : 1
        logger.debug("{} ({}/{}) : creating {} CA creation in Autocrypt at path {} ...", typeCA, 1, 4, type, path);
        JsonApiResponse responseFromModule;
        if (isRoot) {
            responseFromModule = moduleHandler.generateRootCA(bodyAutocrypt, params);
        } else {
            responseFromModule = moduleHandler.generateIntermediateCA(bodyAutocrypt, params);
        }
        if (!responseFromModule.isSuccess() ||
                !responseFromModule.getResult().has(ISSUER_REF) ||
                !responseFromModule.getResult().get(ISSUER_REF).isString()) {
            logger.error("{} ({}/{}) : {} CA creation at path {} in Autocrypt failed", typeCA, 1, 4, type, path);
            return JsonApiResponse.error("Error creating the CA : " + responseFromModule.getError().toJson());
        }
        String issuerRef = responseFromModule.getResult().get(ISSUER_REF).asString();
        logger.info("{} ({}/{}) : {} CA ({}) created in Autocrypt at path {}", typeCA, 1, 4, type, issuerRef, path);

        String serialNumber = responseFromModule.getResult().get(SERIAL_NUMBER).asString();
        bodyDBapi = mergerJson(responseFromModule.getResult(), bodyDBapi);

        // Get issuer info from Autocrypt
        logger.debug("{} ({}/{}) : gathering issuer ({}) information from AutoCrypt ...", typeCA, 2, 4, issuerRef);
        responseFromModule = moduleHandler.getIssuerInfo(issuerRef, params);
        if (!responseFromModule.isSuccess()) {
            logger.error("{} ({}/{}) : gathering issuer ({}) information from AutoCrypt failed", typeCA, 2, 4, issuerRef);
            return responseFromModule;
        }
        logger.info("{} ({}/{}) : {} issuer ({}) information gathered from AutoCrypt", typeCA, 2, 4, type, issuerRef);

        bodyDBapi = mergerJson(responseFromModule.getResult(), bodyDBapi);

        // Get the certificate of CA from Autocrypt : 3
        logger.debug("{} ({}/{}) : fetching certificate ({}) of {} issuer ({}) ...", typeCA, 3, 4, serialNumber, issuerRef, type);
        JsonApiResponse responseWithCertificate = moduleHandler.getCertificateInfo(serialNumber, Json.object(PATH, PKI));
        if (!responseWithCertificate.isSuccess()) {
            logger.error("{} ({}/{}) : fetching certificate ({}) of {} issuer ({}) from AutoCrypt failed", typeCA, 3, 4, serialNumber, type, issuerRef);
            return responseWithCertificate;
        }
        logger.info("{} ({}/{}) : certificate ({}) of {} issuer ({}) fetched from AutoCrypt", typeCA, 3, 4, serialNumber, type, issuerRef);

        responseWithCertificate.getResult().set(SERIAL_NUMBER, serialNumber);
        responseWithCertificate.getResult().set(PATH, PKI);

        // Save CA into dbapi : 4
        logger.debug("{} ({}/{}) : saving {} CA ({}) in Dbapi ...", typeCA, 4, 4, type, issuerRef);
        JsonApiResponse responseFromDbapi = dbHandler.generateCA(issuerRef, name, path, description, serialNumber, responseWithCertificate.getResult(),
                bodyDBapi);

        if (!responseFromDbapi.isSuccess()) {
            logger.error("{} ({}/{}) : saving {} CA ({}) in Dbapi failed", typeCA, 4, 4, type, issuerRef);
        }
        logger.info("{} ({}/{}) : {} CA ({}) saved in Dbapi", typeCA, 4, 4, type, issuerRef);

        logger.info("{} CA was successfully generated with id {} and certificate number {}", type.substring(0, 1).toUpperCase() + type.substring(1), issuerRef, serialNumber);

        return responseFromDbapi;
    }

    /**
     * Verifies if the module api is reachable
     *
     * @return whether it is reachable
     */
    public Json getStatus() {
//        return Json.object();
        return moduleHandler.getStatus();
    }

    /**
     * Changes the saving to Db
     *
     * @param shouldSaveToDb whether data should save into the DB
     */
    public void setSaveToDb(boolean shouldSaveToDb) {
        this.shouldSaveToDb = shouldSaveToDb;
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

    /**
     * Initial synchronisation for the issuers/ca (intermediate ca)
     *
     * @param path to synchronize the issuers
     */
    private void synchronizeIssuers(String path) {
        Json body = Json.object(PATH, path);
        JsonApiResponse issuers = moduleHandler.getIssuers(body);
        if (issuers.isSuccess()) {
            for (Json issuer_ref : issuers.getResult().asJsonList()) {
                Json body2 = Json.read(body.toString());
                body2.at(ISSUER_REF, issuer_ref.asString());
                JsonApiResponse issuer = moduleHandler.getIssuerInfo(issuer_ref.asString(), body2);
                if (issuer.isSuccess()) {
                    dbHandler.generateIntermediateCA(issuer_ref.asString(), null, issuer.getResult());
                }
            }
        }
    }

    /**
     * Initial synchronisation for the roles
     *
     * @param path to synchronize the roles
     */
    private void synchronizeRoles(String path) {
        Json body = Json.object(PATH, path);
        JsonApiResponse issuers = moduleHandler.getRoles(body);
        if (issuers.isSuccess()) {
            for (Json role_name : issuers.getResult().asJsonList()) {
                Json body2 = Json.read(body.toString());
                body2.at(NAME, role_name.asString());
                JsonApiResponse role = moduleHandler.getRole(role_name.asString(), body2);
                if (role.isSuccess()) {
                    dbHandler.createRole(role_name.asString(), null, null, role.getResult());
                }
            }
        }
    }

    /**
     * Initial synchronisation for the certificates
     *
     * @param path to synchronize the certificates
     */
    private void synchronizeCertificate(String path) {
        Json body = Json.object(PATH, path);
        JsonApiResponse certificates = moduleHandler.getCertificates(body);
        if (certificates.isSuccess()) {
            for (Json serial_number : certificates.getResult().asJsonList()) {
                Json body2 = Json.read(body.toString());
                body2.at(SERIAL_NUMBER, serial_number.asString());
                body2.at(PATH, path);
                JsonApiResponse certificate = moduleHandler.getCertificateInfo(serial_number.asString(), body2);
                if (certificate.isSuccess()) {
                    dbHandler.generateCertificate(serial_number.asString(), null, certificate.getResult());
                }
            }
        }
    }

    /**
     * Initial synchronisation: pki path par default (only one), db is empty so we only have to create
     */
    public void initialSynchronizeDb(String path) {
        synchronizeIssuers(path);
        synchronizeRoles(path);
        synchronizeCertificate(path);
    }
}
