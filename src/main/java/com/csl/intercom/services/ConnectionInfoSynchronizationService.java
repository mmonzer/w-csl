package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.dbapi.models.Connection;
import com.csl.intercom.dbapi.models.ConnectionProtocol;
import com.csl.intercom.services.exceptions.SynchronizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ConnectionInfoSynchronizationService extends PaginatedSynchronizationService<Connection> {
    private final Logger logger = LoggerFactory.getLogger(ConnectionInfoSynchronizationService.class);
    private final ScanApiHandler scanApiHandler = new ScanApiHandler();
    private final DbapiHandlerForCSLScan dbapiHandler = new DbapiHandlerForCSLScan();
    private List<ConnectionProtocol> protocols;

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public List<Connection> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        return scanApiHandler.getConnectionsSince(since, limit, offset, this.protocols);
    }

    @Override
    public void sendData(List<Connection> items) throws SynchronizationException {
        try {
            dbapiHandler.sendConnections(items);
        } catch (Exception e) {
            throw new SynchronizationException("Could not send connections to DB-API", e);
        }
    }

    @Override
    public OffsetDateTime getLastChangeDate() throws SynchronizationException {
        return scanApiHandler.getConnectionLastUpdatedDate();
    }

    @PreReceive
    public void getProtocols() {
        try {
            this.protocols = dbapiHandler.fetchDiscoveryProtocols();
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            logger.error("Could not fetch discovery protocols", e);
            this.protocols = null;
        }
    }
}
