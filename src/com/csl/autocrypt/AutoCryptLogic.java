package com.csl.autocrypt;

import com.ucsl.json.Json;
import main.services.JsonApiResponse;

import static com.csl.autocrypt.outils.JsonHelper.mergerJson;

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
     * @param id        identifier in the dbapi side
     * @param name      name for dbapi side
     * @param issuerRef identifier of the issuer (module side)
     * @param body      body of the request
     * @param params    parameters of the request
     */
    public JsonApiResponse updateIssuerInfo(Integer id, String name, String description, String issuerRef, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.updateIssuerInfo(issuerRef, body, params);
        params.at("issuer_ref", issuerRef);
        body.set("id", id);
        body.set("type", "internal");
        return sendToDbApiIfSaveToDb(dbHandler::updateIssuerInfo, issuerRef, name, description, body.get("path").asString(),
                JsonApiResponse.result(mergerJson(mergerJson(responseFromModule.getResult(), body), params)));
    }

    /**
     * Updates the information of the given issuer in the module and the DB
     *
     * @param name      name for dbapi side
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
     * @param name      name in the dbapi side
     * @param issuerRef identifier in the module side
     * @param body      body of the request
     * @param params    parameters with the path
     */
    public JsonApiResponse deleteIssuer(String name, String issuerRef, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.deleteIssuer(issuerRef, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::deleteIssuer, issuerRef, name, responseFromModule);
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
        JsonApiResponse responseFromDbapi = sendToDbApiIfSaveToDb(dbHandler::deleteIssuer, issuerRef, name, responseFromModule);
        if (!responseFromDbapi.isSuccess()) {
            return JsonApiResponse.error("Error deleting the main issuer from DBapi");
        }
        JsonApiResponse issuersToDelete = moduleHandler.getIssuers(params);
        // Delete the other issuers in the path, except for pki (only roots)
        if (issuersToDelete.isSuccess() && params.has("path") &&
                params.get("path").isString() && !params.get("path").asString().equals("pki")) {
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
                if (roleDbapi.has("certificate_authority_id") && roleDbapi.get("certificate_authority_id").isNumber() && roleDbapi.get("certificate_authority_id").asInteger() == caId &&
                        roleDbapi.has("name") && roleDbapi.get("name").isString() && roleDbapi.get("name").asString().equals(name)) {
                    response = dbHandler.deleteRole(roleDbapi.get("id").asString(), null, null);
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
        if (responseFromModule.isSuccess()) {
            String issuerRef = responseFromModule.getResult().get("imported_issuers").asJsonList().get(0).asString();
            responseFromModule = moduleHandler.getIssuerInfo(issuerRef, params);
            responseFromModule.getResult().set("ca_type", isRoot?"root":"intermediate");
            return sendToDbApiIfSaveToDb(isRoot?dbHandler::generateRootCA:dbHandler::generateIntermediateCA, issuerRef, idName, null,
                    JsonApiResponse.result(mergerJson(responseFromModule.getResult(), params)));
//            return sendToDbApiIfSaveToDb(dbHandler::generateRootCA, issuerRef, idName, null, null, responseFromModule);
        }
        return responseFromModule;
    }

    /**
     * Exports the given issuer
     *
     * @param issuerRef identifier of the issuer
     * @param params parameters with the path
     * @return the issuer certificate as a file
     */
    public JsonApiResponse exportIssuer(String issuerRef, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.getIssuerInfo(issuerRef, params);
        if (responseFromModule.isSuccess() &&
        responseFromModule.getResult().has("certificate") && responseFromModule.getResult().get("certificate").isString()) {
            Json response = Json.object();
            // TODO : range this formating elsewhere, probably not the right place
            response.at("Content-Type", "application/octet-stream");
            response.at("Content", responseFromModule.getResult().get("certificate").asString());
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
     * @param body        body with the information
     * @param params      parameters with the path
     */
    public JsonApiResponse createRole(String idName, String description, String certificateAuthorityId, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.createRole(body, params);
        if (responseFromModule.isSuccess()) {

            Json result = mergerJson(mergerJson(responseFromModule.getResult(), body), params);
            result.set("name", idName);
            result.set("country", result.get("country").asJsonList().get(0).asString());
            result.set("organization", result.get("organization").asJsonList().get(0).asString());
            result.set("locality", result.get("locality").asJsonList().get(0).asString());
            result.set("certificate_authority_id", certificateAuthorityId);
            return sendToDbApiIfSaveToDb(dbHandler::createRole, idName, description, certificateAuthorityId, JsonApiResponse.result(result));
        } else {

            return JsonApiResponse.error("Error creating role : " + responseFromModule.getError().toJson());
        }
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
     * @param id     identifier in the module/dbapi
     * @param name   name in the module/dbapi side
     * @param body   body of the request
     * @param params parameters with the path
     */
    public JsonApiResponse deleteRole(Integer id, String name, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.deleteRole(name, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::deleteRole, id.toString(), name, responseFromModule);
    }

    /**
     * Deletes the given role
     *
     * @param id     identifier in the module/dbapi
     * @param name   name in the module/dbapi side
     * @param body   body of the request
     * @param params parameters with the path
     */
    public JsonApiResponse deleteRole(String id, String name, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.deleteRole(name, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::deleteRole, id, name, responseFromModule);
    }

    /**
     * Updates the information of the given role
     *
     * @param name        identifier in the module/dbapi side
     * @param description description in the dbapi
     * @param body        parameters with the path and name of role, others?
     * @param params      parameters with the path
     */
    public JsonApiResponse updateRole(Integer id, String name, String description, String certificateAuthorityId, Json body, Json params) {
        moduleHandler.updateRole(name, body, params);
        // TODO : change this
        params.at("name", name);
        JsonApiResponse responseFromModule = moduleHandler.getRole(name, params);
        responseFromModule.getResult().set("country", responseFromModule.getResult().get("country").asJsonList().get(0).asString());
        responseFromModule.getResult().set("organization", responseFromModule.getResult().get("organization").asJsonList().get(0).asString());
        responseFromModule.getResult().set("locality", responseFromModule.getResult().get("locality").asJsonList().get(0).asString());
        return sendToDbApiIfSaveToDb(dbHandler::updateRole, id.toString(), name, description, certificateAuthorityId,
                JsonApiResponse.result(mergerJson(mergerJson(responseFromModule.getResult(), body), params)));
    }

    /**
     * Updates the information of the given role
     *
     * @param name        identifier in the module/dbapi side
     * @param description description in the dbapi
     * @param body        parameters with the path and name of role, others?
     * @param params      parameters with the path
     */
    public JsonApiResponse updateRole(String id, String name, String description, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.updateRole(name, body, params);
        return sendToDbApiIfSaveToDb(dbHandler::updateRole, id, name, description, null, responseFromModule);
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
     * @param body   body of the request
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
     * @param idName      identifier in the dbapi side
     * @param description description in the dbapi
     * @param body        body of the request with commonName, ttl, and optionally others
     * @param params      parameters with  path
     */
    public JsonApiResponse generateRootCA(String idName, String description, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.generateRootCA(body, params);
        if (responseFromModule.isSuccess() &&
                responseFromModule.getResult().has("issuer_ref") &&
                responseFromModule.getResult().get("issuer_ref").isString()) {
            String issuerRef = responseFromModule.getResult().get("issuer_ref").asString();
            // params.at("path", idName);
            responseFromModule = moduleHandler.getIssuerInfo(issuerRef, Json.object("path", "pki"));
            responseFromModule.getResult().set("path", "pki");
            responseFromModule.getResult().set("ca_type", "root");
            return sendToDbApiIfSaveToDb(dbHandler::generateRootCA, issuerRef, idName, description,
                    JsonApiResponse.result(mergerJson(mergerJson(responseFromModule.getResult(), body), params)));
        } else {
            return JsonApiResponse.error("Error creating the CA: " + responseFromModule.getError().toJson());
        }
    }

    /**
     * Generate intermediate CA
     *
     * @param idName      identifier in the dbapi side
     * @param description description in the dbapi
     * @param body        body of the request with commonName, ttl, and optionally others
     * @param params      parameters with  path
     */
    public JsonApiResponse generateIntermediateCA(String idName, String description, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.generateIntermediateCA(body, params);

        if (responseFromModule.isSuccess() &&
                responseFromModule.getResult().has("issuer_ref") &&
                responseFromModule.getResult().get("issuer_ref").isString()) {
            String issuerRef = responseFromModule.getResult().get("issuer_ref").asString();
            responseFromModule = moduleHandler.getIssuerInfo(issuerRef, params);
            responseFromModule.getResult().set("ca_type", "intermediate");
            return sendToDbApiIfSaveToDb(dbHandler::generateIntermediateCA, issuerRef, idName, description,
                    JsonApiResponse.result(mergerJson(mergerJson(responseFromModule.getResult(), body), params)));
        } else {
            return JsonApiResponse.error("Error creating the CA : " + responseFromModule.getError().toJson());
        }
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
     *
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
     *
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
