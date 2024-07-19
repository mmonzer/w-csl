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
 * Service for synchronization of deleted Roles
 */
public class RoleSynchronizationService extends AutocryptTemplateSynchronizationService {
    public RoleSynchronizationService(DbapiHandlerForCSLAutoCrypt dbapiHandlerForCSLAutoCrypt, ApiHandlerForCSLAutoCrypt apiHandlerForCSLAutoCrypt) {
        super( dbapiHandlerForCSLAutoCrypt, apiHandlerForCSLAutoCrypt, "SYNC-Autocrypt:Roles");
        logger = LoggerFactory.getLogger(RoleSynchronizationService.class);
    }

    @Override
    public List<Json> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException {
//        return super.retrieveData(apiHandlerForCSLAutoCrypt::getRoles, since, limit, offset);
        return super.retrieveData(apiHandlerForCSLAutoCrypt::getDeletedIssuers, apiHandlerForCSLAutoCrypt::getRoles, since, limit, offset);
    }

    @Override
    public void sendData(List<Json> items) throws SynchronizationException {
//        super.sendData(dbapiHandlerForCSLAutoCrypt::upsertRoles, Json.make(items));
        super.sendData(dbapiHandlerForCSLAutoCrypt::deleteRoles, dbapiHandlerForCSLAutoCrypt::upsertRoles, items);
    }

    @Override
    public OffsetDateTime getLastChangeDate() throws SynchronizationException {
        return super.getLastChangeDate(dbapiHandlerForCSLAutoCrypt::getRoleLastUpdateDate);
    }
}
