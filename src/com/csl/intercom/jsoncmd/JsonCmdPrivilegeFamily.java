package com.csl.intercom.jsoncmd;

public enum JsonCmdPrivilegeFamily {
    MANAGE_HTTP_TEMPLATES("manage_http_template"),
    START_CPE_SCAN("start_cpe_scan"),
    MANAGE_SCAN_DB("manage_scan_db"),
    START_DEVICE_SCAN("start_device_scan"),
    CREATE_EXTERNAL_CONNECTION_INFO("add_externalconnectioninfo"),
    DELETE_EXTERNAL_CONNECTION_INFO("delete_externalconnectioninfo"),
    DELETE_EXTERNAL_DISCOVERED_DEVICE("delete_devicediscoveredcpevulnerability"),
    ;

    private final String privilegeFamily;

    private JsonCmdPrivilegeFamily(String privilegeFamily) {
        this.privilegeFamily = privilegeFamily;
    }

    public String toString() {
        return privilegeFamily;
    }
}
