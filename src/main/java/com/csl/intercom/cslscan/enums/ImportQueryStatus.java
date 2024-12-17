package com.csl.intercom.cslscan.enums;

import java.util.List;

public enum ImportQueryStatus {
    PENDING("PENDING"),
    RUNNING("RUNNING"),
    ERROR("ERROR"),
    SUCCESS("SUCCESS"),
    ;

    public static final List<ImportQueryStatus> IN_PROGRESS_STATUSES = List.of(PENDING, RUNNING);
    public static final List<ImportQueryStatus> FINISHED_STATUSES = List.of(SUCCESS, ERROR);

    private final String name;

    ImportQueryStatus(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
