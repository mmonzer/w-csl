package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.csl.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

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
            // get deleted microsoft kbs from DB-API
            List<Pair<String, OffsetDateTime>> deletedMicrosoftKbs = dbapiHandler.getDeletedMicrosoftKbsSince(since, limit, offset);
            deleteMicrosoftKbSynchronizationProcess(deletedMicrosoftKbs); // TODO: validate if it's in the right place
            return deletedMicrosoftKbs;
        } catch (Exception e) {
            throw new SynchronizationException("Error while retrieving deleted Microsoft KBs from DBAPI", e);
        }
    }

    public void deleteMicrosoftKbSynchronizationProcess(List<Pair<String, OffsetDateTime>> deletedMicrosoftKbs) throws Exception {
        // hard delete them in DB-API
        List<String> deletedMicrosoftKbIds = deletedMicrosoftKbs.stream().map(Pair::getFirst).toList();
        if (deletedMicrosoftKbIds.isEmpty()) {
            return;
        }
        dbapiHandler.hardDeleteMicrosoftKbsByIds(deletedMicrosoftKbIds);
        // hard delete them in CSL-Scan
        scanApiHandler.deleteMicrosoftKBsFromScan(deletedMicrosoftKbs, true);
        cleanMicrosoftKbs();
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


    public void cleanMicrosoftKbs() throws Exception {
        // step 1: get soft deleted id of microsoft kbs from dbapi
        List<Pair<String, OffsetDateTime>> softDeletedMicrosoftKbs  = dbapiHandler.getDeletedMicrosoftKbsSince(null, 0, 0);
        if (softDeletedMicrosoftKbs.isEmpty()) {
            return;
        }
        // step2: hard delete them from csl-scan
        scanApiHandler.deleteMicrosoftKBsFromScan(softDeletedMicrosoftKbs, true);
        // step3: hard delete them in dbapi
        // get min date
        OffsetDateTime minDate = softDeletedMicrosoftKbs.stream().map(Pair::getSecond).min(OffsetDateTime::compareTo).orElse(null);
        dbapiHandler.hardDeleteMicrosoftKbsSince(minDate);
        // check if dbapi is empty , delete all microsoft kbs in CSL-SCAN
        if (dbapiHandler.getSoftDeletedMicrosoftKbsCountFromDbApi() == 0) {
            scanApiHandler.deleteAllSoftDeletedMicrosoftKbsInScan();
        }
    }
}
