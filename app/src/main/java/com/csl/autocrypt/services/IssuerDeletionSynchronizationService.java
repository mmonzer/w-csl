package com.csl.autocrypt.services;

import com.csl.autocrypt.ApiHandlerForCSLAutoCrypt;
import com.csl.autocrypt.DbapiHandlerForCSLAutoCrypt;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.ucsl.json.Json;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service for synchronization of Issuers
 */
public class IssuerDeletionSynchronizationService extends AutocryptTemplateSynchronizationService {
    public IssuerDeletionSynchronizationService(DbapiHandlerForCSLAutoCrypt dbapiHandlerForCSLAutoCrypt, ApiHandlerForCSLAutoCrypt apiHandlerForCSLAutoCrypt) {
        super( dbapiHandlerForCSLAutoCrypt, apiHandlerForCSLAutoCrypt, "SYNC-Autocrypt:IssuersDeletion");
        logger = LoggerFactory.getLogger(IssuerDeletionSynchronizationService.class);
    }

    @Override
    public List<Json> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        return super.retrieveData(apiHandlerForCSLAutoCrypt::getDeletedIssuers, since, limit, offset);
    }

    @Override
    public void sendData(List<Json> items) throws SynchronizationException {
        super.sendData(dbapiHandlerForCSLAutoCrypt::deleteIssuers, Json.make(items));
    }

    @Override
    public OffsetDateTime getLastChangeDate() throws SynchronizationException {
        return super.getLastChangeDate(dbapiHandlerForCSLAutoCrypt::getIssuerLastUpdateDate);
    }
}
