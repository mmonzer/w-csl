package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.ExternalConnectionInfoTemplate;
import com.csl.intercom.cslscan.models.ExternalDiscoveredDevice;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.services.exceptions.SynchronizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

public class ExternalConnectionInfoTemplatesSynchronizationService  extends PaginatedSynchronizationService<ExternalConnectionInfoTemplate> {
    private final Logger logger = LoggerFactory.getLogger(ExternalConnectionInfoTemplatesSynchronizationService.class);
    private final ScanApiHandler scanApiHandler;
    private final DbapiHandlerForCSLScan dbapiHandler;

    public ExternalConnectionInfoTemplatesSynchronizationService(ScanApiHandler scanApiHandler, DbapiHandlerForCSLScan dbapiHandler) {
        this.scanApiHandler = scanApiHandler;
        this.dbapiHandler = dbapiHandler;
    }

    public void synchronizeDiscoveredDevices() {
        logger.info("Synchronizing discovered devices");
        OffsetDateTime lastUpdate = dbapiHandler.getExternalConnectionInfoTemplatesLastUpdateDate();   // Not implemented
        List<ExternalDiscoveredDevice> discoveredDevices = scanApiHandler.getExternalDiscoveredDevices(lastUpdate);   // Correct, but not the right context (corresponds to other class). The right method should be implemented on scan
        dbapiHandler.createOrUpdateExternalDiscoveredDevices(discoveredDevices);   // Not implemented
    }

    @Override
    public List<ExternalConnectionInfoTemplate> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        try {
            return scanApiHandler.getExternalConnectionInfoTemplates(since, limit, offset);
        } catch (Exception e) {
            throw new SynchronizationException("Could not retrieve external connexion templates from CSL-Scan", e);
        }
    }

    @Override
    public void sendData(List<ExternalConnectionInfoTemplate> items) throws SynchronizationException {
        try {
            dbapiHandler.createOrUpdateExternalConnectionInfoTemplates(items);
        } catch (Exception e) {
            throw new SynchronizationException("Could not send external connexion templates to DB-API", e);
        }
    }

    @Override
    public OffsetDateTime getLastChangeDate() throws SynchronizationException {
        try {
            return dbapiHandler.getExternalConnectionInfoTemplatesLastUpdateDate();
        } catch (Exception e) {
            throw new SynchronizationException("Could not get last update date for external connexion templates from DB-API", e);
        }
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
