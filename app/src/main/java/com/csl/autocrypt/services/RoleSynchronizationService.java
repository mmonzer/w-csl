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
public class RoleSynchronizationService extends AutocryptTemplateSynchronizationService {
    public RoleSynchronizationService(AutoCrypt autocrypt) {
        super( autocrypt, "SYNC-Autocrypt:Roles");
        logger = LoggerFactory.getLogger(RoleSynchronizationService.class);
    }

    @Override
    public List<Json> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        return super.retrieveData(autocrypt.getAutocryptApiHandler()::getRoles, since, limit, offset);
    }

    @Override
    public void sendData(List<Json> items) throws SynchronizationException {
         super.sendData(autocrypt.getDbApiHandler()::upsertRoles, Json.make(items));
    }

    @Override
    public OffsetDateTime getLastChangeDate() throws SynchronizationException {
        return super.getLastChangeDate(autocrypt.getDbApiHandler()::getRoleLastUpdateDate);
    }
}
