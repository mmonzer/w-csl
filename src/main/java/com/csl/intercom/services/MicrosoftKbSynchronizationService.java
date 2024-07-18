package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.MicrosoftKB;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.dbapi.models.ScanEntity;
import com.csl.intercom.services.exceptions.SynchronizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MicrosoftKbSynchronizationService extends PaginatedSynchronizationService<MicrosoftKB> {
    private DbapiHandlerForCSLScan dbapiHandler = new DbapiHandlerForCSLScan();
    private ScanApiHandler scanApiHandler = new ScanApiHandler();
    private CpeScanService cpeScanService;
    private final Logger logger = LoggerFactory.getLogger(MicrosoftKbSynchronizationService.class);

    public MicrosoftKbSynchronizationService(CpeScanService cpeScanService) {
        this.cpeScanService = cpeScanService;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public List<MicrosoftKB> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        if (cpeScanService.getRunningScan() == null && cpeScanService.getFinishedScan() == null) {
            throw new SynchronizationException("No scan is running or finished (MicrosoftKbSynchronizationService)");
        }
        return scanApiHandler.getMicrosoftKbChangesSince(since, limit, offset);
    }

    @Override
    public void sendData(List<MicrosoftKB> items) throws SynchronizationException {
        try {
            ScanEntity scanEntity = cpeScanService.getRunningScan();
            if (scanEntity == null) {
                scanEntity = cpeScanService.getFinishedScan();
            }
            if (scanEntity == null) {
                throw new SynchronizationException("No scan is running or finished (MicrosoftKbSynchronizationService)");
            }
            dbapiHandler.sendMicrosoftKbs(items, scanEntity);
        } catch (Exception e) {
            throw new SynchronizationException("Could not send CPE items to DB-API", e);
        }
    }

    @Override
    public OffsetDateTime getLastChangeDate() throws SynchronizationException {
        try {
            return dbapiHandler.getMicrosoftKbsLastUpdateDate();
        } catch (Exception e) {
            throw new SynchronizationException("Could not get last update date from DB-API", e);
        }
    }

    @PostSend
    public void hardDeleteMicrosoftKbs(List<MicrosoftKB> items) {
        AtomicReference<OffsetDateTime> maxDate = new AtomicReference<>(OffsetDateTime.MIN);
        items.forEach(item -> {
            if (item.getDiscoveredDate() != null && item.getDiscoveredDate().isAfter(maxDate.get())) {
                maxDate.set(item.getDiscoveredDate());
            }
        });
        if (!maxDate.get().isEqual(OffsetDateTime.MIN)) {
            scanApiHandler.deleteCpeItemsBeforeDate(maxDate.get(), false);
        }
    }
}
