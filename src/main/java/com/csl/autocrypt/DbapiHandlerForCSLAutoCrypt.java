package com.csl.autocrypt;
import java.time.*;
import com.csl.autocrypt.enums.DbapiEndpointForCSLAutocrypt;
import com.csl.intercom.dbapi.DbapiHandler;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;

import static com.csl.autocrypt.CSLAutocryptUtils.*;
import static com.csl.autocrypt.enums.AutocryptConstants.*;
import static com.ucsl.json.JsonUtil.mergerJson;

/**
 * Extension of the Api Handler for implementing the specific methods of AutoCrypt that contact the DBAPI
 */
public class DbapiHandlerForCSLAutoCrypt extends DbapiHandler {

    /**
     * Constructor with no module name
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
     * Creates or modifies the issuers in the database
     */
    public JsonApiResponse upsertIssuers(Json issuers) {
        if( issuers!=null && issuers.isArray() && issuers.asJsonList().isEmpty()) { return JsonApiResponse.success();}
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.ISSUER_UPSERT.endpoint(),
                formatListOfIssuers(issuers));
    }

    /**
     * Fetch the last update date for issuers
     */
    public JsonApiResponse getIssuerLastUpdateDate() {
        return this.sendGet(
                DbapiEndpointForCSLAutocrypt.ISSUER_GET_LAST_UPDATE_DATE.endpoint(),
                null);
    }

    /**
     * Updates the information of the given issuer in the module and the DB
     *
     * @param issuerRef   serial ref of the issuer in dbapi
     * @param description description of the issuer in dbapi side
     * @param body        body of the request
     */
    public JsonApiResponse updateIssuerInfo(String name, String issuerRef, String description, String path, Json body) {
        Json input = Json.object();
        input.at(Common.DESCRIPTION, description);
        input.at(Common.COMMON_NAME, name);
        input.at(Issuer.ISSUER_REF, issuerRef);
        input.at(Common.PATH, path);
        input.at(Issuer.CA_JSON, mergerJson(body, input));
        return this.sendPut(
                DbapiEndpointForCSLAutocrypt.ISSUER_UPT_BY_REF.endpoint() + issuerRef,
                input);
    }

    /**
     * Deletes the list of issuers from the module and the DB
     *
     * @param issuers list of the issuer to delete in dbapi db
     */
    public JsonApiResponse deleteIssuers(Json issuers) {
        if (issuers.isArray()) {
            for (Json issuer : issuers.asJsonList()) {
                if (issuer.has(Issuer.ISSUER_REF) && issuer.has(Common.PATH)) {
                    deleteIssuer(issuer.get(Issuer.ISSUER_REF).asString(),
                            Json.object(Common.PATH, issuer.get(Common.PATH).asString())
                    );
                }
            }
        }
        return JsonApiResponse.success();
    }

    public JsonApiResponse deleteIssuerByIssuerRef(String issuerRef) {
        return this.sendDelete(
                DbapiEndpointForCSLAutocrypt.ISSUER_DEL_BY_REF.endpoint() + issuerRef,
                null);
    }

    /**
     * Deletes the given issuer from the module and the DB
     *
     * @param issuerRef identifier of the issuer in dbapi db
     */
    public JsonApiResponse deleteIssuer(String issuerRef, Json body) {
        return this.sendDelete(
                DbapiEndpointForCSLAutocrypt.ISSUER_DEL_BY_REF.endpoint() + issuerRef,
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
        input.at(Role.CERTIFICATE_AUTHORITY_ID, certificateAuthorityId);
        ;
        input.at(Role.ROLE_JSON, body);

        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.ROLE.endpoint(),
                input);
    }

    /**
     * Deletes the list of role
     *
     * @param roles list of roles to delete
     */
    public JsonApiResponse deleteRoles(Json roles) {
        if (roles.isArray()) {
            for (Json role : roles.asJsonList()) {
                if (role.has(Common.NAME) && role.has(Common.PATH)) {
                    deleteRole(role.get(Common.NAME).asString(), role.get(Common.PATH).asString(), null);
                }
            }
        }
        return JsonApiResponse.success();
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
     * Creates or modifies the roles in the database
     */
    public JsonApiResponse upsertRoles(Json roles) {
        if( roles!=null && roles.isArray() && roles.asJsonList().isEmpty()) { return JsonApiResponse.success();}
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.ROLE_UPSERT.endpoint(),
                formatListOfRoles(roles));
    }

    /**
     * Fetch the last update date for the roles
     */
    public JsonApiResponse getRoleLastUpdateDate() {
        return this.sendGet(
                DbapiEndpointForCSLAutocrypt.ROLE_GET_LAST_UPDATE_DATE.endpoint(),
                null);
    }

    /**
     * Updates the information of the given role
     *
     * @param name        name of the role
     * @param path        identifier of the role  (unique couple name,path)
     * @param description description of the role in the dbapi
     * @param body        parameters with the path and name of role, others?
     */
    public JsonApiResponse updateRole(String name, String description, String certificateAuthorityId, String path, Json body) {
        Json input = Json.object();
        input.at(Common.NAME, name);
        input.at(Common.DESCRIPTION, description);
        input.at(Role.CERTIFICATE_AUTHORITY_ID, certificateAuthorityId);
        ;
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
        if (description != null) {
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
    public JsonApiResponse generateCertificate(String serialNumber, String name, String vaultRoleId, String description, String path, Json body) {
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
     * @param params       parameters with the path
     */
    public JsonApiResponse revokeCertificate(String serialNumber, Json params) {
        return this.sendDelete(
                DbapiEndpointForCSLAutocrypt.CERTIFICATES_DEL_BY_SERIAL_NUMBER.endpoint() + serialNumber,
                params);
    }

    public JsonApiResponse setDeployedSuccessCertificate(String serialNumber) {
        Json params = Json.object();
        params.set(Certificate.SERIAL_NUMBER, serialNumber);
        return this.sendPut(
                DbapiEndpointForCSLAutocrypt.SET_IS_DEPLOYED_CERTIFICATE_SUCCESS.endpoint(),
                params
        );
    }

    public JsonApiResponse removeDeployedCertificate(String serialNumber) {
        Json params = Json.object();
        params.set(Certificate.SERIAL_NUMBER, serialNumber);
        return this.sendPut(
                DbapiEndpointForCSLAutocrypt.REMOVE_DEPLOYED_CERTIFICATE.endpoint(),
                params
        );
    }

    /**
     * Revokes the list of certificate
     *
     * @param certificates list of certificates to delete
     */
    public JsonApiResponse revokeCertificates(Json certificates) {
        if (certificates.isArray()) {
            for (Json certificate : certificates.asJsonList()) {
                if (certificate.has(Certificate.SERIAL_NUMBER) && certificate.has(Common.PATH)) {
                    revokeCertificate(certificate.get(Certificate.SERIAL_NUMBER).asString(),
                            Json.object(Common.PATH, certificate.get(Common.PATH).asString())
                    );
                }
            }
        }
        return JsonApiResponse.success();
    }

    /**
     * Creates or modifies the certificates in the database
     */
    public JsonApiResponse upsertCertificates(Json certificates) {
        if(certificates.isArray() && certificates.asJsonList().isEmpty()) { return JsonApiResponse.success();}
        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CERTIFICATES_UPSERT.endpoint(),
                formatListOfCertificates(certificates));
    }

    /**
     * Fetch the last update date for the certificates
     */
    public JsonApiResponse getCertificateLastUpdateDate() {
        return this.sendGet(
                DbapiEndpointForCSLAutocrypt.CERTIFICATES_GET_LAST_UPDATE_DATE.endpoint(),
                null);
    }

    /**
     * Generate root CA
     *
     * @param issuerRef   serial number of the CA
     * @param name        name of the CA
     * @param description description of the CA in the dbapi
     * @param body        body of the request with commonName, ttl, and optionally others
     */
    public JsonApiResponse generateRootCA(String issuerRef, String name, String description, String serialNumber, Json certificate, Json body) {
        return generateCA(issuerRef, name, Common.PKI, description, serialNumber, certificate, body);
    }

    /**
     * Generate intermediate CA
     *
     * @param name        name of the CA
     * @param description description of the CA in the dbapi
     * @param body        parameters with commonName, ttl, and optionally path
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
     * @param issuerRef   serial number of the CA
     * @param name        name of the CA
     * @param path        path of the CA
     * @param description description of the CA in the dbapi
     * @param body        parameters with commonName, ttl, and optionally path
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

    public static String getTTL(long expirationTimestamp) {
        Instant expirationInstant = Instant.ofEpochSecond(expirationTimestamp);
        Instant now = Instant.now();

        // Calculate the difference in seconds
        long diffSeconds = expirationInstant.getEpochSecond() - now.getEpochSecond();

        // Convert to hours
        long hours = diffSeconds / 3600;
        if (hours < 0) {
            // make it positive if it is negative
            hours = -hours;
        }
        return hours + "h";
    }

    public Json getCaJsonToSetInDbApi(Json certificateAuthOb) {
        Json result = Json.object();
        result.set("name", certificateAuthOb.get("issuer_name"));
        result.set("path", certificateAuthOb.get(Common.PATH));
        result.set("usage", certificateAuthOb.get("usage"));
        result.set("state", certificateAuthOb.get("cert_ou"));
        result.set("key_id", certificateAuthOb.get("key_id"));
        result.set("country", certificateAuthOb.get("cert_country"));
        result.set("ip_sans", certificateAuthOb.get("ip_sans"));
        result.set("is_root", certificateAuthOb.get("is_root"));
        result.set("revoked", certificateAuthOb.get("revoked"));
        result.set("key_bits", certificateAuthOb.get("cert_key_bits"));
        result.set("key_type", certificateAuthOb.get("cert_key_type"));
        result.set("locality", certificateAuthOb.get("cert_locality"));
        result.set("issuer_id", certificateAuthOb.get("issuer_id"));
        result.set("issuer_ref", certificateAuthOb.get("issuer_ref"));
        result.set("expiration", certificateAuthOb.get("cert_expiration"));
        result.set("not_after", certificateAuthOb.get("cert_not_after"));
        result.set("issuing_ca", certificateAuthOb.get("certificate"));
        result.set("certificate", certificateAuthOb.get("certificate"));
        result.set("common_name", certificateAuthOb.get("cert_common_name"));
        result.set("postal_code", certificateAuthOb.get("cert_postal_code"));
        result.set("organization", certificateAuthOb.get("cert_organization"));
        result.set("serial_number", certificateAuthOb.get("cert_serial_number"));
        result.set("street_address", certificateAuthOb.get("cert_street_address"));
        result.set("revocation_signature_algorithm", certificateAuthOb.get("cert_signature_algorithm"));
        // TODO: WHAT TO DO WITH TTL AND TTL UNIT
        result.set("ttl", getTTL(certificateAuthOb.get("cert_expiration").asLong()));
        result.set("ttl_unit", "years");
        return result;
    }

    public JsonApiResponse generateCaAfterImport(Json certificateAuthObj) {
        Json input = Json.object();
        input.set(Common.COMMON_NAME, certificateAuthObj.get("issuer_name"));
        input.set(Common.NAME, certificateAuthObj.get("issuer_name"));
        input.set(Common.DESCRIPTION, certificateAuthObj.get("description"));
        input.set(Issuer.ISSUER_REF, certificateAuthObj.get("issuer_ref"));
        input.set(Common.PATH, certificateAuthObj.get(Common.PATH));
        input.set(Certificate.SERIAL_NUMBER, certificateAuthObj.get("cert_serial_number"));
        input.set(Certificate.CERTIFICATE_OBJECT, certificateAuthObj.get("certificate_details"));
        input.set(Issuer.CA_JSON, getCaJsonToSetInDbApi(certificateAuthObj));
        String ca_type = certificateAuthObj.get("ca_type").asString();
        if (ca_type.equals("root")) {
            input.set("is_root", true);
        } else {
            input.set("is_root", false);
        }

        return this.sendPost(
                DbapiEndpointForCSLAutocrypt.CA.endpoint(),
                input);
    }
}
