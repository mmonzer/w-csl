package com.csl.autocrypt.services;

import com.csl.autocrypt.AutoCrypt;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.ucsl.json.Json;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service for synchronization of Issuers
 */
public class IssuerSynchronizationService extends AutocryptTemplateSynchronizationService {
    public IssuerSynchronizationService(AutoCrypt autocrypt) {
        super( autocrypt, "SYNC-Autocrypt:Issuers");
        logger = LoggerFactory.getLogger(IssuerSynchronizationService.class);
    }

    @Override
    public List<Json> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        return super.retrieveData(autocrypt.getAutocryptApiHandler()::getIssuers, since, limit, offset);
    }

    @Override
    public void sendData(List<Json> items) throws SynchronizationException {
         super.sendData(autocrypt.getDbApiHandler()::upsertIssuers, Json.make(items));
    }

    @Override
    public OffsetDateTime getLastChangeDate() throws SynchronizationException {
        return super.getLastChangeDate(autocrypt.getDbApiHandler()::getIssuerLastUpdateDate);
    }
}
