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
    CPE_ITEM_DELETE_MANY(CPE_ITEM.endpoint() + "deleteMany"),
    CPE_ITEM_DELETE_MANY_HARD(CPE_ITEM_DELETE_MANY.endpoint() + "?hardDelete=true"),
    CPE_ITEM_HARD_DELETE_BEFORE(CPE_ITEM.endpoint() + "hardDeleteBefore"),
    CPE_ITEM_LAST_DELETION(CPE_ITEM.endpoint() + "last_deletion"),
    ENTITY_CPE_ITEMS(CPE_ITEM.endpoint() + "entity/%s"),
    MICROSOFT_KB("/microsoftKb/"),
    MICROSOFT_KB_DETAILS(MICROSOFT_KB.endpoint() + "%s"),
    MICROSOFT_KB_DELETE_MANY(MICROSOFT_KB.endpoint() + "deleteMany"),
    MICROSOFT_KB_DELETE_MANY_HARD(MICROSOFT_KB_DELETE_MANY.endpoint() + "?hardDelete=true"),
    MICROSOFT_KB_HARD_DELETE_BEFORE(MICROSOFT_KB.endpoint() + "hardDeleteBefore"),
    MICROSOFT_KB_LAST_DELETION(MICROSOFT_KB.endpoint() + "last_deletion"),
    MICROSOFT_KB_LAST_UPDATE(MICROSOFT_KB.endpoint() + "last_update"),
    ENTITY_MICROSOFT_KB(MICROSOFT_KB.endpoint() + "entity/%s"),
    DISCOVERY_STATUS("/discovery/status"),
    DISCOVERY_STATUS_DETAILS(DISCOVERY_STATUS.endpoint() + "/%s"),
    ENTITY_SCAN_STATUS("/status/entity/%s"),
    ENTITY_HTTP_CONNECTION("/entityHttpConnection/"),
    ENTITY_HTTP_CONNECTION_DETAILS(ENTITY_HTTP_CONNECTION.endpoint() + "%s"),
    ENTITY_HTTP_CONNECTION_FETCH_STAGE(ENTITY_HTTP_CONNECTION.endpoint() + "fetchStagePage"),
    ENTITY_HTTP_CONNECTION_TEST(ENTITY_HTTP_CONNECTION.endpoint() + "test"),
    ENTITY_HTTP_CONNECTION_FETCH_PREDEFINED_VARIABLES(ENTITY_HTTP_CONNECTION.endpoint() + "fetchPredefinedVariables"),
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
