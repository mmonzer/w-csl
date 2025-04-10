package com.csl.autocrypt.enums;

/**
 * The various endpoints used inAutoCrypt api
 */
public enum ApiEndpointForCSLAutocrypt {

    ISSUER_URI("/api/issuer"),
    ISSUER_URI_SLASH(ISSUER_URI+"/"),
    ISSUER_URI_IMPORT(ISSUER_URI_SLASH +"import"),

    ROLE_URI("/api/role"),
    ROLE_URI_SLASH(ROLE_URI+"/"),

    MISC_URI("/api/general"),
    MISC_URI_ACTIVATE_OCSP(MISC_URI+"/activate-ocsp"),
    MISC_URI_IS_ALIVE(MISC_URI+"/health-check"),

    CERT_URI("/api/certificate"),
    CERT_URI_SLASH(CERT_URI+"/"),
    CERT_URI_NOT_REVOKED(CERT_URI_SLASH +"not-revoked"),
    CERT_URI_REVOKED(CERT_URI_SLASH +"revoked"),
    CERT_URI_TEMPLATE(CERT_URI_SLASH +"validate-template"),
    CERT_URI_DOWNLOAD(CERT_URI_SLASH +"download/"),
    CERT_URI_GET_WO_PK(CERT_URI_SLASH +"raw/"),
    CERT_URI_GET_WITH_PK(CERT_URI_SLASH +"raw-with-private-key/"),
    CERT_URI_ISSUE(CERT_URI_SLASH +"issue"),
    CERT_SIGN_CSR(CERT_URI_SLASH +"sign-csr"),
    CERT_URI_REVOKE(CERT_URI_SLASH +"revoke/"),
    CERT_URI_DEPLOY_CERTIFICATE(CERT_URI_SLASH +"deploy-certificate"),

    CA_URI("/api/ca"),
    CA_URI_GENERATE_INTER(CA_URI+"/generate-intermediate"),
    CA_URI_GENERATE_ROOT(CA_URI+"/generate-root"),
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
