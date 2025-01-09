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
        OffsetDateTime lastUpdate = dbapiHandler.getExternalConnectionInfoTemplatesLastUpdateDate();   // Not implemented
        List<ExternalDiscoveredDevice> discoveredDevices = scanApiHandler.getExternalDiscoveredDevices(lastUpdate);   // Correct, but not the right context (corresponds to other class). The right method should be implemented on scan
        dbapiHandler.createOrUpdateExternalDiscoveredDevices(discoveredDevices);   // Not implemented
    }

    public synchronized void synchronizeExternalConnectionInfoTemplates() {
        logger.warn("Not yet implemented");
    }
}
