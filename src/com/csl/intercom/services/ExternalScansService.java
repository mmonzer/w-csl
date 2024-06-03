package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.ExternalConnectionInfoTemplate;
import com.csl.intercom.cslscan.models.scans.ExternalScan;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.dbapi.exceptions.DbapiUnexpectedStatusCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ExternalScansService {
    private Logger logger = LoggerFactory.getLogger(ExternalScansService.class);
    private DbapiHandler dbapiHandler;
    private ScanApiHandler scanApiHandler;

    private Map<String, ExternalScan> externalScans = new ConcurrentHashMap<>();

    public ExternalScansService(DbapiHandler dbapiHandler, ScanApiHandler scanApiHandler) {
        this.dbapiHandler = dbapiHandler;
        this.scanApiHandler = scanApiHandler;
    }

    public ExternalScan getDeviceDiscoveryScan(String uuid) {
        return externalScans.get(uuid);
    }

    public ExternalScan startExternalDiscoveryScan(String connectionInfoUuid) {
        ExternalScan scan = scanApiHandler.startExternalDiscoveryScan(connectionInfoUuid);
        externalScans.put(scan.getUuid(), scan);
        return scan;
    }

    public void createOrUpdateExternalScan(ExternalScan scan) {
        externalScans.put(scan.getUuid(), scan);
    }

    private void handleExternalScan(ExternalScan scan) {
//        ExternalScan updatedScan = scanApiHandler
    }

    private void purgeOldScans() {
        externalScans.entrySet().removeIf(entry -> ExternalScan.Status.finishedStatuses.contains(entry.getValue().getStatus()));
        OffsetDateTime limitDate = OffsetDateTime.now().minusHours(6);
        externalScans.entrySet().stream().filter(entry -> entry.getValue().getCreatedAt().isBefore(limitDate)).forEach(entry -> {
            ExternalScan scan = entry.getValue();
            scan.setStatus(ExternalScan.Status.FAILED);
//          dbapiHandler.notifyExternalScanFinished(scan);
            externalScans.remove(entry.getKey());
        });
    }

    public void handleConnectionEstablishedWithScanner() {
        // region Send ExternalConnectionInfoTemplates to DB-API
        List<ExternalConnectionInfoTemplate> externalConnectionInfoTemplates = scanApiHandler.getExternalConnectionInfoTemplates();
        try {
            dbapiHandler.createOrUpdateExternalConnectionInfoTemplates(externalConnectionInfoTemplates);
        } catch (DbapiUnexpectedStatusCodeException | ExecutionException | InterruptedException | TimeoutException e) {
            logger.error("Error while handling external connection info templates", e);
        }
        // endregion Send ExternalConnectionInfoTemplates to DB-API
    }
}
