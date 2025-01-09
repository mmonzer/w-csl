package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.ExternalConnectionInfo;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.dbapi.exceptions.DbapiUnexpectedStatusCodeException;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.csl.logger.CSLApplicativeLogger;
import com.csl.util.ListUtils;
import main.services.JsonApiResponse;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

public class ExternalConnectionInfoSynchronizationService {
    private CSLApplicativeLogger logger = CSLApplicativeLogger.getLogger(ExternalConnectionInfoSynchronizationService.class);
    private DbapiHandlerForCSLScan dbapiHandler;
    private ScanApiHandler scanApiHandler;
    private final long synchronizationIntervalSeconds;
    private ScheduledExecutorService scheduledExecutorService;

    public ExternalConnectionInfoSynchronizationService(ScanApiHandler scanApiHandler, DbapiHandlerForCSLScan dbapiHandler, long synchronizationIntervalSeconds) {
        this.scanApiHandler = scanApiHandler;
        this.dbapiHandler = dbapiHandler;
        this.synchronizationIntervalSeconds = synchronizationIntervalSeconds;

//        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
//        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(
//                scheduledExecutorService,
//                () -> {
//                    this.synchronizeAllExternalConnectionInfos();
//                    logger.info("Successfully synchronized the external connection's informations.");
//                },
//                0, synchronizationIntervalSeconds, java.util.concurrent.TimeUnit.SECONDS,
//                LoggerCustomEndpoints.SYNC_EXT_CONNECTION_INFO
//        );
    }

    public ExternalConnectionInfoSynchronizationService(ScanApiHandler scanApiHandler, DbapiHandlerForCSLScan dbapiHandler) {
        this(scanApiHandler, dbapiHandler, 3600);
    }

    /**
     *  This method synchronizes all external connection infos from the scan API and updates them in the DB-API.
     *  Not lastUpdate is included in the synchronization, thus all the items are synchronized.
     */
    public synchronized void synchronizeAllExternalConnectionInfos() {
        logger.info("Synchronizing external connection infos");
        List<ExternalConnectionInfo> connectionInfos = scanApiHandler.getExternalConnectionInfos(true);
        if (connectionInfos == null) {
            logger.warn("Error while getting external connection infos");
            return;
        }
        List<ExternalConnectionInfo> deletedConnectionInfos = ListUtils.toList(connectionInfos.stream().filter(ExternalConnectionInfo::isDeleted));
        connectionInfos.removeAll(deletedConnectionInfos);
        try {
            dbapiHandler.createOrUpdateExternalConnectionInfos(connectionInfos);
            deletedConnectionInfos.forEach(deletedConnectionInfo -> {
                try {
                    dbapiHandler.deleteExternalConnectionInfo(deletedConnectionInfo.getId());
                    scanApiHandler.deleteExternalConnectionInfo(deletedConnectionInfo.getId(), true);
                } catch (DbapiUnexpectedStatusCodeException | ExecutionException | InterruptedException | TimeoutException e) {
                    logger.error("Error while deleting external connection info: {}", e.getMessage());
                }

            });
        } catch (DbapiUnexpectedStatusCodeException | ExecutionException | InterruptedException | TimeoutException e) {
            logger.error("Error while synchronizing external connection infos: {}", e.getMessage());
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
