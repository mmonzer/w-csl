package com.csl.autocrypt.enums;

/**
 * The various endpoints used inAutoCrypt api
 */
public enum ApiEndpointForCSLAutocrypt {

    ISSUER_URI("/api/issuer"),
    ISSUER_URI_(ISSUER_URI+"/"),
    ISSUER_URI_IMPORT(ISSUER_URI_+"import"),

    ROLE_URI("/api/role"),
    ROLE_URI_(ROLE_URI+"/"),

    MISC_URI("/api/general"),
    MISC_URI_ACTIVATE_OCSP(MISC_URI+"/activate-ocsp"),
    MISC_URI_IS_ALIVE(MISC_URI+"/health-check"),

    CERT_URI("/api/certificate"),
    CERT_URI_(CERT_URI+"/"),
    CERT_URI_TEMPLATE(CERT_URI_+"validate-template"),
    CERT_URI_DOWNLOAD_(CERT_URI_+"download/"),
    CERT_URI_GET_WO_PK_(CERT_URI_+"raw/"),
    CERT_URI_GET_WITH_PK_(CERT_URI_+"raw-with-private-key/"),
    CERT_URI_ISSUE(CERT_URI_+"issue"),
    CERT_URI_REVOKE_(CERT_URI_+"revoke/"),

    CA_URI("/api/ca"),
    CA_URI_GENERATE_INTER(CA_URI+"/generate-intermediate"),
    CA_URI_GENERATE_ROOT(CA_URI+"/generate-root")
    ;

    private final String endpoint;

    ApiEndpointForCSLAutocrypt(String endpoint) {
        this.endpoint = endpoint;
    }

    public String endpoint() {
        return endpoint;
    }

    @Override
    public String toString() {
        return endpoint;
    }
}
