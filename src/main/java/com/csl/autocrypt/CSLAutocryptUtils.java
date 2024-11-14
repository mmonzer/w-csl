package com.csl.autocrypt;

import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import org.jetbrains.annotations.NotNull;

import static com.csl.autocrypt.ConvertDapiVault.transformKeysFromVaultToDbapi;
import static com.csl.autocrypt.enums.AutocryptConstants.*;
import static com.ucsl.json.JsonUtil.*;

public class CSLAutocryptUtils {
    private CSLAutocryptUtils() {}

    /**
     * Reformats the AutoCrypt error to resend only the user-friendly error
     *
     * @param response raw response
     * @return reformated response
     */
    public static @NotNull JsonApiResponse reformatAutoCryptError(JsonApiResponse response) {
        String errorMessage = Json.read(response.getError().getDetails().get("content").toString()).toString();
        return JsonApiResponse.error(errorMessage);
    }

    /**
     * Reformats the AutoCrypt response depending on the possible error
     *
     * @param response raw response
     * @return reformated response
     */
    public static JsonApiResponse cleanApiResponse(JsonApiResponse response) {
        if (response.isSuccess()) {
            return response;
        } else {
            try {
                return reformatAutoCryptError(response);
            } catch (Exception ignored) {
                return response;
            }
        }
    }

    /**
     * Converts the ttl in seconds to hours in format "xxxh".
     */
    public static void convertTTLSecondsToStrHours(Json obj) {
        if (!obj.isNull() && obj.has(Common.TTL) && obj.get(Common.TTL).isNumber()) {
            obj.set(Common.TTL, obj.get(Common.TTL).asInteger() / 3600 + "h");
        }
    }

    /**
     * From a list of raw issuer (json with all fields at same level), it formats to dbapi format:
     * <code>[{"common_name":"", "description":"", "issuer_ref":"","path":"", "serial_number":"", "ca_json":{...}, "certificate_object":{...}},...]</code>
     *
     * @param issuers list of issuers in plain json format
     * @return list of issuers in the right format
     */
    public static Json formatListOfIssuers(Json issuers) {
        Json list = Json.array();
        for (Json issuer : issuers.asJsonList()) {
            list.add(formatIssuer(issuer));
        }
        return list;
    }

    /**
     * From a raw issuer (json with all fields at same level), it formats to dbapi format:
     * <code>{"common_name":"", "description":"", "issuer_ref":"","path":"", "serial_number":"", "ca_json":{...}, "certificate_object":{...}}</code>
     *
     * @param issuerRaw issuer in plain json format
     * @return issuer in the right format
     */
    public static Json formatIssuer(Json issuerRaw) {
        Json issuer = Json.object();
        drop(issuerRaw, Common.CREATEDAT, Common.UPDATEDAT);
        transformKeysFromVaultToDbapi(issuerRaw, Common.OU, Common.PROVINCE);
        copyValueOrNull(issuerRaw, issuer, Common.COMMON_NAME, Common.NAME, Common.DESCRIPTION, Issuer.ISSUER_REF, Issuer.ISSUER_ID, Issuer.IS_ROOT, Common.PATH, Certificate.SERIAL_NUMBER);
        Json certificate = Json.object();
        copyValueOrNull(issuerRaw, certificate, Certificate.CA_CHAIN, Certificate.CERTIFICATE, Certificate.EXPIRATION, Certificate.ISSUING_CA, Certificate.PRIVATE_KEY, Certificate.PRIVATE_KEY_TYPE, Certificate.SERIAL_NUMBER);
        certificate.set(Common.PATH, Common.PKI);
        issuer.set(Issuer.CA_JSON, issuerRaw);
        issuer.set(Certificate.CERTIFICATE_OBJECT, certificate);
        return issuer;
    }

    /**
     * From a list of raw roles (json with all fields at same level), it formats to dbapi format:
     * <code>[{"name":"", "description":"", "CERTIFICATE_AUTHORITY_ID":1,"role_json":{...}},...]</code>
     *
     * @param roles list of roles in plain json format
     * @return list of roles in the right format
     */
    public static Json formatListOfRoles(Json roles) {
        Json list = Json.array();
        for (Json role : roles.asJsonList()) {
            list.add(formatRole(role));
        }
        return list;
    }

    /**
     * From a list of raw roles (json with all fields at same level), it formats to dbapi format:
     * <code>[{"name":"", "description":"", "CERTIFICATE_AUTHORITY_ID":1,"role_json":{...}},...]</code>
     *
     * @param roleRaw role in plain json format
     * @return role in the right format
     */
    public static Json formatRole(Json roleRaw) {
        Json role = Json.object();
        drop(roleRaw, Common.CREATEDAT, Common.UPDATEDAT);
        transformKeysFromVaultToDbapi(roleRaw, Common.OU, Common.PROVINCE);
        copyValueOrNull(roleRaw, role, Common.NAME, Common.DESCRIPTION, Issuer.ISSUER_REF, Issuer.ISSUER_ID, Common.PATH);
        role.set(Role.ROLE_JSON, roleRaw);
        return role;
    }

    /**
     * From a list of raw certificates (json with all fields at same level), it formats to dbapi format:
     * <code>[{"name":"", "description":"", "CERTIFICATE_AUTHORITY_ID":1,"certificate_json":{...}},...]</code>
     *
     * @param certificates list of certificates in plain json format
     * @return list of certificates in the right format
     */
    public static Json formatListOfCertificates(Json certificates) {
        Json list = Json.array();
        for (Json certificate : certificates.asJsonList()) {
            if (!certificate.has(Common.NAME) || certificate.get(Common.NAME) == null || (certificate.has(Common.NAME) && certificate.get(Common.NAME).isNull())) {
                certificate.set(Common.NAME, certificate.get(Certificate.SERIAL_NUMBER));
            }
            list.add(formatCertificate(certificate));
        }
        return list;
    }

    /**
     * From a list of raw certificates (json with all fields at same level), it formats to dbapi format:
     * <code>[{"name":"", "description":"", "CERTIFICATE_AUTHORITY_ID":1,"certificate_json":{...}},...]</code>
     *
     * @param certificateRaw certificate in plain json format
     * @return certificate in the right format
     */
    public static Json formatCertificate(Json certificateRaw) {
        Json certificate = Json.object();
        certificateRaw.set(Certificate.IS_REVOKED, extractValueBooleanOrNull(certificateRaw,Certificate.ISREVOKED));
        copyValueOrNull(certificateRaw, certificate, Common.NAME, Common.DESCRIPTION, Certificate.SERIAL_NUMBER, Common.PATH, Certificate.VAULT_ROLE_ID, Certificate.IS_REVOKED);
        certificate.at(Certificate.CERTIFICATE_JSON, certificateRaw);
        return certificate;
    }
}
