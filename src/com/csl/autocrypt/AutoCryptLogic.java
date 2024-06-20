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
    public JsonApiResponse updateIssuerInfo(Integer id, String name, String description, String issuerRef, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.updateIssuerInfo(issuerRef, body, params);
        // TODO : change output of update
        params.at("issuer_ref", issuerRef);
        responseFromModule = moduleHandler.getIssuerInfo(issuerRef, params);
        return sendToDbApiIfSaveToDb(dbHandler::updateIssuerInfo, issuerRef, name, description, name, responseFromModule);
    }

    /**
     * Updates the information of the given issuer in the module and the DB
     *
     * @param name name for dbapi side
     * @param issuerRef identifier of the issuer (module side)
     * @param body      body of the request
     * @param params    parameters of the request
     */
    public JsonApiResponse updateIssuerInfo(String issuerRef, String name, String description, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.updateIssuerInfo(issuerRef, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::updateIssuerInfo, issuerRef, name, description, name, responseFromModule);
    }

    /**
     * Deletes the given issuer from the module and the DB
     *
     * @param id identifier in the dbapi side
     * @param name name in the dbapi side
     * @param issuerRef identifier in the module side
     * @param body body of the request
     * @param params parameters with the path
     */
    public JsonApiResponse deleteIssuer(int id, String name, String issuerRef, Json body, Json params) {
        // delete issuer
        JsonApiResponse responseFromModule = moduleHandler.deleteIssuer(issuerRef, body, params);
        JsonApiResponse responseFromDbapi = sendToDbApiIfSaveToDb(dbHandler::deleteIssuer, issuerRef, name, responseFromModule);
        // delete roles of the issuer (one intermediate issuer per path)
        deleteRolesOfPath(id, name, params, responseFromDbapi);
        // delete certificates of the issuer (one intermediate issuer per path)
        revokeCertificatesOfPath(params, responseFromDbapi);
        return JsonApiResponse.success();

    }

    private void deleteRolesOfPath(int id, String name, Json params, JsonApiResponse responseFromDbapi) {
        if (responseFromDbapi.isSuccess()) {
            JsonApiResponse rolesToDeleteModule = moduleHandler.getRoles(params);
            JsonApiResponse rolesToDeleteDbapi = dbHandler.listRoles();
            if (rolesToDeleteModule.isSuccess() && rolesToDeleteDbapi.isSuccess())
            {
                for (Json roleDbapi : rolesToDeleteDbapi.getResult()) {
                    if (roleDbapi.has("certificate_authority_id") && roleDbapi.get("certificate_authority_id").isNumber() && roleDbapi.get("certificate_authority_id").asInteger() == id &&
                            roleDbapi.has("name") && roleDbapi.get("name").isString() && roleDbapi.get("name").asString() == name) {
                        dbHandler.deleteRole(roleDbapi.get("id").asString(), null, null);
                    }
                }
                for (Json roleDbapi : rolesToDeleteModule.getResult()) {
                    moduleHandler.deleteRole(roleDbapi.asString(), null, params);
                }
            }
        }
    }

    private void revokeCertificatesOfPath(Json params, JsonApiResponse responseFromDbapi) {
        if (responseFromDbapi.isSuccess()) {
            JsonApiResponse certificatesToRevoke = moduleHandler.getCertificates(params);
            for (Json cert : certificatesToRevoke.getResult()) {
                moduleHandler.revokeCertificate(cert.asString(), params);
                dbHandler.revokeCertificate(cert.asString(), params);
            }
        }
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
        String issuerRef= null;
        if (responseFromModule.isSuccess()) {
            issuerRef = responseFromModule.getResult().get("imported_issuers").asJsonList().get(0).asString();
            responseFromModule = moduleHandler.getIssuerInfo(issuerRef, params);
        }
        return sendToDbApiIfSaveToDb(dbHandler::generateRootCA, issuerRef, idName, null, responseFromModule);
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
     * @param description  description in the dbapi side
     * @param body   body with the information
     * @param params parameters with the path
     */
    public JsonApiResponse createRole(String idName, String description, String certificateAuthorityId, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.createRole(body, params);
        return sendToDbApiIfSaveToDb(dbHandler::createRole, idName, description, certificateAuthorityId, responseFromModule);
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
    public JsonApiResponse deleteRole(Integer id, String name, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.deleteRole(name, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::deleteRole, id.toString(), name, responseFromModule);
    }
    /**
     * Deletes the given role
     *
     * @param id identifier in the module/dbapi
     * @param name name in the module/dbapi side
     * @param body body of the request
     * @param params parameters with the path
     */
    public JsonApiResponse deleteRole(String id, String name, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.deleteRole(name, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::deleteRole, id, name, responseFromModule);
    }

    /**
     * Updates the information of the given role
     *
     * @param name identifier in the module/dbapi side
     * @param description description in the dbapi
     * @param body parameters with the path and name of role, others?
     * @param params parameters with the path
     */
    public JsonApiResponse updateRole(Integer id, String name, String description, String certificateAuthorityId, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.updateRole(name, body, params);
        // TODO : change this
        params.at("name", name);
        responseFromModule = moduleHandler.getRole(name, params);
        return sendToDbApiIfSaveToDb(dbHandler::updateRole, id.toString(), name, description, certificateAuthorityId, responseFromModule);
    }

    /**
     * Updates the information of the given role
     *
     * @param name identifier in the module/dbapi side
     * @param description description in the dbapi
     * @param body parameters with the path and name of role, others?
     * @param params parameters with the path
     */
    public JsonApiResponse updateRole(String id, String name, String description, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.updateRole(name, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::updateRole, id, name, description, null, responseFromModule);
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
     * @param name identifier in the dbapi side
     * @param body body of the request
     * @param params parameters with the path and role
     */
    public JsonApiResponse generateCertificate(String name, String description, String vaultRoleId, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.generateCertificate(body, params);
        if (responseFromModule.isSuccess() &&
                responseFromModule.getResult().has("serial_number") &&
                responseFromModule.getResult().get("serial_number").isString()) {
            return sendToDbApiIfSaveToDb(dbHandler::generateCertificate,
                    responseFromModule.getResult().get("serial_number").asString(),
                    name,
                    vaultRoleId,
                    description,
                    responseFromModule);
        } else {
            return JsonApiResponse.error("Error creating the certificate");
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
     * Generate root CA
     *
     * @param idName identifier in the dbapi side
     * @param description description in the dbapi
     * @param body   body of the request with commonName, ttl, and optionally others
     * @param params parameters with  path
     */
    public JsonApiResponse generateRootCA(String idName, String description, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.generateRootCA(body, params);
        if (responseFromModule.isSuccess() &&
                responseFromModule.getResult().has("issuer_ref") &&
                responseFromModule.getResult().get("issuer_ref").isString()) {
            String issuerRef = responseFromModule.getResult().get("issuer_ref").asString();
            responseFromModule = moduleHandler.getIssuerInfo(issuerRef, params);
            return sendToDbApiIfSaveToDb(dbHandler::generateRootCA, issuerRef, idName, description, responseFromModule);
        } else {
            return JsonApiResponse.error("Error creating the CA");
        }
    }

    /**
     * Generate intermediate CA
     *
     * @param idName identifier in the dbapi side
     * @param description description in the dbapi
     * @param body   body of the request with commonName, ttl, and optionally others
     * @param params parameters with  path
     */
    public JsonApiResponse generateIntermediateCA(String idName, String description, Json body, Json params) {
        String caType = body.get("ca_type").asString();
        body.delAt("ca_type");
        JsonApiResponse responseFromModule = moduleHandler.generateIntermediateCA(body, params);

        if (responseFromModule.isSuccess() &&
                responseFromModule.getResult().has("issuer_ref") &&
                responseFromModule.getResult().get("issuer_ref").isString()) {
            String issuerRef = responseFromModule.getResult().get("issuer_ref").asString();
            responseFromModule = moduleHandler.getIssuerInfo(issuerRef, params);
            Json result = responseFromModule.getResult();
            result.set("ca_type", caType);
            responseFromModule = JsonApiResponse.result(result);
            return sendToDbApiIfSaveToDb(dbHandler::generateIntermediateCA, issuerRef, idName, description, responseFromModule);
        } else {
            return JsonApiResponse.error("Error creating the CA");
        }
    }

    /**
     * Verifies if the module api is reachable
     *
     * @return whether it is reachable
     */
    public Json getStatus() {
//        return Json.object();
        return  moduleHandler.getStatus();
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
    public JsonApiResponse sendToDbApiIfSaveToDb(IJsonApiResponserWithDescription method, String name, String description, JsonApiResponse responseFromModule) {
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            Json nextRequestBody = responseFromModule.getResult();
            nextRequestBody = (nextRequestBody!=null) ? nextRequestBody : Json.object();
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
            nextRequestBody = (nextRequestBody!=null) ? nextRequestBody : Json.object();
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
            nextRequestBody = (nextRequestBody!=null) ? nextRequestBody : Json.object();
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
//    public JsonApiResponse sendToDbApiIfSaveToDb(IJsonApiResponserWithIdStr method, String id, String name, JsonApiResponse responseFromModule) {
//        if (responseFromModule.isSuccess() && shouldSaveToDb) {
//            Json nextRequestBody = responseFromModule.getResult();
//            nextRequestBody = (nextRequestBody!=null) ? nextRequestBody : Json.object();
//            return method.apply(id, name, nextRequestBody);
//        } else {
//            return responseFromModule;
//        }
//    }

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
            nextRequestBody = (nextRequestBody!=null) ? nextRequestBody : Json.object();
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
            nextRequestBody = (nextRequestBody!=null) ? nextRequestBody : Json.object();
            return method.apply(id, name, description, path, nextRequestBody);
        } else {
            return responseFromModule;
        }
    }

    /**
     * Initial synchronisation for the issuers/ca (intermediate ca)
     * @param path to synchronize the issuers
     */
    private void synchronizeIssuers(String path) {
        Json body = Json.object("path", path);
        JsonApiResponse issuers = moduleHandler.getIssuers(body);
        if (issuers.isSuccess()) {
            for (Json issuer_ref : issuers.getResult().asJsonList()) {
                Json body2 = Json.read(body.toString());
                body2.at("issuer_ref", issuer_ref.asString());
                JsonApiResponse issuer = moduleHandler.getIssuerInfo(issuer_ref.asString(), body2);
                if (issuer.isSuccess()) {
                    dbHandler.generateIntermediateCA(issuer_ref.asString(), null, issuer.getResult());
                }
            }
        }

    }

    /**
     * Initial synchronisation for the roles
     * @param path to synchronize the roles
     */
    private void synchronizeRoles(String path) {
        Json body = Json.object("path", path);
        JsonApiResponse issuers = moduleHandler.getRoles(body);
        if (issuers.isSuccess()) {
            for (Json role_name : issuers.getResult().asJsonList()) {
                Json body2 = Json.read(body.toString());
                body2.at("name", role_name.asString());
                JsonApiResponse role = moduleHandler.getRole(role_name.asString(), body2);
                if (role.isSuccess()) {
                    dbHandler.createRole(role_name.asString(), null, null, role.getResult());
                }
            }
        }

    }

    /**
     * Initial synchronisation for the certificates
     * @param path to synchronize the certificates
     */
    private void synchronizeCertificate(String path) {
        Json body = Json.object("path", path);
        JsonApiResponse certificates = moduleHandler.getCertificates(body);
        if (certificates.isSuccess()) {
            for (Json serial_number : certificates.getResult().asJsonList()) {
                Json body2 = Json.read(body.toString());
                body2.at("serial_number", serial_number.asString());
                body2.at("path", path);
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
