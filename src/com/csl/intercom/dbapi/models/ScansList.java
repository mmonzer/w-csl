package com.csl.intercom.dbapi.models;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.ScanConstants;
import com.csl.intercom.dbapi.DbapiHandler;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.JsonApiResponse;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Represents and handles the list of currently running scans.
 * Should not be created, but accessed through the static instance.
 */
public class ScansList {
    static public ScansList instance = new ScansList();
    // The list of scans, indexed by their id (this list contains all the running scans).
    private Map<String, ScanEntity> scanEntities = new ConcurrentHashMap<>();
    // The list of scans that have been modified since the last time they were handled --> need to be handled.
    private Queue<String> modifiedScans = new ConcurrentLinkedQueue<>();
    private ScanApiHandler scanApiHandler = new ScanApiHandler();
    private DbapiHandler dbapiHandler = new DbapiHandler();
    private ScheduledExecutorService scansHandlingTask = null;
    private ScheduledExecutorService scansListSanitizer = Executors.newSingleThreadScheduledExecutor();

    {
//        scansHandlingTask.scheduleAtFixedRate(this::handleScans, 0, 1, TimeUnit.SECONDS);
        scansListSanitizer.scheduleAtFixedRate(this::sanitizeScans, 0, 5, TimeUnit.MINUTES);
    }

    /**
     * Add a scan in the list, or overwrite it if it already exists.
     *
     * @param scan The scan to add to the list.
     */
    public void createOrUpdate(ScanEntity scan) {
        String id = scan.getScanId();
        if (!this.scanEntities.containsKey(id)) {
            this.sanitizeScans();
        }
        this.scanEntities.put(id, scan);
        if (!this.modifiedScans.contains(id)) {
            this.modifiedScans.add(id);
        }

        scheduleScanHandlingIfNecessary();
    }

    private void remove(String scanId) {
        this.scanEntities.remove(scanId);
    }

    private void remove(ScanEntity scan) {
        this.remove(scan.getScanId());
    }

    /**
     * Get a scan from its CSL-Scan id, or null.
     *
     * @param scanId The id to seek.
     * @return The scan entity if found, null otherwise.
     */
    public ScanEntity getScanByScanId(String scanId) {
        return scanEntities.getOrDefault(scanId, null);
    }

    private ScanEntity searchScan(Function<ScanEntity, Boolean> predicate) {
        for (ScanEntity scan : scanEntities.values()) {
            if (predicate.apply(scan)) {
                return scan;
            }
        }
        return null;
    }

    /**
     * Get the (first) running scan.
     *
     * @return The first (though should be the only) scan if there is one, null if no scan is running.
     */
    public ScanEntity getRunningScan() {
        return searchScan(ScanEntity::isRunning);
    }

    /**
     * Get the first finished scan.
     *
     * @return The first scan that is finished if any, null otherwise.
     */
    public ScanEntity getFinishedScan() {
        return searchScan(ScanEntity::isFinished);
    }

    /**
     * Check the scans in scanEntities, to get their status from CSL-Scan directly, and handle them accordingly.
     * If a scan is unknown in CSL-Scan, we consider it is finished with errors.
     */
    private void sanitizeScans() {
        for (ScanEntity scan : scanEntities.values()) {
            Json status = getScanStatus(scan.getScanId());
            String statusString = getStatusStringFromStatus(status);
            if (ScanConstants.finishedScanStatuses.contains(statusString)) {
                //region Set correct status for the scan
                if (ScanConstants.successScanStatuses.contains(statusString)) {
                    scan.setFinishedSuccess();
                } else if ("DISCARDED".equals(statusString)) {
                    scan.setDiscarded();
                } else {
                    String description = getScanDescriptionFromStatus(status);
                    scan.setFinishedFail(description, OffsetDateTime.now());
                }
                //endregion Set correct status for the scan
                try {
                    dbapiHandler.notifyScanFinished(scan);
                    scanEntities.remove(scan.getScanId());
                } catch (Exception e) {
                    System.err.println("Could not notify DB-API a scan finished");
                }
            }
        }
    }

