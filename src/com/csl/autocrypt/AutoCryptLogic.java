package com.csl.autocrypt;

import com.ucsl.json.Json;
import main.services.JsonApiResponse;

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
     * @param issuerRef identifier of the issuer
     * @param body      body of the request
     * @param params    parameters of the request
     */
    public JsonApiResponse updateIssuerInfo(String issuerRef, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.updateIssuerInfo(issuerRef, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::updateIssuerInfo, issuerRef, responseFromModule);
    }

    /**
     * Deletes the given issuer from the module and the DB
     *
     * @param body parameters with the path and the issuer id
     */
    public JsonApiResponse deleteIssuer(String issuerRef, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.deleteIssuer(issuerRef, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::deleteIssuer, issuerRef, responseFromModule);
    }

    /**
     * Imports a new certificate
     *
     * @param body parameters with the path and the file
     */
    public JsonApiResponse importCertificate(Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.importCertificate(body, params);
        return sendToDbApiIfSaveToDb(dbHandler::importCertificate, responseFromModule);
        // TODO : verify this import
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
     * @param body   body with the information
     * @param params parameters with the path
     */
    public JsonApiResponse createRole(Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.createRole(body, params);
        return sendToDbApiIfSaveToDb(dbHandler::createRole, responseFromModule);
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
     * @param body parameters with the path and name of role
     */
    public JsonApiResponse deleteRole(String name, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.deleteRole(name, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::deleteRole, "id", responseFromModule); // TODO : get ID
    }

    /**
     * Updates the information of the given role
     *
     * @param body parameters with the path and name of role, others?
     */
    public JsonApiResponse updateRole(String name, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.updateRole(name, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::updateRole, "id", responseFromModule);
        // TODO : find id
    }

    /**
     * Activates the Online Certificate Status Protocol (OCSP)
     *
     * @param body parameters with the path and name of role, others?
     */
    public JsonApiResponse activateOCSP(Json body, Json params) {
        return moduleHandler.activateOCSP(body, params);
    }

    /**
     * Generates a certificates at the given path and role
     *
     * @param body parameters with the path and role
     */
    public JsonApiResponse generateCertificate(Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.generateCertificate(body, params);
        return sendToDbApiIfSaveToDb(dbHandler::generateCertificate, responseFromModule);
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
     * Revokes the given certificate
     *
     * @param serialNumber identifier of the certificate
     * @param params       parameters with the path
     */
    public JsonApiResponse revokeCertificate(String serialNumber, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.revokeCertificate(serialNumber, params);
        return sendToDbApiIfSaveToDb(dbHandler::revokeCertificate, "id", responseFromModule);
        // TODO : get ID
    }

    /**
     * Generate root CA
     *
     * @param body   body of the request with commonName, ttl, and optionally othes
     * @param params parameters with  path
     */
    public JsonApiResponse generateRootCA(Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.generateRootCA(body, params);
        return sendToDbApiIfSaveToDb(dbHandler::generateRootCA, responseFromModule);
    }

    /**
     * Generate intermediate CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public JsonApiResponse generateIntermediateCA(Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.generateIntermediateCA(body, params);
        return sendToDbApiIfSaveToDb(dbHandler::generateIntermediateCA, responseFromModule);
    }

    /**
     * Verifies if the module api is reachable
     *
     * @return whether it is reachable
     */
    public Json getStatus() {
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
    public JsonApiResponse sendToDbApiIfSaveToDb(IJsonApiResponser method, JsonApiResponse responseFromModule) {
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            return method.apply(responseFromModule.getResult());
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
    public JsonApiResponse sendToDbApiIfSaveToDb(IJsonApiResponser2params method, String id, JsonApiResponse responseFromModule) {
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            return method.apply(id, responseFromModule.getResult());
        } else {
            return responseFromModule;
        }
    }
}
