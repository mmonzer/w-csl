package com.csl.intercom.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.CpeItem;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.services.exceptions.SynchronizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

public abstract class PaginatedSynchronizationService<T> implements DataSynchronizationService {
    private int batch_max_size = 200;

    @Override
    public void syncData() throws SynchronizationException {
        OffsetDateTime lastChangeDate;
        try {
            lastChangeDate = getLastChangeDate();
        } catch (Exception e) {
            getLogger().warn("Could not retrieve last update date from DB-API, retrieving all CPE Items from CSL-Scan");
            getLogger().debug("Could not retrieve last update date from DB-API", e);
            lastChangeDate = null;
        }
        syncData(lastChangeDate);
    }

    public void syncData(OffsetDateTime since) throws SynchronizationException {
        int offset = 0;
        int limit = getBatchMaxSize();
        List<T> items;
        do {
            items = retrieveData(since, limit, offset);
            if (items != null && !items.isEmpty()) {
                try {
                    sendData(items);
                } catch (SynchronizationException e) {
                    getLogger().warn("Failed to send data to DB-API", e);
                    getLogger().debug("Failed to send data to DB-API", e);
                    throw e;
                }
            }
            offset += limit;
        } while (items != null && items.size() == limit);
    }

    protected abstract Logger getLogger();

    public abstract List<T> retrieveData(OffsetDateTime since, int limit, int offset) throws SynchronizationException;

    public abstract void sendData(List<T> items) throws SynchronizationException;
    public abstract OffsetDateTime getLastChangeDate() throws SynchronizationException;

    public int getBatchMaxSize() {
        return batch_max_size;
    }

    public PaginatedSynchronizationService setBatchMaxSize(int batch_max_size) {
        getLogger().debug("Setting batch_max_size to {}", batch_max_size);
        this.batch_max_size = batch_max_size;
        return this;
    }
}
