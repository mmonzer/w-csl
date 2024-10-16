package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.ExternalConnectionInfoTemplate;
import com.csl.intercom.cslscan.models.scans.ExternalScan;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.dbapi.exceptions.DbapiUnexpectedStatusCodeException;
import com.csl.intercom.services.exceptions.SynchronizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ExternalScansService {
    private Logger logger = LoggerFactory.getLogger(ExternalScansService.class);
    private DbapiHandlerForCSLScan dbapiHandler;
    private ScanApiHandler scanApiHandler;
    private ExternalDiscoveredDevicesSynchronizationService externalDiscoveredDevicesSynchronizationService;

    private Map<String, ExternalScan> externalScans = new ConcurrentHashMap<>();

    public ExternalScansService(DbapiHandlerForCSLScan dbapiHandler, ScanApiHandler scanApiHandler, ExternalDiscoveredDevicesSynchronizationService externalDiscoveredDevicesSynchronizationService) {
        this.dbapiHandler = dbapiHandler;
        this.scanApiHandler = scanApiHandler;
        this.externalDiscoveredDevicesSynchronizationService = externalDiscoveredDevicesSynchronizationService;
    }

    public ExternalScan getDeviceDiscoveryScan(String uuid) {
        return externalScans.get(uuid);
    }

    public ExternalScan getLastDeviceDiscoveryScan() {
        return externalScans.values().stream().max(Comparator.comparing(ExternalScan::getCreatedAt)).orElse(null);
    }

    public ExternalScan startExternalDiscoveryScan(String connectionInfoUuid) {
        ExternalScan scan = scanApiHandler.startExternalDiscoveryScan(connectionInfoUuid);
        try {
            int dbapiId = dbapiHandler.createExternalDeviceScanEvent(scan);
            scan.setDbapiId(dbapiId);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            logger.warn("Failed to create external device scan event", e);
        } catch (DbapiUnexpectedStatusCodeException e) {
            logger.error("Unexpected status code while creating external device scan event", e);
        }
        externalScans.put(scan.getUuid(), scan);
        return scan;
    }

    public void createOrUpdateExternalScan(ExternalScan scan) {
        if (externalScans.containsKey(scan.getUuid())) {
            scan.setDbapiId(externalScans.get(scan.getUuid()).getDbapiId());
        }
        externalScans.put(scan.getUuid(), scan);
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
        List<ExternalConnectionInfoTemplate> externalConnectionInfoTemplates = scanApiHandler.getExternalConnectionInfoTemplates();
        try {
            dbapiHandler.createOrUpdateExternalConnectionInfoTemplates(externalConnectionInfoTemplates);
        } catch (DbapiUnexpectedStatusCodeException | ExecutionException | InterruptedException | TimeoutException e) {
            logger.error("Error while handling external connection info templates", e);
        } catch (Exception e) {
            logger.error("Unexpected error when handling established connection", e);
        }
    }

    public void handleScanNotification(ExternalScan scan) {
        createOrUpdateExternalScan(scan);
        try {
            externalDiscoveredDevicesSynchronizationService.syncData();
        } catch (SynchronizationException e) {
            logger.warn("Failed to synchronize external discovered devices", e);
        }
    }
}
