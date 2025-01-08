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
    public static final String STATUS = "status";
    public static final String CREATED_AT = "createdAt";
    public static final String UUID = "uuid";
    private final String scanUuid;
    private int dbapiId = 0;
    private final OffsetDateTime createdAt;
    private Status scanStatus;

    public ExternalScan(String uuid, OffsetDateTime createdAt, Status status) {
        this.scanUuid = uuid;
        this.createdAt = createdAt;
        this.scanStatus = status;
    }

    public static ExternalScan fromScannerJson(Json json) {
        String uuid;
        if (json.has(UUID) && json.get(UUID).isString()) {
            uuid = json.get(UUID).asString();
        } else {
            return null;
        }

        Status status;
        if (json.has(STATUS) && json.get(STATUS).isString()) {
            status = Status.valueOf(json.get(STATUS).asString());
        } else {
            return null;
        }

        OffsetDateTime createdAt;
        if (json.has(CREATED_AT) && json.get(CREATED_AT).isString()) {
            createdAt = ScanUtils.scanTimeToLocal(OffsetDateTime.parse(json.get(CREATED_AT).asString()));
        } else {
            return null;
        }

        return new ExternalScan(uuid, createdAt, status);
    }

    public String getScanUuid() {
        return scanUuid;
    }

    public Status getScanStatus() {
        return scanStatus;
    }

    public ExternalScan setScanStatus(Status scanStatus) {
        this.scanStatus = scanStatus;
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
