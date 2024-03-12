package com.csl.intercom.services;

import com.csl.intercom.services.exceptions.SynchronizationException;
import org.slf4j.Logger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Abstract class for paginated data synchronization services.
 * This class provides a framework for paginated data synchronization services.
 * In most cases, implementing classes will only need to implement the retrieveData, sendData, getLastChangeDate and getLogger methods,
 * the actual synchronization will be handled by this class.
 * The caller of the syncData method will not need to worry about the pagination of the data.
 *
 * The synchronization process will be as follows:
 * 1. Retrieve the last change date from the DB-API
 * 2. Retrieve data from the source system in batches
 * 3. Pre-send processing
 * 4. Send data to the DB-API
 * 5. Post-send processing
 * 6. Repeat steps 2-5 until all data has been sent
 *
 * The batch size can be set using the setBatchMaxSize method.
 *
 * The Pre-Send and Post-Send methods can be annotated with the PreSend and PostSend annotations respectively.
 *
 * @param <T> Type of the data to synchronize
 */
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
                    doPreSendActions(items);
                    sendData(items);
                    doPostSendActions(items);
                } catch (SynchronizationException e) {
                    getLogger().warn("Failed to send data to DB-API");
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

    protected void doPreSendActions(List<T> data) throws SynchronizationException {
        Class<?> clazz = this.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PreSend.class)) {
                try {
                    method.invoke(this, data);
                } catch (Exception e) {
                    getLogger().warn("Failed to execute preSend method");
                    getLogger().debug("Failed to execute preSend method", e);
                    throw new SynchronizationException("Failed to execute preSend method", e);
                }
            }
        }
    }

    protected void doPostSendActions(List<T> data) throws SynchronizationException {
        Class<?> clazz = this.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostSend.class)) {
                try {
                    method.invoke(this, data);
                } catch (Exception e) {
                    getLogger().warn("Failed to execute postSend method");
                    getLogger().debug("Failed to execute postSend method", e);
                    throw new SynchronizationException("Failed to execute postSend method", e);
                }
            }
        }
    }

    /**
     * Annotation for methods that should be executed before sending data to the DB-API.
     * Note that order of execution is not guaranteed.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface PreSend {}

    /**
     * Annotation for methods that should be executed after sending data to the DB-API.
     * Note that order of execution is not guaranteed.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface PostSend {}
}
