package com.csl.autocrypt.enums;

/**
 * Instead of an enum it's a final class with all the static variables as enums
 * This seems more useful when only one attribute as they are used as the type and not need conversions
 */
public final class AutocryptConstants {
    public static final String LIST_DELIMITER = ",";

    public static final class Common {
        public static final String DESCRIPTION = "description";
        public static final String FILE = "file";
        public static final String IP = "ip";
        public static final String PORT = "port";
        public static final String IS_HTTP_API_KEY_REACHABLE = "is_http_api_reachable";
        public static final String NAME = "name";
        public static final String COMMON_NAME = "common_name";
        public static final String ID = "id";
        public static final String COUNTRY = "country";
        public static final String LOCALITY = "locality";
        public static final String ORGANIZATION = "organization";
        public static final String ORGANIZATION_UNIT = "organization_unit";
        public static final String OU = "ou";
        public static final String POSTAL_CODE = "postal_code";
        public static final String PROVINCE = "province";
        public static final String STATE = "state";
        public static final String STREET_ADDRESS = "street_address";
        public static final String PATH = "path";
        public static final String PKI = "pki";
        public static final String TTL = "ttl";
        public static final String TTL_UNIT = "ttl_unit";
        public static final String TYPE = "type";
        public static final String CRL_DISTRIBUTION_POINTS = "crl_distribution_points";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
        public static final String VAULT_ID = "vault_id";

        public static final String VALUE = "value";
        public static final String AFTER_UPDATED_DATE = "after_updated_date";
        public static final String MIN_DATE = "1900-01-01T00:00+18:00";
        public static final String DELETE = "delete";
        public static final String DELETED = "deleted";
        public static final String NON_DELETED = "non_deleted";
        public static final String GET_DELETED = "get_deleted";
        public static final String CREATEDAT = "createdAt";
        public static final String UPDATEDAT = "updatedAt";
        private Common(){}
    }

    public static final class Issuer {
        public static final String CA_JSON = "ca_json";
        public static final String CA_TYPE = "ca_type";
        public static final String IMPORTED_ISSUERS = "imported_issuers";
        public static final String ROOT = "root";
        public static final String IS_ROOT = "is_root";
        public static final String INTERMEDIATE = "intermediate";
        public static final String INTERNAL = "internal";
        public static final String IP_SANS = "ip_sans";
        public static final String URI_SANS = "uri_sans";
        public static final String ISSUER_ID = "issuer_id";
        public static final String ISSUER_NAME = "issuer_name";
        public static final String ISSUER_REF = "issuer_ref";
        public static final String NOT_BEFORE_DURATION = "not_before_duration";
        public static final String NOT_AFTER = "not_after";
        public static final String OCSP_SERVERS = "ocsp_servers";
        public static final String PEM_BUNDLE = "pem_bundle";
        private Issuer(){}
    }

    public static final class Role {
        public static final String ALLOW_TOKEN_DISPLAYNAME = "allow_token_displayname";
        public static final String ROLE_JSON = "role_json";
        public static final String ROLE_NAME = "role_name";
        public static final String KEY_TYPE = "key_type";
        public static final String VAULT_ID = "vault_id";
        public static final String VAULT_ROLE_NAME = "vault_role_name";
        public static final String CERTIFICATE_AUTHORITY = "certificate_authority";
        public static final String CERTIFICATE_AUTHORITY_ID = "certificate_authority_id";
        private Role(){}
    }

    public static final class Certificate {
        public static final String SERIAL_NUMBER = "serial_number";
        public static final String CERTIFICATE = "certificate";
        public static final String CERTIFICATE_JSON = "certificate_json";
        public static final String CERTIFICATE_OBJECT = "certificate_obj_json";
        public static final String VAULT_ROLE = "vault_role";
        public static final String VAULT_ROLE_ID = "vault_role_id";
        public static final String WITH_PRIVATE_KEY = "with_private_key";
        public static final String CA_CHAIN = "ca_chain";
        public static final String EXPIRATION = "expiration";
        public static final String ISSUING_CA = "issuing_ca";
        public static final String PRIVATE_KEY = "private_key";
        public static final String PRIVATE_KEY_TYPE = "private_key_type";
        public static final String IS_REVOKED = "is_revoked";
        public static final String ISREVOKED = "isRevoked";
        public static final String REVOKE = "revoke";
        private Certificate(){}
    }

    public static final class Device {
        public static final String CERTIFICATE_SERIAL_NUMBER = "certificate_serial_number";
        public static final String CERTIFICATE_PATH = "certificate_path";
        public static final String VENDOR = "vendor";
        public static final String IP = "ip";
        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";
        private Device(){}
    }



    /**
     * Unused constructor (mais private)
     */
    private AutocryptConstants() {
    }
}
