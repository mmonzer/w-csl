package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.ExternalDiscoveredDevice;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.logger.LoggerCustomEndpoints;
import com.csl.util.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
}
