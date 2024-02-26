package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.csl.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

public class DeletedCpeItemsSynchronizationService extends PaginatedSynchronizationService<Pair<String, OffsetDateTime>> {
    private DbapiHandler dbapiHandler = new DbapiHandler();
    private ScanApiHandler scanApiHandler = new ScanApiHandler();
    private Logger logger = LoggerFactory.getLogger(DeletedCpeItemsSynchronizationService.class);

    @Override
    public List<Pair<String, OffsetDateTime>> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        try {
            return dbapiHandler.getDeletedCpeItemsSince(since, limit, offset);
        } catch (Exception e) {
            throw new SynchronizationException("Error while retrieving deleted CPE items from DBAPI", e);
        }
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
}
