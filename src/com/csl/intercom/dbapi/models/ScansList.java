package com.csl.intercom.dbapi.models;

import com.csl.core.CSLContext;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.ScanConstants;
import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.dbapi.DbapiHandler;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.JsonApiResponse;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ScansList {
    static public ScansList instance = new ScansList();
    Map<String, ScanEntity> scanEntities = new HashMap<>();
    private ScanApiHandler scanApiHandler = new ScanApiHandler(ScanUtils.generateScanApiUrlFromConfig(CSLContext.instance.getConfig().get("discovery")));
    private DbapiHandler dbapiHandler = new DbapiHandler();

    public synchronized void add(ScanEntity scan) {
        scanEntities.put(scan.getScanId(), scan);
    }

    public void remove(String scanId) {
        scanEntities.remove(scanId);
    }

    public void remove(ScanEntity scan) {
        this.remove(scan.getScanId());
    }

    public ScanEntity getScanByScanId(String scanId) {
        return scanEntities.getOrDefault(scanId, null);
    }

    private ScanEntity searchScan(Function<ScanEntity, Boolean> predicate) {
        for (ScanEntity scan: scanEntities.values()) {
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
        for (ScanEntity scan: scanEntities.values()) {
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
