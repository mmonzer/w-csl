package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.csl.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

public class DeletedMicrosoftKbsSynchronizationService extends PaginatedSynchronizationService<Pair<String, OffsetDateTime>> {
    private DbapiHandler dbapiHandler = new DbapiHandler();
    private ScanApiHandler scanApiHandler = new ScanApiHandler();
    private Logger logger = LoggerFactory.getLogger(DeletedMicrosoftKbsSynchronizationService.class);

    @Override
    public List<Pair<String, OffsetDateTime>> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        try {
            return dbapiHandler.getDeletedMicrosoftKbsSince(since, limit, offset);
        } catch (Exception e) {
            throw new SynchronizationException("Error while retrieving deleted Microsoft KBs from DBAPI", e);
        }
    }

    @Override
    public void sendData(List<Pair<String, OffsetDateTime>> data) {
        scanApiHandler.deleteMicrosoftKBsFromScan(data);
    }

    @Override
    public OffsetDateTime getLastChangeDate() {
        return scanApiHandler.getLastMicrosoftKbsDeletionDate();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
