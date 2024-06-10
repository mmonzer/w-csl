package com.csl.autocrypt;

import com.csl.intercom.cslscan.ApiHandler;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import com.csl.autocrypt.enums.ApiEndpointForCSLAutocrypt;

/**
 * Extension of the Api Handler for implementing the specific methods that contact the module AutoCrypt
 */
public class ApiHandlerForCSLAutoCrypt extends ApiHandler {

    /**
     * Constructor with no module name
     *
     * @param url url of the service api
     */
    public ApiHandlerForCSLAutoCrypt(String url) {
        this("", url);
    }

    /**
     * General constructor
     *
     * @param nameModule nameof the module
     * @param url        url of the service api
     */
    public ApiHandlerForCSLAutoCrypt(String nameModule, String url) {
        super(nameModule, url);
    }

    /**
     * Gets the list of issuers
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getIssuers(Json params) {
        return this.sendGet(
                ApiEndpointForCSLAutocrypt.ISSUER_URI.endpoint(),
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
        return this.sendGet(
                ApiEndpointForCSLAutocrypt.ISSUER_URI_ + issuerRef,
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
        return this.sendPut(
                ApiEndpointForCSLAutocrypt.ISSUER_URI_ + issuerRef,
                params,
                body
        );
    }

    /**
     * Deletes the given issuer from the module and the DB
     *
     * @param body parameters with the path and the issuer id
     */
    public JsonApiResponse deleteIssuer(String issuerRef, Json body, Json params) {
        return this.sendDelete(
                ApiEndpointForCSLAutocrypt.ISSUER_URI_ + issuerRef,
                params,
                body
        );
    }

    /**
     * Imports a new certificate
     *
     * @param body parameters with the path and the file
     */
    public JsonApiResponse importCertificate(Json body, Json params) {
        return this.sendPost(
                ApiEndpointForCSLAutocrypt.ISSUER_URI_IMPORT.endpoint(),
                params,
                body
        );
    }

    /**
     * Gets the list of roles
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getRoles(Json params) {
        return this.sendGet(
                ApiEndpointForCSLAutocrypt.ROLE_URI.endpoint(),
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
        return this.sendPost(
                ApiEndpointForCSLAutocrypt.ROLE_URI.endpoint(),
                params,
                body
        );
    }

    /**
     * Gets the information of the given role
     *
     * @param name   name of the role
     * @param params parameters with the path and name of role
     */
    public JsonApiResponse getRole(String name, Json params) {
        return this.sendGet(
                ApiEndpointForCSLAutocrypt.ROLE_URI_ + name,
                params
        );
    }

    /**
     * Deletes the given role
     *
     * @param body parameters with the path and name of role
     */
    public JsonApiResponse deleteRole(String name, Json body, Json params) {
        return this.sendDelete(
                ApiEndpointForCSLAutocrypt.ROLE_URI_ + name,
                params,
                body
        );
    }

    /**
     * Updates the information of the given role
     *
     * @param body parameters with the path and name of role, others?
     */
    public JsonApiResponse updateRole(String name, Json body, Json params) {
        return this.sendPut(
                ApiEndpointForCSLAutocrypt.ROLE_URI_ + name,
                params,
                body
        );
    }

    /**
     * Activates the Online Certificate Status Protocol (OCSP)
     *
     * @param body parameters with the path and name of role, others?
     */
    public JsonApiResponse activateOCSP(Json body, Json params) {
        return this.sendPost(
                ApiEndpointForCSLAutocrypt.MISC_URI_ACTIVATE_OCSP.endpoint(),
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
        return this.sendPost(
                ApiEndpointForCSLAutocrypt.CERT_URI_ISSUE.endpoint(),
                params,
                body
        );
    }

    /**
     * Gives the list of certificates
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getCertificates(Json params) {
        return this.sendGet(
                ApiEndpointForCSLAutocrypt.CERT_URI.endpoint(),
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
        return this.sendGet(
                ApiEndpointForCSLAutocrypt.CERT_URI_ + serialNumber,
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
        return this.sendDelete(
                ApiEndpointForCSLAutocrypt.CERT_URI_REVOKE_ + serialNumber,
                params
        );
    }

    /**
     * Generate root CA
     *
     * @param body   body of the request with commonName, ttl, and optionally others
     * @param params parameters with  path
     */
    public JsonApiResponse generateRootCA(Json body, Json params) {
        return this.sendPost(
                ApiEndpointForCSLAutocrypt.CA_URI_GENERATE_ROOT.endpoint(),
                params,
                body
        );
    }

    /**
     * Generate intermediate CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public JsonApiResponse generateIntermediateCA(Json body, Json params) {
        return this.sendPost(
                ApiEndpointForCSLAutocrypt.CA_URI_GENERATE_INTER.endpoint(),
                params,
                body
        );
    }

    /**
     * Verifies if the module api is reachable
     *
     * @return whether it is reachable
     */
    public Json getStatus() {
        return Json.object("is_http_api_reachable", this.sendGet(
                ApiEndpointForCSLAutocrypt.MISC_URI_IS_ALIVE.endpoint(), Json.object()).isSuccess());
    }
}


