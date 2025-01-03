package com.csl.intercom.cslscan.enums;

import java.util.List;

public enum ExportQueryStatus {
    PENDING("PENDING"),
    RUNNING("RUNNING"),
    ERROR("ERROR"),
    SUCCESS("SUCCESS"),
    ;

    public static final List<ExportQueryStatus> IN_PROGRESS_STATUSES = List.of(PENDING, RUNNING);
    public static final List<ExportQueryStatus> FINISHED_STATUSES = List.of(SUCCESS, ERROR);

    private final String name;

    ExportQueryStatus(String name) {
        this.name = name;
    }

    @Override
    public String toString(){
        return name;
    }
}
