package com.csl.autocrypt;

import com.ucsl.json.Json;
import main.services.JsonApiResponse;

import static com.csl.autocrypt.ConvertDapiVault.transformToDbapi;
import static com.csl.autocrypt.enums.AutocryptConstants.*;
import static com.csl.autocrypt.outils.JsonHelper.*;

public class AutoCryptLogic {
    private final ApiHandlerForCSLAutoCrypt moduleHandler;
    private final DbapiHandlerForCSLAutoCrypt dbHandler;
    private boolean shouldSaveToDb = true;

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
        JsonApiResponse oldValuesFromDbapi = dbHandler.getInfoIssuerFromDbapi(issuerRef);

        if (!oldValuesFromDbapi.isSuccess()) {
            return oldValuesFromDbapi;
        }

        JsonApiResponse responseFromModule = moduleHandler.updateIssuerInfo(issuerRef, bodyBase, params);
//            responseFromModule.getResult().set(PATH, PKI);
//            responseFromModule.getResult().set(CA_TYPE, ROOT);
//        transformToDbapi(responseFromModule.getResult(), CRL_DISTRIBUTION_POINTS);
//        transformToDbapi(responseFromModule.getResult(), OCSP_SERVERS);

        return sendToDbApiIfSaveToDb(dbHandler::updateIssuerInfo, name, issuerRef, description, params.get(PATH).asString(),
                JsonApiResponse.result(mergerJson(responseFromModule.getResult(),
                        mergerJson(bodyExtra, oldValuesFromDbapi.getResult()))
                )
        );
    }

    /**
     * Deletes the given issuer from the module and the DB
     *
     * @param issuerRef identifier in the module side
     * @param body      body of the request
     * @param params    parameters with the path
     */
    public JsonApiResponse deleteIssuer(String issuerRef, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.deleteIssuer(issuerRef, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::deleteIssuer, issuerRef, responseFromModule);
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
     * @return whether the delete was successful
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
                return dbHandler.generateRootCA(issuerRef, idName, null,null, null,
                        mergerJson(responseFromModule.getResult(), params));
            } else {
                responseFromModule.getResult().set(CA_TYPE, INTERMEDIATE);
                return dbHandler.generateIntermediateCA(issuerRef, idName, null, null, null,
                        mergerJson(responseFromModule.getResult(), params));
            }
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
     * @param idName      identifier in the dbapi side
     * @param description description in the dbapi side
     * @param params      parameters with the path
     */
    public JsonApiResponse createRole(String idName, String description, String certificateAuthorityId, Json params, Json bodyBase, Json bodyExtra) {
        JsonApiResponse responseFromModule = moduleHandler.createRole(bodyBase, params);
        if (responseFromModule.isSuccess()) {

            Json result = responseFromModule.getResult();
            convertRoleToDbapi(result);
            result.set(CERTIFICATE_AUTHORITY_ID, certificateAuthorityId);
            result.set(TTL, result.get(TTL).asInteger()/3600+"h");
            result.set(TTL_UNIT, "days");
            result = mergerJson(result, bodyExtra);
            return sendToDbApiIfSaveToDb(dbHandler::createRole, idName, description, certificateAuthorityId, JsonApiResponse.result(result));
        } else {

            return JsonApiResponse.error("Error creating role : " + responseFromModule.getError().toJson());
        }
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
        JsonApiResponse responseFromModule = moduleHandler.deleteRole(name, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::deleteRole, name, params.get(PATH).asString(), responseFromModule);
    }

    /**
     * Updates the information of the given role
     *
     * @param name        identifier in the module/dbapi side
     * @param description description in the dbapi
     * @param params      parameters with the path
     */
    public JsonApiResponse updateRole(String name, String description, String certificateAuthorityId, Json params, Json bodyBase, Json bodyExtra) {
        JsonApiResponse responseFromModule = moduleHandler.updateRole(name, bodyBase, params);
        // TODO : change this
        if (responseFromModule.isSuccess()) {
            convertRoleToDbapi(responseFromModule.getResult());
            responseFromModule.getResult().set(TTL, responseFromModule.getResult().get(TTL).asInteger()/3600+"h");
            return sendToDbApiIfSaveToDb(dbHandler::updateRole, name, description, certificateAuthorityId, params.get(PATH).asString(),
                    JsonApiResponse.result(mergerJson(responseFromModule.getResult(), bodyExtra)));
        } else {
            return responseFromModule;
        }
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
        JsonApiResponse responseFromModule = moduleHandler.generateCertificate(bodyBase, params);
        if (responseFromModule.isSuccess() &&
                responseFromModule.getResult().has(SERIAL_NUMBER) &&
                responseFromModule.getResult().get(SERIAL_NUMBER).isString()) {
            return dbHandler.generateCertificate(
                    responseFromModule.getResult().get(SERIAL_NUMBER).asString(),
                    name,
                    vaultRoleId,
                    description,
                    params.get(PATH).asString(),
                    mergerJson(responseFromModule.getResult(), bodyExtra));
        } else {
            return JsonApiResponse.error("Error creating the certificate : " + responseFromModule.getError().toJson());
        }
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
        return moduleHandler.getCertificateInfo(serialNumber, params);
    }

    /**
     * Get the given certificate
     *
     * @param serialNumber identifier of the certificate
     * @param needPrivateKey whether we need the private key
     * @param params       parameters with the serialNumber
     */
    public JsonApiResponse getCertificate(String serialNumber, boolean needPrivateKey, Json params) {
        if (needPrivateKey) {
            return moduleHandler.getCertificateWithPrivateKey(serialNumber, params);
        } else {
            return moduleHandler.getCertificateWithoutPrivateKey(serialNumber, params);
        }
    }

    /**
     * Downloads the given certificate
     *
     * @param serialNumber identifier of the certificate
     * @param params       parameters with the serialNumber
     */
    public JsonApiResponse downloadCertificate(String serialNumber, Json params) {
        return moduleHandler.downloadCertificate(serialNumber, params);
    }

    /**
     * Revokes the given certificate
     *
     * @param serialNumber identifier of the certificate on the module side
     * @param params       parameters with the path
     */
    public JsonApiResponse revokeCertificate(String serialNumber, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.revokeCertificate(serialNumber, params);
        return sendToDbApiIfSaveToDb(dbHandler::revokeCertificate, serialNumber, responseFromModule);
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
        JsonApiResponse responseFromModule = moduleHandler.generateRootCA(bodyBase, params);
        if (responseFromModule.isSuccess() &&
                responseFromModule.getResult().has(ISSUER_REF) &&
                responseFromModule.getResult().get(ISSUER_REF).isString()) {
            bodyExtra = mergerJson(responseFromModule.getResult(), bodyExtra);
            String serialNumber = responseFromModule.getResult().get(SERIAL_NUMBER).asString();
            String issuerRef = responseFromModule.getResult().get(ISSUER_REF).asString();
            responseFromModule = moduleHandler.getIssuerInfo(issuerRef, Json.object(PATH, PKI));
//            responseFromModule.getResult().set(PATH, PKI);
//            responseFromModule.getResult().set(CA_TYPE, ROOT);
            transformToDbapi(responseFromModule.getResult(), CRL_DISTRIBUTION_POINTS, OCSP_SERVERS);

            // save to dbapi the certificate of ca
            params =Json.object(PATH, PKI);
            responseFromModule = moduleHandler.getCertificateInfo(serialNumber, params);
            if (!responseFromModule.isSuccess()) {
                return responseFromModule;
            }
            responseFromModule.getResult().set(SERIAL_NUMBER,serialNumber);
            bodyExtra.set(CERTIFICATE_OBJECT, responseFromModule.getResult());
            bodyExtra.set(CERTIFICATE_OBJECT, responseFromModule.getResult());

            return dbHandler.generateRootCA(issuerRef, name, description, serialNumber, responseFromModule.getResult(),
                    mergerJson(responseFromModule.getResult(), bodyExtra));

        } else {
            return JsonApiResponse.error("Error creating the CA: " + responseFromModule.getError().toJson());
        }
    }

    /**
     * Generate intermediate CA
     *
     * @param idName      identifier in the dbapi side
     * @param description description in the dbapi
     * @param params      parameters with  path
     */
    public JsonApiResponse generateIntermediateCA(String idName, String description, Json params, Json bodyBase, Json bodyExtra) {
        JsonApiResponse responseFromModule = moduleHandler.generateIntermediateCA(bodyBase, params);

        if (!responseFromModule.isSuccess() ||
                !responseFromModule.getResult().has(ISSUER_REF) ||
                !responseFromModule.getResult().get(ISSUER_REF).isString()) {
            return JsonApiResponse.error("Error creating the CA : " + responseFromModule.getError().toJson());
        }

        bodyExtra = mergerJson(responseFromModule.getResult(), bodyExtra);
        String issuerRef = responseFromModule.getResult().get(ISSUER_REF).asString();
        responseFromModule = moduleHandler.getIssuerInfo(issuerRef, params);

        transformToDbapi(responseFromModule.getResult(), CRL_DISTRIBUTION_POINTS);
        transformToDbapi(responseFromModule.getResult(), OCSP_SERVERS);

        // save to dbapi the certificate of ca
        params.set(PATH, PKI);
        String serialNumber = responseFromModule.getResult().get(SERIAL_NUMBER).asString();
        responseFromModule = moduleHandler.getCertificateInfo(serialNumber, params);
        if (!responseFromModule.isSuccess()) { return responseFromModule; }

        bodyExtra.set(CERTIFICATE_OBJECT, responseFromModule.getResult());

        return dbHandler.generateIntermediateCA(issuerRef, idName, description, serialNumber, responseFromModule.getResult(),
                mergerJson(responseFromModule.getResult(), bodyExtra));
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
