package com.csl.intercom.cslscan.enums;

public enum EntityHttpConnectionTestResultDetailsField {
    FAILED_STAGE_UUID("failedStageUuid", "failedStageUuid"),
    FAILED_STAGE_NAME("failedStageName", "failedStageName"),
    FAILED_STAGE_INDEX("failedStageIndex", "failedStageIndex"),
    FAILED_PART("failedPart", "failedPart"),
    ERROR_MESSAGE("errorMessage", "errorMessage"),
    STATUS_CODE("statusCode", "statusCode"),
    VARIABLES("variables", "variablesSet"),
    BODY("body", "body"),
    CPE_ITEMS("cpeItems", "cpeItems"),
    URL("url", "url"),
    ;

    private String dbapiName;
    private String scanName;

    private EntityHttpConnectionTestResultDetailsField(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }

    public static EntityHttpConnectionTestResultDetailsField fromDbapiName(String dbapiName) {
        for (EntityHttpConnectionTestResultDetailsField field : EntityHttpConnectionTestResultDetailsField.values()) {
            if (field.dbapiName().equals(dbapiName)) {
                return field;
            }
        }
        return null;
    }

    public static EntityHttpConnectionTestResultDetailsField fromScanName(String scanName) {
        for (EntityHttpConnectionTestResultDetailsField field : EntityHttpConnectionTestResultDetailsField.values()) {
            if (field.scanName().equals(scanName)) {
                return field;
            }
        }
        return null;
    }
}
