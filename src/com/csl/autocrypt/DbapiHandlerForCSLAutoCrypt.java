package com.csl.autocrypt;

import com.csl.autocrypt.enums.DbapiEndpointForCSLAutocrypt;
import com.csl.intercom.cslscan.ApiHandler;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;

/**
 * Extension of the Api Handler for implementing the specific methods of AutoCrypt that contact the DBAPI
 */
public class DbapiHandlerForCSLAutoCrypt extends ApiHandler {

    /**
     * Constructor with no module name
     *
     * @param url url of the service api
     */
    public DbapiHandlerForCSLAutoCrypt(String url) {
        this("", url);
    }

    /**
     * General constructor
     *
     * @param nameModule nameof the module
     * @param url        url of the service api
     */
    public DbapiHandlerForCSLAutoCrypt(String nameModule, String url) {
        super(nameModule, url);
    }

    /**
     * Updates the information of the given issuer in the module and the DB
     *
     * @param id identifier of the issuer in dbapi side
     * @param body      body of the request
     */
    public JsonApiResponse updateIssuerInfo(int id, Json body) {
        return this.sendPut(DbapiEndpointForCSLAutocrypt.ISSUER_.getEndpoint() + id, body);
    }

    /**
     * Deletes the given issuer from the module and the DB
     *
     * @param id identifier of the issuer in dbapi side
     * @param body parameters with the path and the issuer id
     */
    public JsonApiResponse deleteIssuer(int id,  Json body) {
        return this.sendDelete(
                DbapiEndpointForCSLAutocrypt.ISSUER_.toString() + id,
                null);
    }

    /**
     * Imports a new certificate
     *
     * @param body parameters with the path and the file
     */
    public JsonApiResponse importCertificate(Json body) {
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.ISSUER.toString(),
                body);
        // TODO : verify this import
    }

    /**
     * Creates a new role
     *
     * @param body body with the information
     */
    public JsonApiResponse createRole(Json body) {
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.ROLE.toString(),
                body);
    }

    /**
     * Deletes the given role
     *
     * @param id identifier of the issuer in dbapi side
     * @param body parameters with the path and name of role
     */
    public JsonApiResponse deleteRole(int id, Json body) {
        return this.sendDelete(
                DbapiEndpointForCSLAutocrypt.ROLE_.toString() + id,
                body);
    }

    /**
     * Updates the information of the given role
     *
     * @param id identifier of the issuer in dbapi side
     * @param body parameters with the path and name of role, others?
     */
    public JsonApiResponse updateRole(int id, Json body) {
        return this.sendPut(
                DbapiEndpointForCSLAutocrypt.ROLE_.toString() + id,
                body);
    }

    /**
     * Generates a certificates at the given path and role
     *
     * @param body parameters with the path and role
     */
    public JsonApiResponse generateCertificate(Json body) {
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CERTIFICATES.toString(),
                body);
    }

    /**
     * Revokes the given certificate
     *
     * @param id identifier of the issuer in dbapi side
     * @param params parameters with the path
     */
    public JsonApiResponse revokeCertificate(int id, Json params) {
        return this.sendDelete(
                DbapiEndpointForCSLAutocrypt.CERTIFICATES_.toString() + id, // TODO : get ID
                params);
    }

    /**
     * Generate root CA
     *
     * @param body body of the request with commonName, ttl, and optionally others
     */
    public JsonApiResponse generateRootCA(Json body) {
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CA.toString(),
                body);
    }

    /**
     * Generate intermediate CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public JsonApiResponse generateIntermediateCA(Json body) {
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CA.toString(),
                body);
    }
}
