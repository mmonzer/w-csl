package com.csl.autocrypt.enums;

/**
 * The various endpoints used in DB-API.
 */
public enum DbapiEndpointForCSLAutocrypt {
    SELF("/autocrypt"),
    CA(SELF+"/certificate_authorities"),
    ROLE(SELF+"/vault_roles"),
    ROLE_UPSERT(ROLE+"/create_vault_roles_entities"),
    ROLE_GET_LAST_UPDATE_DATE(ROLE+"/last_updated_date"),
    ROLE_UPD_BY_NAME_AND_PATH(ROLE+"/update_vault_role_by_name_and_path"),
    ROLE_DEL_BY_NAME_AND_PATH(ROLE+"/delete_vault_role_by_name_and_path"),
    CERTIFICATES(SELF+"/certificates"),
    CERTIFICATES_UPSERT(CERTIFICATES+"/create_certificates_entities"),
    CERTIFICATES_GET_LAST_UPDATE_DATE(CERTIFICATES+"/get_last_updated_date_for_certificates_not_related_to_ca"),
    CERTIFICATES_UPD_BY_SERIAL_NUMBER(CERTIFICATES+"/update_by_serial_number/"),
    CERTIFICATES_DEL_BY_SERIAL_NUMBER(CERTIFICATES+"/delete_by_serial_number/"),
    ISSUER(SELF+"/certificate_authorities"),
    ISSUER_UPSERT(ISSUER+"/create_certificate_authorities_entities"),
    ISSUER_GET_LAST_UPDATE_DATE(ISSUER+"/last_updated_date"),
    ISSUER_UPT_BY_REF(ISSUER+"/update_by_issuer_ref/"),
    ISSUER_DEL_BY_REF(ISSUER+"/delete_by_issuer_ref/"),
    SET_IS_DEPLOYED_CERTIFICATE_SUCCESS(CERTIFICATES+"/set_is_deployed_certificate_success"),
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
