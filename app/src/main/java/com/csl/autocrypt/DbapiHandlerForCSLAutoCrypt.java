package com.csl.autocrypt;

import com.csl.autocrypt.enums.DbapiEndpointForCSLAutocrypt;
import com.csl.intercom.dbapi.DbapiHandler;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;

import static com.csl.autocrypt.enums.AutocryptConstants.*;
import static com.csl.autocrypt.outils.JsonHelper.mergerJson;

/**
 * Extension of the Api Handler for implementing the specific methods of AutoCrypt that contact the DBAPI
 */
public class DbapiHandlerForCSLAutoCrypt extends DbapiHandler {

    /**
     * Constructor with no module name
     *
     */
    public DbapiHandlerForCSLAutoCrypt() {
        this("CSLAutoCrypt");
    }

    /**
     * General constructor
     *
     * @param nameModule nameof the module
     */
    public DbapiHandlerForCSLAutoCrypt(String nameModule) {
        super(nameModule);
    }

    /**
     * List all the ca

     */
    public JsonApiResponse listIssuers() {
        return this.sendGet(
                DbapiEndpointForCSLAutocrypt.ISSUER.endpoint(),
                Json.object());
    }

    /**
     * Get the information of a ca
     *
     * @param issuerRef serial ref of the issuer in dbapi
     */
    public JsonApiResponse getInfoIssuerFromDbapi(String issuerRef) {
        JsonApiResponse response = listIssuers();
        if (!response.isSuccess()) {
            return response;
        }
        Json result = response.getResult();

        if (!result.isArray()) {
            return response;
        }

        for (Json ca : result.asJsonList()) {
            if (ca.isObject() && !ca.isNull() && ca.has(Issuer.ISSUER_REF) && ca.get(Issuer.ISSUER_REF).asString().equals(issuerRef) && ca.has(Issuer.CA_JSON)) {
                return JsonApiResponse.result(ca.get(Issuer.CA_JSON));
            }
        }

        return JsonApiResponse.error("CA not found in dbapi");
    }

    /**
     * Updates the information of the given issuer in the module and the DB
     *
     * @param issuerRef serial ref of the issuer in dbapi
     * @param description description of the issuer in dbapi side
     * @param body      body of the request
     */
    public JsonApiResponse updateIssuerInfo(String name, String issuerRef, String description, String path, Json body) {
        Json input = Json.object();
        input.at(Common.DESCRIPTION, description);
        input.at(Common.COMMON_NAME, name);
        input.at(Issuer.ISSUER_REF, issuerRef);
        input.at(Common.PATH, path);
        input.at(Issuer.CA_JSON, mergerJson(body, input));
        return this.sendPut(
                DbapiEndpointForCSLAutocrypt.ISSUER_UPT_BY_REF_.endpoint() + issuerRef,
                input);
    }

    /**
     * Deletes the given issuer from the module and the DB
     *
     * @param issuerRef identifier of the issuer in dbapi db
     */
    public JsonApiResponse deleteIssuer(String issuerRef, Json body) {
        return this.sendDelete(
                DbapiEndpointForCSLAutocrypt.ISSUER_DEL_BY_REF_.endpoint() +issuerRef,
                null);
    }

