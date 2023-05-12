package com.csl.intercom.dbapi.enums;

/**
 * The various endpoints used in DB-API.
 */
public enum DbapiEndpoint {
    CPE_ITEMS("/cpe_discovered_items"),
    CPE_ITEMS_LAST_DATE(CPE_ITEMS.getEndpoint() + "/get_last_discovered_date"),
    DELETED_CPE_ITEMS(CPE_ITEMS.getEndpoint() + "/get_deleted_cpe_items"),
    DEVICES("/devices"),
    DELETED_DEVICES(DEVICES.getEndpoint() + "/get_deleted_devices"),
    CONNECTIONS("/connections"),
    DISCOVERY_PROTOCOLS("/cpe_discovery_api"),
    SCAN_EVENTS("/scan_events"),
    ;

    private final String endpoint;

    DbapiEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
