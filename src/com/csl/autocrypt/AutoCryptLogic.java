package com.csl.autocrypt;

import com.csl.autocrypt.enums.DbapiEndpointForCSLAutocrypt;
import com.csl.intercom.cslscan.ApiHandler;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import main.services.endpoints.AutoCryptEndpoints;
import org.eclipse.jetty.servlet.Source;

public class AutoCryptLogic {
    private ApiHandler moduleHandler;
    private ApiHandler dbHandler;
    private boolean shouldSaveToDb = false;

    public AutoCryptLogic(ApiHandler module, ApiHandler db) {
        this.moduleHandler = module;
        this.dbHandler = db;
    }

    /**
     * Gets the list of issuers
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getIssuers(Json params) {
        return moduleHandler.sendGet(
                AutoCryptEndpoints.ISSUER_URI,
                params
        );
    }

    /**
     * Recovers the information of the given issuer
     *
     * @param issuerRef issuer reference
     * @param body      parameters with the path and the issuer id
     */
    public JsonApiResponse getIssuerInfo(String issuerRef, Json body) {
        return moduleHandler.sendGet(
                AutoCryptEndpoints.ISSUER_URI_ + issuerRef,
                body
        );
    }

    /**
     * Updates the information of the given issuer in the module and the DB
     *
     * @param issuerRef identifier of the issuer
     * @param body      body of the request
     * @param params    parameters of the request
     */
    public JsonApiResponse updateIssuerInfo(String issuerRef, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.sendPut(
                AutoCryptEndpoints.ISSUER_URI_ + issuerRef,
                params,
                body
        );
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            return dbHandler.sendPut(
                    DbapiEndpointForCSLAutocrypt.ISSUER_ + issuerRef,
                    responseFromModule.getResult());
        } else {
            return responseFromModule;
        }
    }

    /**
     * Deletes the given issuer from the module and the DB
     *
     * @param body parameters with the path and the issuer id
     */
    public JsonApiResponse deleteIssuer(String issuerRef, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.sendDelete(
                AutoCryptEndpoints.ISSUER_URI_ + issuerRef,
                params,
                body
        );
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            return dbHandler.sendDelete(
                    DbapiEndpointForCSLAutocrypt.ISSUER_ + issuerRef,
                    null);
        } else {
            return responseFromModule;
        }
    }

    /**
     * Imports a new certificate
     *
     * @param body parameters with the path and the file
     */
    public JsonApiResponse importCertificate(Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.sendPost(
                AutoCryptEndpoints.ISSUER_URI_IMPORT,
                params,
                body
        );
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            return dbHandler.sendPost(
                    DbapiEndpointForCSLAutocrypt.ISSUER.toString(),
                    responseFromModule.getResult());
            // TODO : verify this import
        } else {
            return responseFromModule;
        }
    }

    /**
     * Gets the list of roles
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getRoles(Json params) {
        return moduleHandler.sendGet(
                AutoCryptEndpoints.ROLE_URI,
                params
        );
    }

    /**
     * Creates a new role
     *
     * @param body   body with the information
     * @param params parameters with the path
     */
    public JsonApiResponse createRole(Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.sendPost(
                AutoCryptEndpoints.ROLE_URI,
                params,
                body
        );
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            return dbHandler.sendPost(
                    DbapiEndpointForCSLAutocrypt.ROLE.toString(),
                    responseFromModule.getResult());
        } else {
            return responseFromModule;
        }
    }

    /**
     * Gets the information of the given role
     *
     * @param name   name of the role
     * @param params parameters with the path and name of role
     */
    public JsonApiResponse getRole(String name, Json params) {
        return moduleHandler.sendGet(
                AutoCryptEndpoints.ROLE_URI_ + name,
                params
        );
    }

    /**
     * Deletes the given role
     *
     * @param body parameters with the path and name of role
     */
    public JsonApiResponse deleteRole(String name, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.sendDelete(
                AutoCryptEndpoints.ROLE_URI_ + name,
                params,
                body
        );
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            return dbHandler.sendDelete(
                    DbapiEndpointForCSLAutocrypt.ROLE_ + "id", // TODO : get ID
                    responseFromModule.getResult());
        } else {
            return responseFromModule;
        }
    }

    /**
     * Updates the information of the given role
     *
     * @param body parameters with the path and name of role, others?
     */
    public JsonApiResponse updateRole(String name, Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.sendPut(
                AutoCryptEndpoints.ROLE_URI_ + name,
                params,
                body
        );
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            return dbHandler.sendPut(
                    DbapiEndpointForCSLAutocrypt.ROLE_ + "id", // TODO : find id
                    responseFromModule.getResult());
        } else {
            return responseFromModule;
        }
    }

    /**
     * Activates the Online Certificate Status Protocol (OCSP)
     *
     * @param body parameters with the path and name of role, others?
     */
    public JsonApiResponse activateOCSP(Json body, Json params) {
        return moduleHandler.sendPost(
                AutoCryptEndpoints.MISC_URI_ACTIVATE_OCSP,
                params,
                body
        );
    }

    /**
     * Generates a certificates at the given path and role
     *
     * @param body parameters with the path and role
     */
    public JsonApiResponse generateCertificate(Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.sendPost(
                AutoCryptEndpoints.CERT_URI_ISSUE,
                params,
                body
        );
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            return dbHandler.sendPost(
                    DbapiEndpointForCSLAutocrypt.CERTIFICATES.toString(),
                    responseFromModule.getResult());
        } else {
            return responseFromModule;
        }
    }

    /**
     * Gives the list of certificates
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getCertificates(Json params) {
        return moduleHandler.sendGet(
                AutoCryptEndpoints.CERT_URI,
                params
        );
    }

    /**
     * Gives the information of the given certificate
     *
     * @param serialNumber identifier of the certificate
     * @param params       parameters with the serialNumber
     */
    public JsonApiResponse getCertificateInfo(String serialNumber, Json params) {
        return moduleHandler.sendGet(
                AutoCryptEndpoints.CERT_URI_ + serialNumber,
                params
        );
    }

    /**
     * Revokes the given certificate
     *
     * @param serialNumber identifier of the certificate
     * @param params       parameters with the path
     */
    public JsonApiResponse revokeCertificate(String serialNumber, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.sendDelete(
                AutoCryptEndpoints.CERT_URI_REVOKE_ + serialNumber,
                params
        );
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            return dbHandler.sendDelete(
                    DbapiEndpointForCSLAutocrypt.CERTIFICATES_ + "id", // TODO : get ID
                    responseFromModule.getResult());
        } else {
            return responseFromModule;
        }
    }

    /**
     * Generate root CA
     *
     * @param body   body of the request with commonName, ttl, and optionally othes
     * @param params parameters with  path
     */
    public JsonApiResponse generateRootCA(Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.sendPost(
                AutoCryptEndpoints.CA_URI_GENERATE_ROOT,
                params,
                body
        );
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            return dbHandler.sendPost(
                    DbapiEndpointForCSLAutocrypt.CA.toString(),
                    responseFromModule.getResult());
        } else {
            return responseFromModule;
        }
    }

    /**
     * Generate intermediate CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public JsonApiResponse generateIntermediateCA(Json body, Json params) {
        JsonApiResponse responseFromModule = moduleHandler.sendPost(
                AutoCryptEndpoints.CA_URI_GENERATE_INTER,
                params,
                body
        );
        if (responseFromModule.isSuccess() && shouldSaveToDb) {
            return dbHandler.sendPost(
                    DbapiEndpointForCSLAutocrypt.CA.toString(),
                    responseFromModule.getResult());
        } else {
            return responseFromModule;
        }
    }

    /**
     * Verifies if the module api is reachable
     *
     * @return whether it is reachable
     */
    public Json getStatus() {
       return Json.object("is_http_api_reachable", moduleHandler.sendGet(
                AutoCryptEndpoints.MISC_URI_IS_ALIVE, Json.object()).isSuccess());
    }

    /**
     * Changes the saving to Db
     * @param shouldSaveToDb
     */
    public void setSaveToDb(boolean shouldSaveToDb) {
        this.shouldSaveToDb = shouldSaveToDb;
    }
}
