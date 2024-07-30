package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.csl.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DeletedMicrosoftKbsSynchronizationService extends PaginatedSynchronizationService<Pair<String, OffsetDateTime>> {
    private Logger logger = LoggerFactory.getLogger(DeletedMicrosoftKbsSynchronizationService.class);
    private DbapiHandlerForCSLScan dbapiHandler;
    private ScanApiHandler scanApiHandler;

    public DeletedMicrosoftKbsSynchronizationService(ScanApiHandler scanApiHandler, DbapiHandlerForCSLScan dbapiHandler) {
        this.scanApiHandler = scanApiHandler;
        this.dbapiHandler = dbapiHandler;
    }

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
        scanApiHandler.deleteMicrosoftKBsFromScan(data, true);
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
