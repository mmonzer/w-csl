package com.csl.intercom.dbapi.enums;

import lombok.Getter;

/**
 * The various endpoints used in DB-API.
 */

@Getter
public enum DbapiEndpointForCSLScan {

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
    EXTERNAL_CONNECTION_INFO_DETAILS(EXTERNAL_CONNECTION_INFO.getEndpoint() + "/%s"),
    EXTERNAL_CONNECTION_INFO_CREATE_OR_UPDATE(EXTERNAL_CONNECTION_INFO.getEndpoint() + "/create_or_update_external_connections_info"),
    EXTERNAL_CONNECTION_INFO_CLEAR(EXTERNAL_CONNECTION_INFO.getEndpoint() + "/clear"),
    EXTERNAL_DISCOVERED_DEVICES_LAST_UPDATED_DATE(EVENTS.getEndpoint() + "/get_last_discovered_device_updated_date"),
    EXTERNAL_DISCOVERED_DEVICES_CREATE_EVENT(EVENTS.getEndpoint() + "/create_discovery_devices_from_third_party_event"),
    EXTERNAL_DISCOVERED_DEVICES_UPDATE_EVENT(EVENTS.getEndpoint() + "/update_discovery_scan_event/%d"),
    EXTERNAL_DISCOVERED_DEVICES("/device_discovered_items"),
    EXTERNAL_DISCOVERED_DEVICES_CREATE(EXTERNAL_DISCOVERED_DEVICES.getEndpoint() + "/create_discovery_device_entities"),
    EXTERNAL_DISCOVERED_DEVICES_CLEAR(EXTERNAL_DISCOVERED_DEVICES.getEndpoint() + "/clear"),
    JAVACOMM("/javacomm"),
    JAVACOMM_SEND_COMMANDS(JAVACOMM.getEndpoint() + "/create_or_update_japi_commands"),
    DOWNLOAD_HTTP_TEMPLATES_BSON_FILE("/cpecve/download_http_template_bson_file/"),
    UPLOAD_HTTP_TEMPLATES_BSON_FILE("/cpecve/upload_http_template_bson_file/"),
    FILE_ACTION_STATUS("/file_action_status"),
    FILE_ACTION_STATUS_DETAILS(FILE_ACTION_STATUS.getEndpoint() + "/%d"),
//    FILE_ACTION_STATUS_AVAILABLE(FILE_ACTION_STATUS.getEndpoint() + "/available"),
    FILE_ACTION_STATUS_AVAILABLE(FILE_ACTION_STATUS.getEndpoint()),
    FILE_ACTION_STATUS_CREATE_FOR_HTTP_TEMPLATE_EXPORT("/cpecve/create_file_action_status_for_exported_http_templates/"),
    ;

    @Getter
    private final String endpoint;

    DbapiEndpointForCSLScan(String endpoint) {
        this.endpoint = endpoint;
    }

}
