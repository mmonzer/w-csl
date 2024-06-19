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
     * @param issuerRef serial ref of the issuer in dbapi
     * @param name identifier of the issuer in dbapi side
     * @param description description of the issuer in dbapi side
     * @param body      body of the request
     */
    public JsonApiResponse updateIssuerInfo(String issuerRef, String name, String description, String path, Json body) {
        Json input = Json.object();
        input.at("name", name);
        input.at("ca_json", body);
        input.at("description", description);
        input.at("issuer_ref", issuerRef);
        input.at("path", path);
        return this.sendPut(
                DbapiEndpointForCSLAutocrypt.ISSUER_UPT_BY_REF_.endpoint() + issuerRef,
                input);
    }

    /**
     * Deletes the given issuer from the module and the DB
     *
     * @param id identifier of the issuer in dbapi db
     * @param body parameters with the path and the issuer id
     */
    public JsonApiResponse deleteIssuer(String id, String name, Json body) {
        return this.sendDelete(
                DbapiEndpointForCSLAutocrypt.ISSUER_DEL_BY_REF_.endpoint() +id,
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
    public JsonApiResponse createRole(String name, String description, String certificateAuthorityId, Json body) {
        Json input = Json.object();
        input.at("name", name);
        input.at("description", description);
        input.at("certificate_authority_id", certificateAuthorityId);;
        input.at("role_json", body);

        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.ROLE.endpoint(),
                input);
    }

    /**
     * Deletes the given role
     *
     * @param id identifier of the issuer in dbapi db
     * @param body parameters with the path and name of role
     */
    public JsonApiResponse deleteRole(String id, String name, Json body) {
        return this.sendDelete(
                DbapiEndpointForCSLAutocrypt.ROLE_.endpoint() +id,
                formatBody("role", name, body));
    }

    /**
     * Updates the information of the given role
     *
     * @param id identifier of the issuer in dbapi db
     * @param name name of the role
     * @param description description of the role in the dbapi
     * @param body parameters with the path and name of role, others?
     */
    public JsonApiResponse updateRole(String id, String name, String description, String certificateAuthorityId, Json body) {
        Json input = Json.object();
        input.at("name", name);
        input.at("description", description);
        input.at("certificate_authority_id", certificateAuthorityId);;
        input.at("role_json", body);

        return this.sendPut(
                DbapiEndpointForCSLAutocrypt.ROLE_.endpoint() +id,
                input);
    }

    /**
     * Generates a certificates at the given path and role
     *
     * @param body parameters with the path and role
     */
    public JsonApiResponse generateCertificate(String name, String description, Json body) {
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CERTIFICATES.endpoint(),
                formatBody("certificate", name, description, body));
    }

    /**
     * Generates a certificates at the given path and role
     *
     * @param body parameters with the path and role
     */
    public JsonApiResponse generateCertificate(String serialNumber, String name, String description, Json body) {
        Json input = Json.object();
        input.at("name", name);
        input.at("description", description);
        input.at("certificate_json", body);
        input.at("serial_number", serialNumber);

        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CERTIFICATES.endpoint(),
                formatBody("certificate", serialNumber, name, description, body));
    }

    /**
     * Revokes the given certificate
     *
     * @param id serial number of the certificate
     * @param serialNumber identifier of the certificate in the dbapi
     * @param params parameters with the path
     */
    public JsonApiResponse revokeCertificate(String id, String serialNumber, Json params) {
        return this.sendDelete(
                DbapiEndpointForCSLAutocrypt.CERTIFICATES_DEL_BY_SERIAL_NUMBER_.endpoint() +serialNumber,
                params);
    }

    /**
     * Generate root CA
     *
     * @param name name of the CA
     * @param description description of the CA in the dbapi
     * @param body body of the request with commonName, ttl, and optionally others
     */
    public JsonApiResponse generateRootCA(String name, String description, Json body) {
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CA.endpoint(),
                formatBody("ca", name, description, body));
    }

    /**
     * Generate root CA
     *
     * @param id serial number of the CA
     * @param name name of the CA
     * @param description description of the CA in the dbapi
     * @param body body of the request with commonName, ttl, and optionally others
     */
    public JsonApiResponse generateRootCA(String id, String name, String description, Json body) {
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CA.endpoint(),
                formatBody("ca", id, name, description, body));
    }

    /**
     * Generate intermediate CA
     *
     * @param name name of the CA
     * @param description description of the CA in the dbapi
     * @param body parameters with commonName, ttl, and optionally path
     */
    public JsonApiResponse generateIntermediateCA(String name, String description, Json body) {
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CA.endpoint(),
                formatBody("ca", name, description, body));
    }

    /**
     * Generate intermediate CA
     *
     * @param id serial number of the CA
     * @param name name of the CA
     * @param description description of the CA in the dbapi
     * @param body parameters with commonName, ttl, and optionally path
     */
    public JsonApiResponse generateIntermediateCA(String id, String name, String description, Json body) {
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CA.endpoint(),
                formatBodyCA("ca", id, name, description, body));
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
        return formatBody(category, null, name, null, body);
    }

    /**
     * Format body to feed dbapi
     *
     * @param category either roles, issuer, certificates or ca
     * @param name name of the thing in the dbapi db
     * @param body old body
     * @return new body with the right format
     */
    private Json formatBody(String category, String name, String description, Json body) {
        return formatBody(category, null, name, description, body);
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
    private Json formatBody(String category, String id, String name, String description, Json body) {
        Json newBody = Json.object();
        newBody.at("id", id);
        newBody.at("name", name);
        if (description!=null) {
            newBody.at("description", description);
        }
        newBody.at(category+"_json",body);
        return newBody;
    }

    /**
     * Format body to feed dbapi
     *
     * @param issuerRef identifier in the dbapi
     * @param category either roles, issuer, certificates or ca
     * @param name name of the thing in the dbapi db
     * @param body old body
     * @return new body with the right format
     */
    private Json formatBodyCA(String category, String issuerRef, String name, String description, Json body) {
        Json newBody = Json.object();
        newBody.at("issuer_ref", issuerRef);
        newBody.at("name", name);
        if (description!=null) {
            newBody.at("description", description);
        }
        newBody.at(category+"_json",body);
        return newBody;
    }
}
