package com.csl.intercom.cslscan.models.scans;

import com.csl.intercom.cslscan.ScanUtils;
import com.ucsl.json.Json;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Represents a device discovery scan.
 * Holds the state of the scan.
 */
public class ExternalScan {
    private final String uuid;
    private int dbapiId = 0;
    private final OffsetDateTime createdAt;
    private Status status;

    public ExternalScan(String uuid, OffsetDateTime createdAt, Status status) {
        this.uuid = uuid;
        this.createdAt = createdAt;
        this.status = status;
    }

    public static ExternalScan fromScannerJson(Json json) {
        String uuid;
        if (json.has("uuid") && json.get("uuid").isString()) {
            uuid = json.get("uuid").asString();
        } else {
            return null;
        }

        Status status;
        if (json.has("status") && json.get("status").isString()) {
            status = Status.valueOf(json.get("status").asString());
        } else {
            return null;
        }

        OffsetDateTime createdAt;
        if (json.has("createdAt") && json.get("createdAt").isString()) {
            createdAt = ScanUtils.scanTimeToLocal(OffsetDateTime.parse(json.get("createdAt").asString()));
        } else {
            return null;
        }

        return new ExternalScan(uuid, createdAt, status);
    }

    public String getUuid() {
        return uuid;
    }

    public Status getStatus() {
        return status;
    }

    public ExternalScan setStatus(Status status) {
        this.status = status;
        return this;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public int getDbapiId() {
        return dbapiId;
    }

    public ExternalScan setDbapiId(int dbapiId) {
        this.dbapiId = dbapiId;
        return this;
    }

    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        ;
        public static final Set<Status> finishedStatuses = Set.of(COMPLETED, FAILED);
        public static final Set<Status> unfinishedStatuses = Set.of(PENDING, IN_PROGRESS);
    }
}
