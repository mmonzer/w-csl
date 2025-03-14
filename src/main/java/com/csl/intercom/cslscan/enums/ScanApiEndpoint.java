package com.csl.intercom.cslscan.enums;

public enum ScanApiEndpoint {
    ENTITY("/entity/"),
    ENTITY_DETAILS(ENTITY.endpoint() + "%s"),
    ENTITY_LAST_UPDATE(ENTITY.endpoint() + "last_update"),
    ENTITY_LAST_DELETION(ENTITY.endpoint() + Constants.LAST_DELETION),
    ENTITY_TEST_CONNECTION(ENTITY.endpoint() + "test_connection"),
    ENTITY_TEST_EXISTING_CONNECTION(ENTITY_TEST_CONNECTION.endpoint() + "/%s"),
    ENTITY_DELETE_MULTIPLE_ENTITIES(ENTITY.endpoint() + "delete_multiple_entities"),
    ENTITY_DELETE_MULTIPLE_ENTITIES_HARD(ENTITY_DELETE_MULTIPLE_ENTITIES.endpoint() + "?hardDelete=true"),

    ENTITY_DELETED_ENTITIES(ENTITY.endpoint() + "deleted_entities"),
    ENTITY_DELETED_ENTITIES_UUIDS(ENTITY.endpoint() + "deleted_entities_uuid"),

    CONNECTIONS("/entity-connection-info/"),
    CREATE_CONNECTIONS(CONNECTIONS.endpoint() + "create_list_of_connections"),
    CONNECTIONS_DETAILS(CONNECTIONS.endpoint() + "%s"),
    CONNECTIONS_LAST_UPDATE(CONNECTIONS.endpoint() + "last-updated"),
    CLEAR_ALL_ENTITY_CONNECTIONS(CONNECTIONS.endpoint() + Constants.CLEAR),

    CONNECTIONS_DRAFT("/entity-connection-info-draft/"),
    CONNECTION_DRAFT_DETAILS(CONNECTIONS_DRAFT.endpoint() + "%s"),
    CREATE_CONNECTIONS_DRAFT(CONNECTIONS_DRAFT.endpoint() + "create_list_of_connections_draft"),
    CONNECTIONS_DRAFT_LAST_UPDATE(CONNECTIONS_DRAFT.endpoint() + "last-updated"),
    CLEAR_FAILED_CONNECTIONS_DRAFT(CONNECTIONS_DRAFT.endpoint() + "clear_failed"),
    CLEAR_VERIFIED_CONNECTIONS_DRAFT(CONNECTIONS_DRAFT.endpoint() + "clear_verified"),
    CLEAR_ALL_CONNECTIONS_DRAFT(CONNECTIONS_DRAFT.endpoint() + Constants.CLEAR),
    PUBLISH_ALL_VERIFIED_CONNECTION_DRAFT(CONNECTIONS_DRAFT.endpoint() + "publish_all_verified"),

    CPE_ITEM("/cpeItem/"),
    CPE_ITEM_DETAILS(CPE_ITEM.endpoint() + "%s"),
    CPE_ITEM_DELETE_MANY(CPE_ITEM.endpoint() + "deleteMany"),
    CPE_ITEM_DELETE_MANY_HARD(CPE_ITEM_DELETE_MANY.endpoint() + "?hardDelete=true"),
    CPE_ITEM_DELETE_ALL_SOFT_DELETED(CPE_ITEM.endpoint() + "deleteAllSoftDeleted"),
    CPE_ITEM_HARD_DELETE_BEFORE(CPE_ITEM.endpoint() + "hardDeleteBefore"),
    CPE_ITEM_LAST_DELETION(CPE_ITEM.endpoint() + Constants.LAST_DELETION),

    ENTITY_CPE_ITEMS(CPE_ITEM.endpoint() + "entity/%s"),

    MICROSOFT_KB("/microsoftKb/"),
    MICROSOFT_KB_DETAILS(MICROSOFT_KB.endpoint() + "%s"),
    MICROSOFT_KB_DELETE_MANY(MICROSOFT_KB.endpoint() + "deleteMany"),
    MICROSOFT_KB_DELETE_MANY_HARD(MICROSOFT_KB_DELETE_MANY.endpoint() + "?hardDelete=true"),
    MICROSOFT_KB_HARD_DELETE_BEFORE(MICROSOFT_KB.endpoint() + "hardDeleteBefore"),
    MICROSOFT_KB_LAST_DELETION(MICROSOFT_KB.endpoint() + Constants.LAST_DELETION),
    MICROSOFT_KB_LAST_UPDATE(MICROSOFT_KB.endpoint() + "last_update"),
    MICROSOFT_KB_DELETE_ALL_SOFT_DELETED(MICROSOFT_KB.endpoint() + "deleteAllSoftDeleted"),
    ENTITY_MICROSOFT_KB(MICROSOFT_KB.endpoint() + "entity/%s"),