    /**
     * Creates a new role
     *
     * @param body body with the information
     */
    public JsonApiResponse createRole(String name, String description, String certificateAuthorityId, Json body) {
        Json input = Json.object();
        input.at(Common.NAME, name);
        input.at(Common.DESCRIPTION, description);
        input.at(Role.CERTIFICATE_AUTHORITY_ID, certificateAuthorityId);;
        input.at(Role.ROLE_JSON, body);

        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.ROLE.endpoint(),
                input);
    }

    /**
     * Deletes the given role
     *
     * @param name name of the role in vault
     * @param path path of the role in vault (unique couple name,path)
     */
    public JsonApiResponse deleteRole(String name, String path, Json body) {
        return this.sendDelete(
                DbapiEndpointForCSLAutocrypt.ROLE_DEL_BY_NAME_AND_PATH.endpoint(),
                Json.object(Common.NAME, name, Common.PATH, path)
        );
    }

    /**
     * Lists all the roles
     */
    public JsonApiResponse listRoles() {
        return this.sendGet(
                DbapiEndpointForCSLAutocrypt.ROLE.endpoint(),
                null);
    }

    /**
     * Updates the information of the given role
     *
     * @param name name of the role
     * @param path identifier of the role  (unique couple name,path)
     * @param description description of the role in the dbapi
     * @param body parameters with the path and name of role, others?
     */
    public JsonApiResponse updateRole(String name, String description, String certificateAuthorityId, String path, Json body) {
        Json input = Json.object();
        input.at(Common.NAME, name);
        input.at(Common.DESCRIPTION, description);
        input.at(Role.CERTIFICATE_AUTHORITY_ID, certificateAuthorityId);;
        input.at(Role.ROLE_JSON, mergerJson(input, body));

        return this.sendPut(
//                DbapiEndpointForCSLAutocrypt.ROLE_.endpoint() +id,
                DbapiEndpointForCSLAutocrypt.ROLE_UPD_BY_NAME_AND_PATH.endpoint(),
                Json.object(Common.NAME, name, Common.PATH, path),
                input);
    }

    /**
     * Generates a certificates at the given path and role
     *
     * @param body parameters with the path and role
     */
    public JsonApiResponse generateCertificate(String name, String description, Json body) {
        Json input = Json.object();
        input.at(Common.NAME, name);
        if (description!=null) {
            input.at(Common.DESCRIPTION, description);
        }
        input.at(Certificate.CERTIFICATE_JSON, body);
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CERTIFICATES.endpoint(),
                input);
    }

    /**
     * Generates a certificates at the given path and role
     *
     * @param body parameters with the path and role
     */
    public JsonApiResponse generateCertificate(String serialNumber, String name, String vaultRoleId, String description,  String path, Json body) {
        Json input = Json.object();
        input.at(Common.NAME, name);
        input.at(Common.DESCRIPTION, description);
        input.at(Certificate.CERTIFICATE_JSON, body);
        input.at(Certificate.SERIAL_NUMBER, serialNumber);
        input.at(Common.PATH, path);
        input.at(Certificate.VAULT_ROLE_ID, vaultRoleId);

        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CERTIFICATES.endpoint(),
                input);
    }

    /**
     * Revokes the given certificate
     *
     * @param serialNumber identifier of the certificate
     * @param params parameters with the path
     */
    public JsonApiResponse revokeCertificate(String serialNumber, Json params) {
        return this.sendDelete(
                DbapiEndpointForCSLAutocrypt.CERTIFICATES_DEL_BY_SERIAL_NUMBER_.endpoint() +serialNumber,
                params);
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
     * @param issuerRef serial number of the CA
     * @param name name of the CA
     * @param description description of the CA in the dbapi
     * @param body body of the request with commonName, ttl, and optionally others
     */
    public JsonApiResponse generateRootCA(String issuerRef, String name, String description, String serialNumber, Json certificate, Json body) {
        return generateCA(issuerRef, name, Common.PKI, description, serialNumber, certificate, body);
    }

    /**
     * Generate intermediate CA
     *
     * @param name name of the CA
     * @param description description of the CA in the dbapi
     * @param body parameters with commonName, ttl, and optionally path
     */
    public JsonApiResponse generateIntermediateCA(String name, String description, Json body) {
//        return this.sendPost(
//                DbapiEndpointForCSLAutocrypt.CA.endpoint(),
//                formatBody("ca", name, description, body));
        Json input = Json.object();
        input.at(Common.COMMON_NAME, name);
        input.at(Common.DESCRIPTION, description);
        input.at(Common.PATH, Common.PKI);
        input.at(Issuer.CA_JSON, mergerJson(body, input));
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CA.endpoint(),
                input);
    }

    /**
     * Generate intermediate CA
     *
     * @param issuerRef serial number of the CA
     * @param name name of the CA
     * @param path path of the CA
     * @param description description of the CA in the dbapi
     * @param body parameters with commonName, ttl, and optionally path
     */
    public JsonApiResponse generateCA(String issuerRef, String name, String path, String description, String serialNumber, Json certificate, Json body) {
        Json input = Json.object();
        input.set(Common.COMMON_NAME, name);
        input.set(Common.DESCRIPTION, description);
        input.set(Issuer.ISSUER_REF, issuerRef);
        input.set(Common.PATH, path);
        input.set(Certificate.SERIAL_NUMBER, serialNumber);
        input.set(Issuer.CA_JSON, mergerJson(body, input));
        input.set(Certificate.CERTIFICATE_OBJECT, certificate);
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CA.endpoint(),
                input);
    }

}
