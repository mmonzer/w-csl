package com.csl.intercom.services;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.csl.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

import com.csl.util.Pair;

import java.time.OffsetDateTime;

public class DeletedConnectionsInfoSynchronizationService extends PaginatedSynchronizationService<Pair<String, OffsetDateTime>> {
    private Logger logger = LoggerFactory.getLogger(DeletedMicrosoftKbsSynchronizationService.class);
    private DbapiHandlerForCSLScan dbapiHandler;
    private ScanApiHandler scanApiHandler;

    public DeletedConnectionsInfoSynchronizationService(ScanApiHandler scanApiHandler, DbapiHandlerForCSLScan dbapiHandler) {
        this.scanApiHandler = scanApiHandler;
        this.dbapiHandler = dbapiHandler;
    }
    @Override
    protected Logger getLogger() {
        return null;
    }

    @Override
    public List<Pair<String, OffsetDateTime>> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        return List.of();
    }

    public void deleteMicrosoftKbSynchronizationProcess(List<Pair<String, OffsetDateTime>> deletedMicrosoftKbs) throws Exception {
     // TODO
    }

    @Override
    public void sendData(List<Pair<String, OffsetDateTime>> items) throws SynchronizationException {

    }

    @Override
    public OffsetDateTime getLastChangeDate() throws SynchronizationException {
        return null;
    }
    public void cleanMicrosoftKbs() throws Exception {

    }
}
