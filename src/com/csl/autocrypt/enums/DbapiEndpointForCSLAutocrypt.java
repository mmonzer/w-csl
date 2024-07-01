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
    CERTIFICATES_UPD_BY_SERIAL_NUMBER_(CERTIFICATES_+"update_by_serial_number/"),
    CERTIFICATES_DEL_BY_SERIAL_NUMBER_(CERTIFICATES_+"delete_by_serial_number/"),
    ISSUER(SELF+"/certificate_authorities"),
    ISSUER_(ISSUER+"/"),
    ISSUER_UPT_BY_REF_(ISSUER_+"update_by_issuer_ref/"),
    ISSUER_DEL_BY_REF_(ISSUER_+"delete_by_issuer_ref/"),
    ;

    private final String endpoint;

    DbapiEndpointForCSLAutocrypt(String endpoint) {
        this.endpoint = endpoint;
    }

    public String endpoint() {
        return endpoint;
    }

    public String toString() {
        return endpoint;
    }
}
