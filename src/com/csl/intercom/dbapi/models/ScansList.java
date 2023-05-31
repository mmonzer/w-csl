package com.csl.intercom.dbapi.models;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ScansList {
    static public ScansList instance = new ScansList();
    Map<String, ScanEntity> scanEntities = new HashMap<>();

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
}
