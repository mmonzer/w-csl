package com.csl.intercom.dbapi.models;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a scan's status.
 */
public class ScanEntity {
    /**
     * The possible statuses a scan may be in.
     */
    public enum Status {
        STARTED,
        PENDING,
        RUNNING,
        FINISHED_SUCCESS,
        FINISHED_FAIL,
        DISCARDED,
        ;

        public static final List<Status> finishedStates = new ArrayList<>() {{
            add(FINISHED_SUCCESS);
            add(FINISHED_FAIL);
            add(DISCARDED);
        }};
    }

    private int dbapiId;                // The scan's id in DB-API
    private String scanId;              // The scan's id in CSL-Scan
    private Status status;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private String description = null;  // Description of failure
    private double progress = 0.0;

    protected ScanEntity(int dbapiId, String scanId, Status status, OffsetDateTime startDate, OffsetDateTime endDate) {
        this.dbapiId = dbapiId;
        this.scanId = scanId;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public ScanEntity(int dbapiId, String scanId) {
        this(dbapiId, scanId, Status.STARTED, OffsetDateTime.now(), null);
    }

    /**
     * Create a new {@link ScanEntity} from the starting date and the id given by DB-API.
     *
     * @param dbapiId   The id created by DB-API.
     * @param startDate The start time of the scan.
     * @return The newly created {@link ScanEntity}.
     */
    public static ScanEntity fromDbapiId(int dbapiId, OffsetDateTime startDate) {
        return new ScanEntity(dbapiId, null, Status.STARTED, startDate, null);
    }

    public static ScanEntity fromScanId(String scanId, OffsetDateTime startDate) {
        return new ScanEntity(0, scanId, Status.STARTED, startDate, null);
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

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public OffsetDateTime getEndDate() {
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

    public double getProgress() {
        return progress;
    }

    public void setDbapiId(int dbapiId) {
        this.dbapiId = dbapiId;
    }

    public void setScanId(String scanId) {
        this.scanId = scanId;
    }

    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Change the status to finished (success or fail), and set the date of the scan's end.
     *
     * @param status  The new status (FINISHED_SUCCESS or FINISHED_FAIL).
     * @param endDate The date of the scan's end.
     */
    public void setFinished(Status status, OffsetDateTime endDate) {
        this.status = status;
        this.endDate = endDate;
    }

    /**
     * Change the status to finished successfully, and set the date of the scan's end.
     *
     * @param endDate The date of the scan's end.
     */
    public void setFinishedSuccess(OffsetDateTime endDate) {
        setFinished(Status.FINISHED_SUCCESS, endDate);
    }

    /**
     * Change the status to finished successfully, and set the date of the scan's end to now.
     */
    public void setFinishedSuccess() {
        setFinishedSuccess(OffsetDateTime.now());
    }

    public void setFinishedFail(String description, OffsetDateTime endDate) {
        setFinished(Status.FINISHED_FAIL, endDate);
        this.description = description;
    }

    public void setDiscarded(OffsetDateTime endDate) {
        setFinished(Status.DISCARDED, endDate);
    }

    public void setDiscarded() {
        setDiscarded(OffsetDateTime.now());
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    /**
     * Check if a scan is in progress.
     *
     * @return true if the scan is not finished, false otherwise.
     */
    public boolean isRunning() {
        return status == Status.STARTED;
    }

    /**
     * Check if a scan finished successfully.
     *
     * @return true if the scan is finished successfully, false otherwise (still running or finished with a failure).
     */
    public boolean isSuccess() {
        return this.status == Status.FINISHED_SUCCESS;
    }

    public boolean isFailure() {
        return this.status == Status.FINISHED_FAIL;
    }
    public boolean isDiscarded() {
        return this.status == Status.DISCARDED;
    }

    public boolean isFinished() {
        return this.isSuccess() || this.isFailure() || this.isDiscarded();
    }
}
