package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.ExternalConnectionInfo;
import com.csl.intercom.cslscan.models.ExternalDiscoveredDevice;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.dbapi.exceptions.DbapiUnexpectedStatusCodeException;
import com.csl.logger.LoggerCustomEndpoints;
import com.csl.util.ListUtils;
import com.csl.util.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

public class ExternalConnectionInfoTemplatesSynchronizationService {
    private Logger logger = LoggerFactory.getLogger(ExternalConnectionInfoTemplatesSynchronizationService.class);
    private ScanApiHandler scanApiHandler;
    private DbapiHandlerForCSLScan dbapiHandler;
    private final long synchronizationIntervalSeconds;
    private ScheduledExecutorService scheduledExecutorService;

    public ExternalConnectionInfoTemplatesSynchronizationService(ScanApiHandler scanApiHandler, DbapiHandlerForCSLScan dbapiHandler, long synchronizationIntervalSeconds) {
        this.scanApiHandler = scanApiHandler;
        this.dbapiHandler = dbapiHandler;
        this.synchronizationIntervalSeconds = synchronizationIntervalSeconds;

        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(
                scheduledExecutorService,
                () -> {
                    this.synchronizeDiscoveredDevices();
                    logger.info("Successfully synchronized external connection's information templates.");
                }, 0,
                synchronizationIntervalSeconds, java.util.concurrent.TimeUnit.SECONDS,
                LoggerCustomEndpoints.SYNC_DISCOVERED_DEVICES
        );
    }

    public void synchronizeDiscoveredDevices() {
        logger.info("Synchronizing discovered devices");
        OffsetDateTime lastUpdate = dbapiHandler.getExternalConnectionInfoTemplatesLastUpdateDate();
        List<ExternalDiscoveredDevice> discoveredDevices = scanApiHandler.getExternalDiscoveredDevices(lastUpdate);
        dbapiHandler.createOrUpdateExternalDiscoveredDevices(discoveredDevices);
    }

    public synchronized void synchronizeExternalConnectionInfoTemplates() {
        logger.info("Synchronizing external connection info templates");

        OffsetDateTime lastUpdate = dbapiHandler.getExternalConnectionInfoTemplatesLastUpdateDate();
        List<ExternalDiscoveredDevice> discoveredDevices = scanApiHandler.getExternalDiscoveredDevices(lastUpdate);
        dbapiHandler.createOrUpdateExternalDiscoveredDevices(discoveredDevices);



        List<ExternalConnectionInfo> connectionInfos = scanApiHandler.getExternalConnectionInfos(true);
        if (connectionInfos == null) {
            logger.warn("Error while getting external connection infos");
            return;
        }
        List<ExternalConnectionInfo> deletedConnectionInfos = ListUtils.filter(connectionInfos, ExternalConnectionInfo::isDeleted);
        connectionInfos.removeAll(deletedConnectionInfos);
        try {
            dbapiHandler.createOrUpdateExternalConnectionInfos(connectionInfos);
            deletedConnectionInfos.forEach(deletedConnectionInfo -> {
                try {
                    dbapiHandler.deleteExternalConnectionInfo(deletedConnectionInfo.getId());
                    scanApiHandler.deleteExternalConnectionInfo(deletedConnectionInfo.getId(), true);
                } catch (DbapiUnexpectedStatusCodeException | ExecutionException | InterruptedException | TimeoutException e) {
                    logger.error("Error while deleting external connection info: {}", e.getMessage());
                }

            });
        } catch (DbapiUnexpectedStatusCodeException | ExecutionException | InterruptedException | TimeoutException e) {
            logger.error("Error while synchronizing external connection infos: {}", e.getMessage());
        }
    }
}
