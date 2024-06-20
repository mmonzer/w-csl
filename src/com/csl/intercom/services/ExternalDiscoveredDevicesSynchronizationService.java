package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.ExternalDiscoveredDevice;
import com.csl.intercom.cslscan.models.scans.ExternalScan;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.dbapi.exceptions.DbapiUnexpectedStatusCodeException;
import com.csl.intercom.services.exceptions.SynchronizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ExternalDiscoveredDevicesSynchronizationService extends PaginatedSynchronizationService<ExternalDiscoveredDevice> {
    private final Logger logger = LoggerFactory.getLogger(ExternalDiscoveredDevicesSynchronizationService.class);
    private final DbapiHandler dbapiHandler;
    private final ScanApiHandler scanApiHandler;
    private ExternalScansService externalScansService = null;

    public ExternalDiscoveredDevicesSynchronizationService(DbapiHandler dbapiHandler, ScanApiHandler scanApiHandler) {
        this.dbapiHandler = dbapiHandler;
        this.scanApiHandler = scanApiHandler;
    }

    public void init(ExternalScansService externalScansService) {
        this.externalScansService = externalScansService;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public List<ExternalDiscoveredDevice> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        return scanApiHandler.getExternalDiscoveredDevices(since, limit, offset);
    }

    @Override
    public void sendData(List<ExternalDiscoveredDevice> items) throws SynchronizationException {
        ExternalScan scan = externalScansService.getLastDeviceDiscoveryScan();
        if (scan == null) {
            throw new SynchronizationException("No device discovery scan found");
        }
        try {
            dbapiHandler.sendExternalDiscoveredDevices(items, scan);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new SynchronizationException("Could not send discovered devices to DB-API", e);
        } catch (DbapiUnexpectedStatusCodeException e) {
            throw new SynchronizationException("Unexpected status code while sending discovered devices to DB-API", e);
        }
    }

    @Override
    public OffsetDateTime getLastChangeDate() throws SynchronizationException {
        try {
            return dbapiHandler.getExternalDiscoveredDevicesLastUpdateDate();
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new SynchronizationException("Could not get last update date from DB-API", e);
        }
    }
}
