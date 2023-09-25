package com.csl.intercom.cslscan.enums;

public enum ScanApiEndpoint {
    ENTITY("/entity/"),
    ENTITY_DETAILS(ENTITY.endpoint() + "%s"),
    ENTITY_LAST_UPDATE(ENTITY.endpoint() + "last_update"),
    ENTITY_LAST_DELETION(ENTITY.endpoint() + "last_deletion"),
    CPE_ITEM("/cpeItem/"),
    CPE_ITEM_DETAILS(CPE_ITEM.endpoint() + "%s"),
    CPE_ITEM_LAST_DELETION(CPE_ITEM.endpoint() + "last_deletion"),
    ENTITY_CPE_ITEMS(CPE_ITEM.endpoint() + "entity/%s"),
    DISCOVERY_STATUS("/discovery/status"),
    DISCOVERY_STATUS_DETAILS(DISCOVERY_STATUS.endpoint() + "/%s"),
    ENTITY_SCAN_STATUS("/status/entity/%s"),
    ENTITY_HTTP_CONNECTION("/entityHttpConnection/"),
    ENTITY_HTTP_CONNECTION_DETAILS(ENTITY_HTTP_CONNECTION.endpoint() + "%s"),
    DROP_COLLECTION("/%s/drop")
    ;

    private String endpoint;

    private ScanApiEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String endpoint() {
        return endpoint;
    }
}
