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
    public JsonApiResponse updateIssuerInfo(int id, String name, Json body) {
        return this.sendPut(
                DbapiEndpointForCSLAutocrypt.ISSUER_.endpoint() + id,
                formatBody("issuer", id, name, body));
    }

    /**
     * Deletes the given issuer from the module and the DB
     *
     * @param id identifier of the issuer in dbapi db
     * @param body parameters with the path and the issuer id
     */
    public JsonApiResponse deleteIssuer(int id, String name, Json body) {
        return this.sendDelete(
                DbapiEndpointForCSLAutocrypt.ISSUER_.endpoint() +id,
                null);
    }

    /**
     * Imports a new certificate
     *
     * @param body parameters with the path and the file
     */
    public JsonApiResponse importCertificate(String name, Json body) {
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.ISSUER.endpoint(),
                formatBody("issuer", name, body));
        // TODO : verify this import
    }

    /**
     * Creates a new role
     *
     * @param body body with the information
     */
    public JsonApiResponse createRole(String name, Json body) {
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.ROLE.endpoint(),
                formatBody("role", name, body));
    }

    /**
     * Deletes the given role
     *
     * @param id identifier of the issuer in dbapi db
     * @param body parameters with the path and name of role
     */
    public JsonApiResponse deleteRole(int id, String name, Json body) {
        return this.sendDelete(
                DbapiEndpointForCSLAutocrypt.ROLE_.endpoint() +id,
                formatBody("role", name, body));
    }

    /**
     * Updates the information of the given role
     *
     * @param id identifier of the issuer in dbapi db
     * @param body parameters with the path and name of role, others?
     */
    public JsonApiResponse updateRole(int id, String name, Json body) {
        return this.sendPut(
                DbapiEndpointForCSLAutocrypt.ROLE_.endpoint() +id,
                formatBody("role", id, name, body));
    }

    /**
     * Generates a certificates at the given path and role
     *
     * @param body parameters with the path and role
     */
    public JsonApiResponse generateCertificate(String name, Json body) {
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CERTIFICATES.endpoint(),
                formatBody("certificate", name, body));
    }

    /**
     * Revokes the given certificate
     *
     * @param id identifier of the issuer in dbapi db
     * @param params parameters with the path
     */
    public JsonApiResponse revokeCertificate(int id, String name, Json params) {
        return this.sendDelete(
                DbapiEndpointForCSLAutocrypt.CERTIFICATES_.endpoint() +id,
                params);
    }

    /**
     * Generate root CA
     *
     * @param body body of the request with commonName, ttl, and optionally others
     */
    public JsonApiResponse generateRootCA(String name, Json body) {
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CA.endpoint(),
                formatBody("ca", name, body));
    }

    /**
     * Generate intermediate CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public JsonApiResponse generateIntermediateCA(String name, Json body) {
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CA.endpoint(),
                formatBody("ca", name, body));
    }

    /**
     * Format body to feed dbapi
     *
     * @param category either roles, issuer, certificates or ca
     * @param name name of the thing in the dbapi db
     * @param body old body
     * @return new body with the right format
     */
    private Json formatBody(String category, String name, Json body) {
        Json newBody = Json.object();
        newBody.at("name", name);
        newBody.at(category+"_json",body);
        return newBody;
    }

    /**
     * Format body to feed dbapi
     *
     * @param id identifier in the dbapi
     * @param category either roles, issuer, certificates or ca
     * @param name name of the thing in the dbapi db
     * @param body old body
     * @return new body with the right format
     */
    private Json formatBody(String category, int id, String name, Json body) {
        Json newBody = Json.object();
        newBody.at("id", id);
        newBody.at("name", name);
        newBody.at(category+"_json",body);
        return newBody;
    }
}
