package com.csl.intercom.services;

import com.csl.intercom.services.exceptions.SynchronizationException;

import java.time.OffsetDateTime;

public interface DataSynchronizationService {
    void syncData(OffsetDateTime since) throws SynchronizationException;

    default void syncData() throws SynchronizationException {
        syncData(null);
    }
}