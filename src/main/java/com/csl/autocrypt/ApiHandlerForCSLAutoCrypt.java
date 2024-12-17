package com.csl.autocrypt;

import com.csl.autocrypt.enums.AutocryptConstants;
import com.csl.web.apiclient.ApiHandler;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import com.csl.autocrypt.enums.ApiEndpointForCSLAutocrypt;

import static com.csl.autocrypt.enums.AutocryptConstants.Issuer;
import static com.csl.autocrypt.enums.AutocryptConstants.Common;

/**
 * Extension of the Api Handler for implementing the specific methods that contact the module AutoCrypt
 */
public class ApiHandlerForCSLAutoCrypt extends ApiHandler {

    /**
     * General constructor
     *
     * @param nameModule name of the module
     * @param ip ip of the module
     * @param port port of the module
     * @param useSSL use SSL for connecting the module
     *
     */
    public ApiHandlerForCSLAutoCrypt(String nameModule, String ip, int port, boolean useSSL) {
        super(nameModule, ip, port, useSSL);
        testConnexion(()->this.sendGet(ApiEndpointForCSLAutocrypt.MISC_URI_IS_ALIVE.endpoint(), Json.object(), true));
    }

    /**
     * Gets the list of issuers
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getIssuers(Json params) {
        return this.sendGet(
                ApiEndpointForCSLAutocrypt.ISSUER_URI.endpoint(),
                params.set(Common.GET_DELETED, false)
        );
    }

    /**
     * Gets the list of issuers
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getDeletedIssuers(Json params) {
        return this.sendGet(
                ApiEndpointForCSLAutocrypt.ISSUER_URI.endpoint(),
                params.set(Common.GET_DELETED, true)
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
                ApiEndpointForCSLAutocrypt.ISSUER_URI_SLASH + issuerRef,
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
                ApiEndpointForCSLAutocrypt.ISSUER_URI_SLASH + issuerRef,
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
                ApiEndpointForCSLAutocrypt.ISSUER_URI_SLASH + issuerRef,
                params.set(Common.DELETE, true).set(AutocryptConstants.Certificate.REVOKE, true),
                body
        );
    }

    /**
     * Imports a new issuer
     *
     * @param params parameters with the path
     * @param file certificate content
     */
    public JsonApiResponse importIssuer(Json params, String file) {
        Json newBody = Json.object();
        newBody.set(Issuer.PEM_BUNDLE, file.replace("\r",""));
        return this.sendPost(
                ApiEndpointForCSLAutocrypt.ISSUER_URI_IMPORT.endpoint(),
                params,
                newBody
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
                params.set(Common.GET_DELETED, false)
        );
    }

    /**
     * Gets the list of deleted roles
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getDeletedRoles(Json params) {
        return this.sendGet(
                ApiEndpointForCSLAutocrypt.ROLE_URI.endpoint(),
                params.set(Common.GET_DELETED, true)
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
                ApiEndpointForCSLAutocrypt.ROLE_URI_SLASH + name,
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
                ApiEndpointForCSLAutocrypt.ROLE_URI_SLASH + name,
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
                ApiEndpointForCSLAutocrypt.ROLE_URI_SLASH + name,
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
     * Validate the template of a certificate
     *
     * @param body body of the request
     * @param params parameters with the path and role
     */
    public JsonApiResponse validateTemplate(Json body, Json params) {
        return this.sendPost(
                ApiEndpointForCSLAutocrypt.CERT_URI_TEMPLATE.endpoint(),
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
     * Gives the list of certificates : revoked and not revoked
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getCertificates(Json params) {
        return this.sendGet(
                ApiEndpointForCSLAutocrypt.CERT_URI.endpoint(),
                params.set(Common.GET_DELETED, false)
        );
    }

    /**
     * Gives the list of certificates not revoked
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getCertificatesNonRevoked(Json params) {
        return this.sendGet(
                ApiEndpointForCSLAutocrypt.CERT_URI_NOT_REVOKED.endpoint(),
                params.set(Common.GET_DELETED, false)
        );
    }

    /**
     * Gives the list of revoked certificates
     *
     * @param params parameters with the path
     */
    public JsonApiResponse getRevokedCertificates(Json params) {
        return this.sendGet(
                ApiEndpointForCSLAutocrypt.CERT_URI_REVOKED.endpoint(),
                params.set(Common.GET_DELETED, false)
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
                ApiEndpointForCSLAutocrypt.CERT_URI_SLASH + serialNumber,
                params
        );
    }

    /**
     * Gets the certificate without the private key
     *
     * @param serialNumber identifier of the certificate
     * @param params       parameters with the serialNumber
     */
    public JsonApiResponse getCertificateWithoutPrivateKey(String serialNumber, Json params) {
            return this.sendGet(
                    ApiEndpointForCSLAutocrypt.CERT_URI_GET_WO_PK + serialNumber,
                    params
            );
    }

    /**
     * Gets the certificate with the private key
     *
     * @param serialNumber identifier of the certificate
     * @param params       parameters with the serialNumber
     */
    public JsonApiResponse getCertificateWithPrivateKey(String serialNumber, Json params) {
        return this.sendGet(
                    ApiEndpointForCSLAutocrypt.CERT_URI_GET_WITH_PK + serialNumber,
                    params
            );
    }

    /**
     * Downloads the given certificate
     *
     * @param serialNumber identifier of the certificate
     * @param params       parameters with the serialNumber
     */
    public JsonApiResponse downloadCertificate(String serialNumber, Json params) {
        try {
            return this.downloadFileGet(
                    ApiEndpointForCSLAutocrypt.CERT_URI_DOWNLOAD + serialNumber,
                    params
            );
        } catch (Exception e) {
            return JsonApiResponse.error(e.getMessage());
        }
    }

    /**
     * Revokes the given certificate
     *
     * @param serialNumber identifier of the certificate
     * @param params       parameters with the path
     */
    public JsonApiResponse revokeCertificate(String serialNumber, Json params) {
        return this.sendDelete(
                ApiEndpointForCSLAutocrypt.CERT_URI_REVOKE + serialNumber,
                params
        );
    }

    /**
     * Revokes the given certificate
     *
     * @param body         body of the request
     * @param params       parameters with the path
     */
    public JsonApiResponse deployCertificate(Json body, Json params) {
        return this.sendPost(
                ApiEndpointForCSLAutocrypt.CERT_URI_DEPLOY_CERTIFICATE.toString(),
                params,
                body
        );
    }

    /**
     * Delete all the revoked certificates
     */
    public JsonApiResponse deleteRevokedCertificates() {
        throw new UnsupportedOperationException("Not yet implemented");
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
        return Json.object(Common.IS_HTTP_API_KEY_REACHABLE, this.sendGet(
                ApiEndpointForCSLAutocrypt.MISC_URI_IS_ALIVE.endpoint(), Json.object()).isSuccess());
    }
}


