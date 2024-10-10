package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.ExternalConnectionInfo;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.dbapi.exceptions.DbapiUnexpectedStatusCodeException;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.csl.logger.LoggerCustomEndpoints;
import com.csl.logger.LoggerInterfaces;
import com.csl.util.ThreadUtils;
import main.services.JsonApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ExternalConnectionInfoSynchronizationService {
    private Logger logger = LoggerFactory.getLogger(ExternalConnectionInfoSynchronizationService.class);
    private DbapiHandlerForCSLScan dbapiHandler;
    private ScanApiHandler scanApiHandler;
    private final long synchronizationIntervalSeconds;
    private ScheduledExecutorService scheduledExecutorService;

    public ExternalConnectionInfoSynchronizationService(ScanApiHandler scanApiHandler, DbapiHandlerForCSLScan dbapiHandler, long synchronizationIntervalSeconds) {
        this.scanApiHandler = scanApiHandler;
        this.dbapiHandler = dbapiHandler;
        this.synchronizationIntervalSeconds = synchronizationIntervalSeconds;

        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(
                scheduledExecutorService,
                this::synchronizeExternalConnectionInfos,
                0, synchronizationIntervalSeconds, java.util.concurrent.TimeUnit.SECONDS,
                LoggerCustomEndpoints.SYNC_EXT_CONNECTION_INFO, LoggerInterfaces.CSL_CLIENT
        );
    }

    public ExternalConnectionInfoSynchronizationService(ScanApiHandler scanApiHandler, DbapiHandlerForCSLScan dbapiHandler) {
        this(scanApiHandler, dbapiHandler, 3600);
    }

    synchronized public void synchronizeExternalConnectionInfos() {
        logger.info("Synchronizing external connection infos");
        List<ExternalConnectionInfo> connectionInfos = scanApiHandler.getExternalConnectionInfos(true);
        if (connectionInfos == null) {
            logger.warn("Error while getting external connection infos");
            return;
        }
        List<ExternalConnectionInfo> deletedConnectionInfos = connectionInfos.stream().filter(ExternalConnectionInfo::isDeleted).collect(Collectors.toList());
        connectionInfos.removeAll(deletedConnectionInfos);
        try {
            dbapiHandler.createOrUpdateExternalConnectionInfos(connectionInfos);
            deletedConnectionInfos.forEach(deletedConnectionInfo -> {
                try {
                    dbapiHandler.deleteExternalConnectionInfo(deletedConnectionInfo.getId());
                    scanApiHandler.deleteExternalConnectionInfo(deletedConnectionInfo.getId(), true);
                } catch (DbapiUnexpectedStatusCodeException | ExecutionException | InterruptedException | TimeoutException e) {
                    logger.error("Error while deleting external connection info", e);
                }

            });
        } catch (DbapiUnexpectedStatusCodeException | ExecutionException | InterruptedException | TimeoutException e) {
            logger.error("Error while synchronizing external connection infos", e);
        }
    }

    public void clear() throws SynchronizationException {
        JsonApiResponse response = scanApiHandler.clearExternalConnectionInfos();
        if (!response.isSuccess()) {
            throw new SynchronizationException("Error while clearing external connection infos in CSL-Scan");
        }
        try {
            dbapiHandler.clearExternalConnectionInfos();
        } catch (DbapiUnexpectedStatusCodeException | ExecutionException | InterruptedException | TimeoutException e) {
            throw new SynchronizationException("Error while clearing external connection infos in DB-API", e);
        }
    }

    public long getSynchronizationIntervalSeconds() {
        return synchronizationIntervalSeconds;
    }
}
