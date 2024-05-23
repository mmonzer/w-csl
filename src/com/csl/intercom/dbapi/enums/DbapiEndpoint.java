package com.csl.intercom.dbapi.enums;

/**
 * The various endpoints used in DB-API.
 */
public enum DbapiEndpoint {
    EVENTS("/events"),
    EVENTS_CANCEL_ALL(EVENTS.getEndpoint() + "/set_all_active_cpe_scan_event_to_discarded"),
    CPE_ITEMS_LAST_DATE(EVENTS.getEndpoint() + "/last_discovery_date"),
    CPE_ITEMS("/cpe_discovered_items"),
    DELETED_OBJECTS("/deleted_objects"),
    CREATE_CPE_ITEMS(CPE_ITEMS.getEndpoint() + "/create_discovery_cpe_entities"),
    GET_DELETED_CPE_ITEMS(DELETED_OBJECTS.getEndpoint() + "/get_deleted_cpe_items"),
    NO_NEW_CPE_ITEM(CPE_ITEMS.getEndpoint() + "/notify_sync_ended_with_no_discovered_cpe"),
    DELETE_CPE_ITEMS(CPE_ITEMS.getEndpoint() + "/delete_by_mongo_ids"),
    MICROSOFT_KB("/microsoft_kb"),
    MICROSOFT_KB_LAST_DATE(EVENTS.getEndpoint() + "/last_microsoft_kb_date"),
    CREATE_MICROSOFT_KBS(MICROSOFT_KB.getEndpoint() + "/create_microsoft_kbs"),
    DELETE_MICROSOFT_KBS(MICROSOFT_KB.getEndpoint() + "/delete_by_mongo_ids"),
    GET_DELETED_MICROSOFT_KBS(DELETED_OBJECTS.getEndpoint() + "/get_deleted_microsoft_kbs"),
    DEVICES("/devices"),
    DELETED_DEVICES(DELETED_OBJECTS.getEndpoint() + "/get_deleted_devices"),
    CONNECTIONS("/connections"),
    DISCOVERY_PROTOCOLS("/cpe_discovery_api"),
    DISCOVERY_PROTOCOLS_DETAILS(DISCOVERY_PROTOCOLS.getEndpoint() + "/%d"),
    DISCOVERY_PROTOCOLS_DETAILS_BY_TEMPLATE_ID(DISCOVERY_PROTOCOLS.getEndpoint() + "/get_dynamic_discovery_protocol"),
    SCAN_EVENT_CREATION(EVENTS.getEndpoint() + "/create_discovery_scan_event"),
    SCAN_EVENT_UPDATE(EVENTS.getEndpoint() + "/%d/update_discovery_scan_event"),
    ORGANIZATIONS("/organization_api_key"),
    GET_ORGANIZATION_NAME(ORGANIZATIONS.getEndpoint() + "/get_organization_name_from_api_key"),
    GET_MQTT_TOPIC_PREFIX(ORGANIZATIONS.getEndpoint() + "/get_mqtt_topic_prefix"),
    EXTERNAL_CONNECTION_INFO_TEMPLATES("/external_connection_info_templates"),
    EXTERNAL_CONNECTION_INFO_TEMPLATES_CREATE_OR_UPDATE(EXTERNAL_CONNECTION_INFO_TEMPLATES.getEndpoint() + "/create_or_update_external_connection_info_templates"),
    EXTERNAL_CONNECTION_INFO("/external_connections_info"),
    EXTERNAL_CONNECTION_INFO_CREATE_OR_UPDATE(EXTERNAL_CONNECTION_INFO.getEndpoint() + "/create_or_update_external_connections_info"),
    ;

    private final String endpoint;

    DbapiEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
