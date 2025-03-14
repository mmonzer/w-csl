package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.csl.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

public class DeletedCpeItemsSynchronizationService extends PaginatedSynchronizationService<Pair<String, OffsetDateTime>> {
    private Logger logger = LoggerFactory.getLogger(DeletedCpeItemsSynchronizationService.class);
    private DbapiHandlerForCSLScan dbapiHandler;
    private ScanApiHandler scanApiHandler;

    public DeletedCpeItemsSynchronizationService(ScanApiHandler scanApiHandler, DbapiHandlerForCSLScan dbapiHandler) {
        this.scanApiHandler = scanApiHandler;
        this.dbapiHandler = dbapiHandler;
    }

    @Override
    public List<Pair<String, OffsetDateTime>> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        try {
            // get deleted cpe items from DB-API
            List<Pair<String, OffsetDateTime>> deletedCpeItems = dbapiHandler.getDeletedCpeItemsSince(since, limit, offset);
            deleteCpeItemsSynchronizationProcess(deletedCpeItems); // TODO: validate if it's in the right place
            return deletedCpeItems;

        } catch (Exception e) {
            throw new SynchronizationException("Error while retrieving deleted CPE items from DBAPI", e);
        }
    }

    public void deleteCpeItemsSynchronizationProcess(List<Pair<String, OffsetDateTime>> deletedCpeItems) throws Exception {
        // hard delete them in DB-API
        List<String> deletedCpeItemsIds = deletedCpeItems.stream().map(Pair::getFirst).toList();
        if (deletedCpeItemsIds.isEmpty()) {
            return;
        }
        dbapiHandler.hardDeleteCpeItemsByIds(deletedCpeItemsIds);
        // hard delete them in CSL-Scan
        scanApiHandler.deleteCpeItemsFromScan(deletedCpeItems, true);
        cleanCpeItems();
    }

    @Override
    public void sendData(List<Pair<String, OffsetDateTime>> data) {
        scanApiHandler.deleteCpeItemsFromScan(data, true);
    }

    @Override
    public OffsetDateTime getLastChangeDate() {
        return scanApiHandler.getLastCpeItemsDeletionDate();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }


    public void cleanCpeItems() throws Exception {
        // step 1: get soft deleted  of cpe item from dbapi
        List<Pair<String, OffsetDateTime>> softDeletedCpeItems  = dbapiHandler.getDeletedCpeItemsSince(null, 0, 0);
        if (softDeletedCpeItems.isEmpty()) {
            return;
        }
        // step2: hard delete them from csl-scan
        scanApiHandler.deleteCpeItemsFromScan(softDeletedCpeItems, true);
        // step3: hard delete them in dbapi
        // get min date
        OffsetDateTime minDate = softDeletedCpeItems.stream().map(Pair::getSecond).min(OffsetDateTime::compareTo).orElse(null);
        dbapiHandler.hardDeleteCpeItemsSince(minDate);
        // check if DB-API is empty, delete all CPE items in CSL-Scan
        if (dbapiHandler.getSoftDeletedCpeItemsCountFromDbApi() == 0) {
            scanApiHandler.deleteAllSoftDeletedCpeItemsInScan();
        }
    }
}
