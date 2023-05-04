package com.csl.intercom.dbapi.enums;

public enum DbapiEndpoint {
    CPE_ITEMS("/cpe_discovered_items"),
    CPE_ITEMS_LAST_DATE("/cpe_discovered_items/get_last_discovered_date"),
    DELETED_CPE_ITEMS("/cpe_discovered_items/get_deleted_cpe_items"),
    DEVICES("/devices"),
    DELETED_DEVICES("/devices/get_deleted_devices"),
    CONNECTIONS("/connections"),
    DISCOVERY_PROTOCOLS("/cpe_discovery_api"),
    SCAN_EVENTS("/scan_events")
    ;

    private final String endpoint;

    DbapiEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
