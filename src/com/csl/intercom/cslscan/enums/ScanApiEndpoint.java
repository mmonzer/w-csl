package com.csl.intercom.cslscan.enums;

public enum ScanApiEndpoint {
    ENTITY("/entity/"),
    ENTITY_DETAILS(ENTITY.endpoint() + "%s"),
    ENTITY_LAST_UPDATE(ENTITY.endpoint() + "last_update"),
    ENTITY_LAST_DELETION(ENTITY.endpoint() + "last_deletion"),
    ENTITY_TEST_CONNECTION(ENTITY.endpoint() + "test_connection"),
    ENTITY_TEST_EXISTING_CONNECTION(ENTITY_TEST_CONNECTION.endpoint() + "/%s"),
    CPE_ITEM("/cpeItem/"),
    CPE_ITEM_DETAILS(CPE_ITEM.endpoint() + "%s"),
    CPE_ITEM_LAST_DELETION(CPE_ITEM.endpoint() + "last_deletion"),
    ENTITY_CPE_ITEMS(CPE_ITEM.endpoint() + "entity/%s"),
    DISCOVERY_STATUS("/discovery/status"),
    DISCOVERY_STATUS_DETAILS(DISCOVERY_STATUS.endpoint() + "/%s"),
    ENTITY_SCAN_STATUS("/status/entity/%s"),
    ENTITY_HTTP_CONNECTION("/entityHttpConnection/"),
    ENTITY_HTTP_CONNECTION_UUIDS(ENTITY_HTTP_CONNECTION.endpoint() + "uuids"),
    ENTITY_HTTP_CONNECTION_DETAILS(ENTITY_HTTP_CONNECTION.endpoint() + "%s"),
    ENTITY_HTTP_CONNECTION_FETCH_STAGE(ENTITY_HTTP_CONNECTION.endpoint() + "fetchStagePage"),
    ENTITY_HTTP_CONNECTION_TEST(ENTITY_HTTP_CONNECTION.endpoint() + "test"),
    ENTITY_HTTP_CONNECTION_FETCH_PREDEFINED_VARIABLES(ENTITY_HTTP_CONNECTION.endpoint() + "fetchPredefinedVariables"),
    ENTITY_HTTP_CONNECTION_IMPORT_BSON(ENTITY_HTTP_CONNECTION.endpoint() + "importBson"),
    ENTITY_HTTP_CONNECTION_IMPORT_BSON_STATUS(ENTITY_HTTP_CONNECTION.endpoint() + "importBson/status/%s"),
    ENTITY_HTTP_CONNECTION_GET_SYNC_NEEDED(ENTITY_HTTP_CONNECTION.endpoint() + "getEntityHttpConnectionsToSync"),
    ENTITY_HTTP_CONNECTION_SET_SYNC_NEEDED(ENTITY_HTTP_CONNECTION.endpoint() + "setEntityHttpConnectionsNeedsSync"),
    DROP_COLLECTION("/%s/drop"),
    ;

    private String endpoint;

    private ScanApiEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String endpoint() {
        return endpoint;
    }
}
