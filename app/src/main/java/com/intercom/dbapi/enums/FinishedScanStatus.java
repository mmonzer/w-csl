package com.csl.intercom.dbapi.enums;

public enum FinishedScanStatus {
    FINISHED_SUCCESS(0),
    FINISHED_ERROR(1),
    DISCARDED(2),
    ;

    private int dbapiCode;

    private FinishedScanStatus(int dbapiCode)
    {
        this.dbapiCode = dbapiCode;
    }

    public int getDbapiCode() {
        return dbapiCode;
    }
}
