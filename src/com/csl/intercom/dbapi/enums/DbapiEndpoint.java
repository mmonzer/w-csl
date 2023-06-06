package com.csl.intercom.dbapi.enums;

/**
 * The various endpoints used in DB-API.
 */
public enum DbapiEndpoint {
    EVENTS("/events"),
    CPE_ITEMS_LAST_DATE(EVENTS.getEndpoint() + "/last_discovery_date"),
    CPE_ITEMS("/cpe_discovered_items"),
    CREATE_CPE_ITEMS(CPE_ITEMS.getEndpoint() + "/create_discovery_cpe_entities"),
    GET_DELETED_CPE_ITEMS(CPE_ITEMS.getEndpoint() + "/get_deleted_cpe_items"),
    NO_NEW_CPE_ITEM(CPE_ITEMS.getEndpoint() + "/notify_sync_ended_with_no_discovered_cpe"),
    DELETE_CPE_ITEMS(CPE_ITEMS.getEndpoint() + "/delete_by_mongo_ids"),
    DEVICES("/devices"),
    DELETED_DEVICES(DEVICES.getEndpoint() + "/get_deleted_devices"),
    CONNECTIONS("/connections"),
    DISCOVERY_PROTOCOLS("/cpe_discovery_api"),
    SCAN_EVENT_CREATION(EVENTS.getEndpoint() + "/create_discovery_scan_event"),
    SCAN_EVENT_UPDATE(EVENTS.getEndpoint() + "/%d/update_discovery_scan_event"),
    ;

    private final String endpoint;

    DbapiEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