    /**
     * Check if the scan is already in the list of scans, and id so calls the other handling function.
     *
     * @param id The id to seek in scanEntities.
     */
    private void handleScan(String id) {
        ScanEntity scan = scanEntities.get(id);
        if (scan != null) {
            handleScan(scan);
        }
    }

    /**
     * Take the appropriate actions for a scan :
     * - If it has no DB-API id, request one from DB-API (by notifying the scan started)
     * - Send a CPE Items batch to DB-API
     * - If the scan is finished, notify DB-API and remove it from scanEntities
     * Moreover, stops the handling task if no scan is registered.
     * @param scan
     */
    private void handleScan(ScanEntity scan) {
        // If the scan has no DB-API id, create one for it and assign it to the scan
        if (scan.getDbapiId() == 0) {
            scan.setDbapiId(dbapiHandler.notifyScanStarted(scan.getStartDate()));
        }

        this.scanApiHandler.sendNewCpeItemsToDbapi(this.dbapiHandler);

        if (scan.isFinished()) {
            try {
                this.dbapiHandler.notifyScanFinished(scan);
                this.remove(scan);

                // If there is no more scan currently active, stop the handling task after the end of the current execution
                if (this.scanEntities.isEmpty()) {
                    this.scansHandlingTask.shutdown();
                }
            } catch (Exception e) {
                System.err.println("Could not notify DB-API the scan " + scan.getScanId() + " ended.");
            }
        }
    }

    /**
     * Loop over modifiedScans to handle the scans in there.
     */
    private void handleScans() {
        while (!modifiedScans.isEmpty()) {
            handleScan(this.modifiedScans.poll());
        }
    }

    /**
     * Get the current status of a scan directly from CSL-Scan with its id.
     *
     * @param id The id of the scan to check.
     * @return The status in JSON format, as returned by CSL-Scan, or null if there was an error.
     */
    private Json getScanStatus(String id) {
        JsonApiResponse response = scanApiHandler.getScanStatus(id);
        return response.isSuccess() ? response.getResult() : null;
    }

    /**
     * Extract the status string off the JSON status as received from CSL-Scan.
     *
     * @param status The Json status as received from CSL-Scan.
     * @return The status string of the scan, or ERROR if not present in the JSON.
     */
    private String getStatusStringFromStatus(Json status) {
        return JsonUtil.getStringFromJson(status, "status", "ERROR");
    }

    /**
     * Extract the message off the JSON status as received from CSL-Scan.
     *
     * @param status The Json status as received from CSL-Scan.
     * @return The message of the scan, or an empty String if not present in the JSON.
     */
    private String getScanDescriptionFromStatus(Json status) {
        Json message = status.get("message");
        if (message == null || !message.isString()) {
            return "";
        } else {
            return message.asString();
        }
    }

    /**
     * Check if the scans handling task is currently scheduled, and if not start it.
     * If it was shutdown, leave a grace period to finish (10 seconds), and forcefully stop it before re-scheduling it.
     */
    private void scheduleScanHandlingIfNecessary() {
        if (this.scansHandlingTask != null) {
            if (scansHandlingTask.isShutdown() || scansHandlingTask.isTerminated()) {
                // Leave time (10 seconds) for the possibly running tasks to terminate, and after that timeout kill the tasks.
                try {
                    if (!scansHandlingTask.awaitTermination(10, TimeUnit.SECONDS)) {
                        scansHandlingTask.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    // The exception is thrown if the current thread is terminated, so shut down immediately and resume the interruption
                    scansHandlingTask.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                scansHandlingTask = Executors.newSingleThreadScheduledExecutor();
                scansHandlingTask.scheduleAtFixedRate(this::handleScans, 0, 1, TimeUnit.SECONDS);
            }
        } else {
            scansHandlingTask = Executors.newSingleThreadScheduledExecutor();
            scansHandlingTask.scheduleAtFixedRate(this::handleScans, 0, 1, TimeUnit.SECONDS);
        }
    }
}
