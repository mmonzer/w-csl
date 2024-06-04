package com.csl.intercom.dbapi.enums;

import lombok.Getter;

@Getter
public enum FinishedScanStatus {
    FINISHED_SUCCESS(0),
    FINISHED_ERROR(1),
    DISCARDED(2),
    ;

    private final int dbapiCode;

    private FinishedScanStatus(int dbapiCode)
    {
        this.dbapiCode = dbapiCode;
    }

}
