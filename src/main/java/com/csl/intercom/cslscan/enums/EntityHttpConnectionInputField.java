package com.csl.intercom.cslscan.enums;

public enum EntityHttpConnectionInputField {
    KEY("key", "key"),
    DEFAULT_VALUE("default_value", "defaultValue"),
    SECRET("isSecret", "secret"),
    REQUIRED("isRequired", "required"),
    DESCRIPTION("description", "description"),
    DISPLAY("display", "display"),
    TOOLTIP("tooltip", "tooltip"),
    PLACEHOLDER("placeholder", "placeholder");

    private String dbapiName;
    private String scanName;

    private EntityHttpConnectionInputField(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }

    public static EntityHttpConnectionInputField fromDbapiName(String dbapiName) {
        for (EntityHttpConnectionInputField field : EntityHttpConnectionInputField.values()) {
            if (field.dbapiName.equals(dbapiName)) {
                return field;
            }
        }
        return null;
    }

    public static EntityHttpConnectionInputField fromScanName(String scanName) {
        for (EntityHttpConnectionInputField field : EntityHttpConnectionInputField.values()) {
            if (field.scanName.equals(scanName)) {
                return field;
            }
        }
        return null;
    }
}
