package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.ScanConstants;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.dbapi.models.ScanEntity;
import com.csl.intercom.services.annotations.PostInit;
import com.csl.intercom.services.exceptions.CpeScanException;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.csl.util.SchedulerUtil;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.JsonApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Represents and handles the list of currently running scans.
 * Should not be created, but accessed through the static instance.
 */
public class CpeScanService {
    static public CpeScanService instance = new CpeScanService();
    static private final Logger logger = LoggerFactory.getLogger(CpeScanService.class);
    // The list of scans, indexed by their id (this list contains all the running scans).
    private final Map<String, ScanEntity> scanEntities = new ConcurrentHashMap<>();
    // The list of scans that have been modified since the last time they were handled --> need to be handled.
    private final Queue<String> modifiedScans = new ConcurrentLinkedQueue<>();
    private final ScanApiHandler scanApiHandler = new ScanApiHandler();
    private final DbapiHandler dbapiHandler = new DbapiHandler();
    private DataSynchronizationService cpeItemsSynchronizationService;
    private DataSynchronizationService microsoftKbSynchronizationService;
    private ScheduledExecutorService scansHandlingTask = null;
    private final ScheduledExecutorService scansListSanitizer = Executors.newSingleThreadScheduledExecutor();

    public void init(DataSynchronizationService cpeItemsSynchronizationService, DataSynchronizationService microsoftKbSynchronizationService) {
        this.cpeItemsSynchronizationService = cpeItemsSynchronizationService;
        this.microsoftKbSynchronizationService = microsoftKbSynchronizationService;

        scansListSanitizer.scheduleAtFixedRate(this::sanitizeScans, 0, 5, TimeUnit.MINUTES);

        // Execute post-init tasks
        Class<?> clazz = this.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostInit.class)) {
                try {
                    method.invoke(this);
                } catch (Exception e) {
                    logger.error("Could not execute post-init method {}", method.getName(), e);
                }
            }
        }
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

    @PostInit
    public void cancelScan() throws CpeScanException {
        try {
            this.scanApiHandler.cancelScan();
            this.scanEntities.forEach((id, scan) -> {
                if (scan.isRunning()) {
                    scan.setStatus(ScanEntity.Status.DISCARDED);
                }
            });
            this.dbapiHandler.cancelAllScans();
        } catch (Exception e) {
            throw new CpeScanException("Could not cancel the scan", e);
        }
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
                    logger.error("Could not notify DB-API a scan finished", e);
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

        try {
            this.cpeItemsSynchronizationService.syncData();
        } catch (SynchronizationException e) {
            logger.error("Could not synchronize CPE Items with DB-API");
            logger.debug("Could not synchronize CPE Items with DB-API", e);
        }

        try {
            this.microsoftKbSynchronizationService.syncData();
        } catch (SynchronizationException e) {
            logger.error("Could not synchronize Microsoft KBs with DB-API");
            logger.debug("Could not synchronize Microsoft KBs with DB-API", e);
        }

        if (scan.isFinished()) {
            try {
                this.dbapiHandler.notifyScanFinished(scan);
                this.remove(scan);

                // If there is no more scan currently active, stop the handling task after the end of the current execution
                if (this.scanEntities.isEmpty()) {
                    this.scansHandlingTask.shutdown();
                }
            } catch (Exception e) {
                logger.error("Could not notify DB-API the scan {} ended.", scan.getScanId(), e);
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
        String defaultStatus = "ERROR";
        return status != null ? JsonUtil.getStringFromJson(status, "status", defaultStatus) : defaultStatus;
    }

    /**
     * Extract the message off the JSON status as received from CSL-Scan.
     *
     * @param status The Json status as received from CSL-Scan.
     * @return The message of the scan, or an empty String if not present in the JSON.
     */
    private String getScanDescriptionFromStatus(Json status) {
        if (status == null) {
            return "";
        }
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
                SchedulerUtil.scheduleAtFixedRatedWithTimeout(scansHandlingTask, this::handleScans, 0, 1, TimeUnit.SECONDS, 5, TimeUnit.MINUTES);
            }
        } else {
            scansHandlingTask = Executors.newSingleThreadScheduledExecutor();
            SchedulerUtil.scheduleAtFixedRatedWithTimeout(scansHandlingTask, this::handleScans, 0, 1, TimeUnit.SECONDS, 5, TimeUnit.MINUTES);
        }
    }
}
