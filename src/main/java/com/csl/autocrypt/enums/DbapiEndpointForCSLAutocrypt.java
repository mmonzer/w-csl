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
    ROLE_UPSERT(ROLE_+"create_vault_roles_entities"),
    ROLE_GET_LAST_UPDATE_DATE(ROLE_+"last_updated_date"),
    ROLE_UPD_BY_NAME_AND_PATH(ROLE_+"update_vault_role_by_name_and_path"),
    ROLE_DEL_BY_NAME_AND_PATH(ROLE_+"delete_vault_role_by_name_and_path"),
    CERTIFICATES(SELF+"/certificates"),
    CERTIFICATES_(CERTIFICATES+"/"),
    CERTIFICATES_UPSERT(CERTIFICATES_+"create_certificates_entities"),
    CERTIFICATES_GET_LAST_UPDATE_DATE(CERTIFICATES_+"get_last_updated_date_for_certificates_not_related_to_ca"),
    CERTIFICATES_UPD_BY_SERIAL_NUMBER_(CERTIFICATES_+"update_by_serial_number/"),
    CERTIFICATES_DEL_BY_SERIAL_NUMBER_(CERTIFICATES_+"delete_by_serial_number/"),
    ISSUER(SELF+"/certificate_authorities"),
    ISSUER_(ISSUER+"/"),
    ISSUER_UPSERT(ISSUER_+"create_certificate_authorities_entities"),
    ISSUER_GET_LAST_UPDATE_DATE(ISSUER_+"last_updated_date"),
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
