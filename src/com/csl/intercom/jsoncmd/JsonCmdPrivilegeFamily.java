package com.csl.intercom.jsoncmd;

public enum JsonCmdPrivilegeFamily {
    MANAGE_HTTP_TEMPLATES("manage_http_template"),
    START_CPE_SCAN("start_cpe_scan"),
    MANAGE_SCAN_DB("manage_scan_db"),
    ;

    private final String privilegeFamily;

    private JsonCmdPrivilegeFamily(String privilegeFamily) {
        this.privilegeFamily = privilegeFamily;
    }

    public String toString() {
        return privilegeFamily;
    }
}
