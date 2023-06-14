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

public class ScansList {
    static public ScansList instance = new ScansList();
    Map<String, ScanEntity> scanEntities = new ConcurrentHashMap<>();
    private Queue<String> modifiedScans = new ConcurrentLinkedQueue<>();
    private ScanApiHandler scanApiHandler = new ScanApiHandler();
    private DbapiHandler dbapiHandler = new DbapiHandler();
    private ScheduledExecutorService scansHandlingTask = null;
    private ScheduledExecutorService scansListSanitizer = Executors.newSingleThreadScheduledExecutor();

    {
//        scansHandlingTask.scheduleAtFixedRate(this::handleScans, 0, 1, TimeUnit.SECONDS);
        scansListSanitizer.scheduleAtFixedRate(this::sanitizeScans, 0, 5, TimeUnit.MINUTES);
    }

    public void createOrUpdate(ScanEntity scan) {
        this.add(scan);
    }

    public synchronized void add(ScanEntity scan) {
        String id = scan.getScanId();
        if (!this.scanEntities.containsKey(id)) {
            this.sanitizeScans();
        }
        this.scanEntities.put(id, scan);
        if (!this.modifiedScans.contains(id)) {
            this.modifiedScans.add(id);
        }
        if (scansHandlingTask == null) {
            scansHandlingTask = Executors.newSingleThreadScheduledExecutor();
        }

        if (scansHandlingTask.isShutdown() || scansHandlingTask.isTerminated()) {
            // Leave time (1 minute) for the possibly running tasks to terminate, and after that timeout kill the tasks.
            try {
                if (!scansHandlingTask.awaitTermination(1, TimeUnit.MINUTES)) {
                    scansHandlingTask.shutdownNow();
                }
            } catch (InterruptedException e) {
                scansHandlingTask.shutdownNow();
//                Thread.currentThread().interrupt();
            }
            scansHandlingTask = Executors.newSingleThreadScheduledExecutor();
        }
    }

    private void remove(String scanId) {
        this.scanEntities.remove(scanId);
    }

    private void remove(ScanEntity scan) {
        this.remove(scan.getScanId());
    }

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

    public ScanEntity getFinishedScan() {
        return searchScan(ScanEntity::isFinished);
    }

    public void sanitizeScans() {
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

    private void handleScan(String id) {
        ScanEntity scan = scanEntities.get(id);
        if (scan != null) {
            handleScan(scan);
        }
    }

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
            } catch (Exception e) {
                System.err.println("Could not notify DB-API the scan " + scan.getScanId() + " ended.");
            }
        }
    }

    private void handleScans() {
        while (!modifiedScans.isEmpty()) {
            handleScan(this.modifiedScans.poll());
        }
    }

    private Json getScanStatus(String id) {
        JsonApiResponse response = scanApiHandler.getScanStatus(id);
        return response.isSuccess() ? response.getResult() : null;
    }

    private String getStatusStringFromStatus(Json status) {
        return JsonUtil.getStringFromJson(status, "status", "ERROR");
    }

    private String getScanDescriptionFromStatus(Json status) {
        return JsonUtil.getStringFromJson(status, "message", "");
    }
}
