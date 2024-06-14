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
     * @param id identifier in the dbapi side
     * @param name name for dbapi side
     * @param issuerRef identifier of the issuer (module side)
     * @param body      body of the request
     * @param params    parameters of the request
     */
    public JsonApiResponse updateIssuerInfo(int id, String name, String issuerRef, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.updateIssuerInfo(issuerRef, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::updateIssuerInfo, id, name, responseFromModule);
    }

    /**
     * Deletes the given issuer from the module and the DB
     *
     * @param id identifier in the dbapi db
     * @param name name in the dbapi side
     * @param issuerRef identifier in the module side
     * @param body body of the request
     * @param params parameters with the path
     */
    public JsonApiResponse deleteIssuer(int id, String name, String issuerRef, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.deleteIssuer(issuerRef, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::deleteIssuer, id, name, responseFromModule);
    }

    /**
     * Imports a new certificate
     *
     * @param idName identifier in the dbapi side
     * @param body body for the request
     * @param params parameters with the path and the file
     */
    public JsonApiResponse importCertificate(String idName, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.importCertificate(body, params);
        return sendToDbApiIfSaveToDb(dbHandler::importCertificate, idName, responseFromModule);
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
     * @param idName  identifier in the dbapi side
     * @param body   body with the information
     * @param params parameters with the path
     */
    public JsonApiResponse createRole(String idName, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.createRole(body, params);
        return sendToDbApiIfSaveToDb(dbHandler::createRole, idName, responseFromModule);
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
     * @param id identifier in the module/dbapi
     * @param name name in the module/dbapi side
     * @param body body of the request
     * @param params parameters with the path
     */
    public JsonApiResponse deleteRole(int id, String name, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.deleteRole(name, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::deleteRole, id, name, responseFromModule);
    }

    /**
     * Updates the information of the given role
     *
     * @param name identifier in the module/dbapi side
     * @param body parameters with the path and name of role, others?
     * @param params parameters with the path
     */
    public JsonApiResponse updateRole(int id, String name, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.updateRole(name, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::updateRole, id, name, responseFromModule);
    }

    /**
     * Activates the Online Certificate Status Protocol (OCSP)
     *
     * @param body body of the request
     * @param params parameters with the path
     */
    public JsonApiResponse activateOCSP(Json body, Json params) {
        return moduleHandler.activateOCSP(body, params);
    }

    /**
     * Validate the template of a certificate
     *
     * @param body body of the request
     * @param params parameters with the path and role
     */
    public JsonApiResponse validateTemplate(Json body, Json params) {
        return moduleHandler.validateTemplate(body, params);
    }

    /**
     * Generates a certificates at the given path and role
     *
     * @param idName identifier in the dbapi side
     * @param body body of the request
     * @param params parameters with the path and role
     */
    public JsonApiResponse generateCertificate(String idName, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.generateCertificate(body, params);
        return sendToDbApiIfSaveToDb(dbHandler::generateCertificate, idName, responseFromModule);
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
     * @param id identifier in the dbapi db
     * @param name  name in the dbapi side
     * @param serialNumber identifier of the certificate on the module side
     * @param params       parameters with the path
     */
    public JsonApiResponse revokeCertificate(int id, String name, String serialNumber, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.revokeCertificate(serialNumber, params);
        return sendToDbApiIfSaveToDb(dbHandler::revokeCertificate, id, name, responseFromModule);
    }

    /**
     * Generate root CA
     *
     * @param idName identifier in the dbapi side
     * @param body   body of the request with commonName, ttl, and optionally others
     * @param params parameters with  path
     */
    public JsonApiResponse generateRootCA(String idName, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.generateRootCA(body, params);
        return sendToDbApiIfSaveToDb(dbHandler::generateRootCA, idName, responseFromModule);
    }

    /**
     * Generate intermediate CA
     *
     * @param idName identifier in the dbapi side
     * @param body   body of the request with commonName, ttl, and optionally others
     * @param params parameters with  path
     */
    public JsonApiResponse generateIntermediateCA(String idName, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.generateIntermediateCA(body, params);
        return sendToDbApiIfSaveToDb(dbHandler::generateIntermediateCA, idName, responseFromModule);
    }

    /**
     * Verifies if the module api is reachable
     *
     * @return whether it is reachable
     */
    public Json getStatus() {
        return Json.object();
//        return  moduleHandler.getStatus();
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
            nextRequestBody = (nextRequestBody!=null) ? nextRequestBody : Json.object();
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
    public JsonApiResponse sendToDbApiIfSaveToDb(IJsonApiResponserWithId method, int id, String name, JsonApiResponse responseFromModule) {
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            Json nextRequestBody = responseFromModule.getResult();
            nextRequestBody = (nextRequestBody!=null) ? nextRequestBody : Json.object();
            return method.apply(id, name, nextRequestBody);
        } else {
            return responseFromModule;
        }
    }
}
