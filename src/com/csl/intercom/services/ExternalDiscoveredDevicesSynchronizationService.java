package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.ExternalDiscoveredDevice;
import com.csl.intercom.dbapi.DbapiHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ExternalDiscoveredDevicesSynchronizationService {
    private Logger logger = LoggerFactory.getLogger(ExternalDiscoveredDevicesSynchronizationService.class);
    private ScanApiHandler scanApiHandler;
    private DbapiHandler dbapiHandler;
    private final long synchronizationIntervalSeconds;
    private ScheduledExecutorService scheduledExecutorService;

    public ExternalDiscoveredDevicesSynchronizationService(ScanApiHandler scanApiHandler, DbapiHandler dbapiHandler, long synchronizationIntervalSeconds) {
        this.scanApiHandler = scanApiHandler;
        this.dbapiHandler = dbapiHandler;
        this.synchronizationIntervalSeconds = synchronizationIntervalSeconds;

        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.scheduledExecutorService.scheduleAtFixedRate(this::synchronizeDiscoveredDevices, 0, synchronizationIntervalSeconds, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void synchronizeDiscoveredDevices() {
        logger.info("Synchronizing discovered devices");
        OffsetDateTime lastUpdate = dbapiHandler.getExternalConnectionInfoTemplatesLastUpdateDate();
        List<ExternalDiscoveredDevice> discoveredDevices = scanApiHandler.getExternalDiscoveredDevices(lastUpdate);
        dbapiHandler.createOrUpdateExternalDiscoveredDevices(discoveredDevices);
    }
}
