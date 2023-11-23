package com.csl.intercom.cslscan.enums;

public enum EntityHttpConnectionField {
    UUID("uuid", "uuid"),
    VARIABLES("variables", "variables"),
    NAME("name", "name"),
    INPUTS("inputs", "inputs"),
    STAGES("stages", "stages");

    private String dbapiName;
    private String scanName;

    EntityHttpConnectionField(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }

    public static EntityHttpConnectionField fromDbapiName(String dbapiName) {
        for (EntityHttpConnectionField field: EntityHttpConnectionField.values()) {
            if (field.dbapiName.equals(dbapiName)) {
                return field;
            }
        }
        return null;
    }

    public static EntityHttpConnectionField fromScanName(String scanName) {
        for (EntityHttpConnectionField field: EntityHttpConnectionField.values()) {
            if (field.scanName.equals(scanName)) {
                return field;
            }
        }
        return null;
    }
}
