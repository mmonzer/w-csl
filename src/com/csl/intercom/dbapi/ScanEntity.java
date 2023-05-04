package com.csl.intercom.dbapi;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScanEntity {
    public enum Status {
        STARTED,
        FINISHED_SUCCESS,
        FINISHED_FAIL;

        public static final List<Status> finishedStates = new ArrayList<>() {{
            add(FINISHED_SUCCESS);
            add(FINISHED_FAIL);
        }};
    }
    private int dbapiId;
    private String scanId;
    private Status status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String description = null;  // Description of failure

    public ScanEntity(int dbapiId, String scanId, Status status, LocalDateTime startDate, LocalDateTime endDate) {
        this.dbapiId = dbapiId;
        this.scanId = scanId;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    public ScanEntity(int dbapiId, String scanId) {
        this(dbapiId, scanId, Status.STARTED, LocalDateTime.now(), null);
    }

    public static ScanEntity fromDbapiId(int dbapiId) {
        return fromDbapiId(dbapiId, LocalDateTime.now());
    }

    public static ScanEntity fromDbapiId(int dbapiId, LocalDateTime startDate) {
        return new ScanEntity(dbapiId, null, Status.STARTED, startDate, null);
    }

    public int getDbapiId() {
        return dbapiId;
    }

    public String getScanId() {
        return scanId;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        if (Status.finishedStates.contains(this.status)) {
            return endDate;
        } else {
            return null;
        }
    }

    public String getDescription() {
        if (this.status == Status.FINISHED_FAIL) {
            return description;
        } else {
            return null;
        }
    }

    public void setDbapiId(int dbapiId) {
        this.dbapiId = dbapiId;
    }

    public void setScanId(String scanId) {
        this.scanId = scanId;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setFinished(Status status, LocalDateTime endDate) {
        this.status = status;
        this.endDate = endDate;
    }

    public void setFinished(boolean success, LocalDateTime endDate) {
        setFinished(success ? Status.FINISHED_SUCCESS : Status.FINISHED_FAIL, endDate);
    }

    public void setFinished(boolean success) {
        setFinished(success, LocalDateTime.now());
    }

    public void setFinished(LocalDateTime endDate) {
        setFinished(true, endDate);
    }

    public void setFinished() {
        setFinished(LocalDateTime.now());
    }

    public void setFinishedFail(String description, LocalDateTime endDate) {
        setFinished(Status.FINISHED_FAIL, endDate);
        this.description = description;
    }

    public boolean isRunning() {
        return status == Status.STARTED;
    }

    public boolean isSuccess() {
        return this.status == Status.FINISHED_SUCCESS;
    }
}
