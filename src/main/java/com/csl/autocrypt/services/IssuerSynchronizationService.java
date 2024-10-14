package com.csl.autocrypt.services;

import com.csl.autocrypt.ApiHandlerForCSLAutoCrypt;
import com.csl.autocrypt.DbapiHandlerForCSLAutoCrypt;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.csl.logger.CSLApplicativeLogger;
import com.ucsl.json.Json;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service for synchronization of Issuers
 */
public class IssuerSynchronizationService extends AutocryptTemplateSynchronizationService {
    public IssuerSynchronizationService(DbapiHandlerForCSLAutoCrypt dbapiHandlerForCSLAutoCrypt, ApiHandlerForCSLAutoCrypt apiHandlerForCSLAutoCrypt) {
        super( dbapiHandlerForCSLAutoCrypt, apiHandlerForCSLAutoCrypt, "SYNC-Autocrypt:Issuers");
        logger = CSLApplicativeLogger.getLogger(IssuerSynchronizationService.class);
    }

    @Override
    public List<Json> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        return super.retrieveData(apiHandlerForCSLAutoCrypt::getDeletedIssuers, apiHandlerForCSLAutoCrypt::getIssuers, since, limit, offset);
    }

    @Override
    public void sendData(List<Json> items) throws SynchronizationException {
        super.sendData(dbapiHandlerForCSLAutoCrypt::deleteIssuers, dbapiHandlerForCSLAutoCrypt::upsertIssuers, items);
    }

    @Override
    public OffsetDateTime getLastChangeDate() throws SynchronizationException {
        return super.getLastChangeDate(dbapiHandlerForCSLAutoCrypt::getIssuerLastUpdateDate);
    }
}
