package com.csl.autocrypt.services;

import com.csl.autocrypt.ApiHandlerForCSLAutoCrypt;
import com.csl.autocrypt.AutoCrypt;
import com.csl.autocrypt.DbapiHandlerForCSLAutoCrypt;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.ucsl.json.Json;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service for synchronization of Certificates : revoked and not revoked
 */
public class CertificateSynchronizationService extends AutocryptTemplateSynchronizationService {

    public CertificateSynchronizationService(DbapiHandlerForCSLAutoCrypt dbapiHandlerForCSLAutoCrypt, ApiHandlerForCSLAutoCrypt apiHandlerForCSLAutoCrypt) {
        super( dbapiHandlerForCSLAutoCrypt, apiHandlerForCSLAutoCrypt, "SYNC-Autocrypt:Certificates");
        logger = LoggerFactory.getLogger(CertificateSynchronizationService.class);
    }

    @Override
    public List<Json> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        return super.retrieveData(apiHandlerForCSLAutoCrypt::getCertificates, since, limit, offset);
    }

    @Override
    public void sendData(List<Json> items) throws SynchronizationException {
        super.sendData(dbapiHandlerForCSLAutoCrypt::upsertCertificates, Json.make(items));
    }

    @Override
    public OffsetDateTime getLastChangeDate() throws SynchronizationException {
        return super.getLastChangeDate(dbapiHandlerForCSLAutoCrypt::getCertificateLastUpdateDate);
    }
}