    DISCOVERY("/discovery"),
    DISCOVERY_STATUS(DISCOVERY.endpoint() + "/status"),
    DISCOVERY_STATUS_DETAILS(DISCOVERY_STATUS.endpoint() + "/%s"),
    DISCOVERY_GET_CRON(DISCOVERY.endpoint() + "/getCron"),
    DISCOVERY_IS_CRON_ACTIVE(DISCOVERY.endpoint() + "/isCronActive"),
    DISCOVERY_SET_CRON_ACTIVE(DISCOVERY.endpoint() + "/setCronActive"),
    DISCOVERY_UPDATE_CRON(DISCOVERY.endpoint() + "/updateCron"),
    DISCOVERY_CANCEL(DISCOVERY.endpoint() + "/cancel"),
    ENTITY_SCAN_STATUS("/status/entity/%s"),
    ENTITY_HTTP_CONNECTION("/entityHttpConnection/"),
    ENTITY_HTTP_CONNECTION_UUIDS(ENTITY_HTTP_CONNECTION.endpoint() + "uuids"),
    ENTITY_HTTP_CONNECTION_DETAILS(ENTITY_HTTP_CONNECTION.endpoint() + "%s"),
    ENTITY_HTTP_CONNECTION_FETCH_STAGE(ENTITY_HTTP_CONNECTION.endpoint() + "fetchStagePage"),
    ENTITY_HTTP_CONNECTION_GET_INSTALLED_NPM_PACKAGES(ENTITY_HTTP_CONNECTION.endpoint() + "getInstalledNpmPackages"),
    ENTITY_HTTP_CONNECTION_TEST(ENTITY_HTTP_CONNECTION.endpoint() + "test"),
    ENTITY_HTTP_CONNECTION_FETCH_PREDEFINED_VARIABLES(ENTITY_HTTP_CONNECTION.endpoint() + "fetchPredefinedVariables"),
    ENTITY_HTTP_CONNECTION_IMPORT_BSON(ENTITY_HTTP_CONNECTION.endpoint() + "importBson"),
    ENTITY_HTTP_CONNECTION_IMPORT_BSON_STATUS(ENTITY_HTTP_CONNECTION.endpoint() + "importBson/status/%s"),
    ENTITY_HTTP_CONNECTION_GET_SYNC_NEEDED(ENTITY_HTTP_CONNECTION.endpoint() + "getEntityHttpConnectionsToSync"),
    ENTITY_HTTP_CONNECTION_SET_SYNC_NEEDED(ENTITY_HTTP_CONNECTION.endpoint() + "setEntityHttpConnectionsNeedsSync"),
    ENTITY_HTTP_CONNECTION_EXPORT_BSON(ENTITY_HTTP_CONNECTION.endpoint() + "exportBson"),
    ENTITY_HTTP_CONNECTION_EXPORT_BSON_STATUS(ENTITY_HTTP_CONNECTION_EXPORT_BSON.endpoint() + "/status/%s"),
    ENTITY_HTTP_CONNECTION_EXPORT_BSON_DOWNLOAD(ENTITY_HTTP_CONNECTION_EXPORT_BSON.endpoint() + "/file/%s"),
    ENTITY_HTTP_CONNECTION_EXPORT_BSON_DELETE(ENTITY_HTTP_CONNECTION_EXPORT_BSON.endpoint() + "/file/%s"),
    DROP_COLLECTION("/%s/drop"),
    EXTERNAL_DISCOVERY("/externalDiscovery"),
    EXTERNAL_CONNECTION_INFO_TEMPLATES(EXTERNAL_DISCOVERY.endpoint() + "/connectionInfoTemplates"),
    EXTERNAL_CONNECTION_INFOS(EXTERNAL_DISCOVERY.endpoint() + "/connectionInfo"),
    EXTERNAL_CONNECTION_INFO_DETAILS(EXTERNAL_DISCOVERY.endpoint() + "/connectionInfo/%s"),
    EXTERNAL_CONNECTION_INFO_CLEAR(EXTERNAL_DISCOVERY.endpoint() + "/connectionInfo/clear"),
    EXTERNAL_DISCOVERY_START_SCAN(EXTERNAL_DISCOVERY.endpoint() + "/startDiscovery/%s"),
    EXTERNAL_DISCOVERED_DEVICES("/externalDiscoveredDevices/"),
    EXTERNAL_DISCOVERED_DEVICES_CLEAR(EXTERNAL_DISCOVERED_DEVICES.endpoint() + Constants.CLEAR),
    EXTERNAL_PUBLISH_DISCOVERED_DEVICES(EXTERNAL_DISCOVERED_DEVICES.endpoint() + Constants.PUBLISH),
    CERTIFICATE_CONNECTION("/connexion-certificate/"),
    CERTIFICATE_CONNECTION_DETAILS(CERTIFICATE_CONNECTION.endpoint() + "%s"),
    ;

    private final String endpoint;

    private ScanApiEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String endpoint() {
        return endpoint;
    }

    public String toString() {
        return endpoint;
    }

    private static class Constants {
        public static final String CLEAR = "clear";
        public static final String LAST_DELETION = "last_deletion";
        public static final String PUBLISH = "publish";
    }
}