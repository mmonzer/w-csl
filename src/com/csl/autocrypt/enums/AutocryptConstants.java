package com.csl.autocrypt.enums;

/**
 * Instead of an enum it's a final class with all the static variables as enums
 * This seems more useful when only one attribute as they are used as the type and not need conversions
 */
public final class AutocryptConstants {

    // Alphabetic order
    public static final String CA_JSON = "ca_json";
    public static final String CA_TYPE = "ca_type";
    public static final String CERTIFICATE = "certificate";
    public static final String CERTIFICATE_AUTHORITY = "certificate_authority";
    public static final String CERTIFICATE_AUTHORITY_ID = "certificate_authority_id";
    public static final String CERTIFICATE_JSON = "certificate_json";
    public static final String COMMON_NAME = "common_name";
    public static final String COUNTRY = "country";
    public static final String CREATED_AT = "created_at";
    public static final String DESCRIPTION = "description";
    public static final String FILE = "file";
    public static final String ID = "id";
    public static final String IMPORTED_ISSUERS = "imported_issuers";
    public static final String INTERMEDIATE = "intermediate";
    public static final String INTERNAL = "internal";
    public static final String IP = "ip";
    public static final String IS_HTTP_API_KEY_REACHABLE = "is_http_api_reachable";
    public static final String ISSUER_ID = "issuer_id";
    public static final String ISSUER_NAME = "issuer_name";
    public static final String ISSUER_REF = "issuer_ref";
    public static final String KEY_TYPE = "key_type";
    public static final String LOCALITY = "locality";
    public static final String NAME = "name";
    public static final String OCSP_SERVERS = "ocsp_servers";
    public static final String ORGANIZATION = "organisation";
    public static final String PATH = "path";
    public static final String PEM_BUNDLE = "pem_bundle";
    public static final String PKI = "pki";
    public static final String PORT = "port";
    public static final String ROLE_JSON = "role_json";
    public static final String ROLE_NAME = "role_name";
    public static final String ROOT = "root";
    public static final String SERIAL_NUMBER = "serial_number";
    public static final String TTL = "ttl";
    public static final String TTL_UNIT = "ttl_unit";
    public static final String TYPE = "type";
    public static final String UPDATED_AT = "updated_at";
    public static final String VAULT_ID = "vault_id";
    public static final String VAULT_ROLE_ID = "vault_role_id";
    public static final String VAULT_ROLE_NAME = "vault_role_name";

    /**
     * Unused constructor (mais private)
     */
    private AutocryptConstants() {}
}
