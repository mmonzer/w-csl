package com.csl.autocrypt.enums;

/**
 * The various endpoints used in DB-API.
 */
public enum DbapiEndpointForCSLAutocrypt {
    SELF("/autocrypt"),
    CA(SELF+"/certificate_authorities"),
    CA_(CA+"/"),
    ROLE(SELF+"/vault_roles"),
    ROLE_(ROLE+"/"),
    CERTIFICATES(SELF+"/certificates"),
    CERTIFICATES_(CERTIFICATES+"/"),
    ISSUER(SELF+"/issuer"),
    ISSUER_(ISSUER+"/"),
    ;

    private final String endpoint;

    DbapiEndpointForCSLAutocrypt(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String toString() {
        return endpoint;
    }
}
