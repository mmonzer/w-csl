package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.CpeItem;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.dbapi.models.ScanEntity;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.csl.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class CpeItemsSynchronizationService extends PaginatedSynchronizationService<CpeItem> {
    private final DbapiHandler dbapiHandler = new DbapiHandler();
    private final ScanApiHandler scanApiHandler = new ScanApiHandler();
    private final CpeScanService cpeScanService;
    private final Logger logger = LoggerFactory.getLogger(CpeItemsSynchronizationService.class);

    public CpeItemsSynchronizationService(CpeScanService cpeScanService) {
        this.cpeScanService = cpeScanService;
    }

    @Override
    public List<CpeItem> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        if (cpeScanService.getRunningScan() == null && cpeScanService.getFinishedScan() == null) {
            throw new SynchronizationException("No scan is running or finished");
        }
        return scanApiHandler.getCpeItemChangesSince(since, limit, offset);
    }

    @Override
    public void sendData(List<CpeItem> items) throws SynchronizationException {
        try {
            ScanEntity scanEntity = cpeScanService.getRunningScan();
            if (scanEntity == null) {
                scanEntity = cpeScanService.getFinishedScan();
            }
            if (scanEntity == null) {
                throw new SynchronizationException("No running scan found");
            }
            dbapiHandler.sendCpeItems(items, scanEntity);
        } catch (Exception e) {
            throw new SynchronizationException("Could not send CPE items to DB-API", e);
        }
    }

    @Override
    public OffsetDateTime getLastChangeDate() throws SynchronizationException {
        try {
            return dbapiHandler.getCpeItemsLastUpdateDate();
        } catch (Exception e) {
            throw new SynchronizationException("Could not get last update date from DB-API", e);
        }
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    /**
     * Hard-delete the soft-deleted CPE items from the scan API after they have been sent to the DB-API.
     *
     * @param items The data that was sent to the DB-API.
     */
    @PostSend
    public void hardDeleteCpeItems(List<CpeItem> items) {
        AtomicReference<OffsetDateTime> lastChangeDate = new AtomicReference<>(OffsetDateTime.MIN);
        items.forEach(item -> {
            if (item.isDeleted() && item.getDiscoveredDate().isAfter(lastChangeDate.get())) {
                lastChangeDate.set(item.getDiscoveredDate());
            }
        });
        if (!lastChangeDate.get().isEqual(OffsetDateTime.MIN)) {
            scanApiHandler.deleteCpeItemsBeforeDate(lastChangeDate.get(), false);
        }
    }
}
