package com.csl.autocrypt.services;

import com.csl.autocrypt.ApiHandlerForCSLAutoCrypt;
import com.csl.autocrypt.AutoCrypt;
import com.csl.autocrypt.DbapiHandlerForCSLAutoCrypt;
import com.csl.autocrypt.enums.AutocryptConstants.Common;
import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.dbapi.DbapiUtilsForCSLScan;
import com.csl.intercom.services.PaginatedSynchronizationService;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import org.slf4j.Logger;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public abstract class AutocryptTemplateSynchronizationService extends PaginatedSynchronizationService<Json> {
    protected final DbapiHandlerForCSLAutoCrypt dbapiHandlerForCSLAutoCrypt;
    protected final ApiHandlerForCSLAutoCrypt apiHandlerForCSLAutoCrypt;
    protected final String prefixLogger;
    protected Logger logger;

    public AutocryptTemplateSynchronizationService(DbapiHandlerForCSLAutoCrypt dbapiHandlerForCSLAutoCrypt, ApiHandlerForCSLAutoCrypt apiHandlerForCSLAutoCrypt, String prefixLogger) {
        super("CSL-Autocrypt");
        this.dbapiHandlerForCSLAutoCrypt = dbapiHandlerForCSLAutoCrypt;
        this.apiHandlerForCSLAutoCrypt = apiHandlerForCSLAutoCrypt;
        this.prefixLogger = prefixLogger;
    }

    public List<Json> retrieveData(IJsonToJsonApiResponse method, OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        Json params = Json.object();
        if (since != null) {
            params.set(Common.AFTER_UPDATED_DATE, ScanUtils.localTimeToScan(since).toString());
        }
        getLogger().debug("{} : retrieving data from Autocrypt after {}", prefixLogger, since);
        JsonApiResponse listIssuers = method.apply(params);

        if (!listIssuers.isSuccess() || listIssuers.getResult().isNull() || !listIssuers.getResult().isArray()) {
            getLogger().error("{} : Could not get retrieve data from Autocrypt.", prefixLogger);
            throw new SynchronizationException("Could not get retrieve data from Autocrypt");
        }

        getLogger().debug("{} : retrieved data from Autocrypt after {}", prefixLogger, since);

        return listIssuers.getResult().asJsonList();
    }

    public void sendData(IJsonToJsonApiResponse method, Json items) throws SynchronizationException {
        getLogger().debug("{} : sending data to DB-API : {}", prefixLogger, items);
        JsonApiResponse response = method.apply(items);
        if (!response.isSuccess()) {
            getLogger().error("{} : Could not send data to DB-API for Autocrypt service.", prefixLogger);
            throw new SynchronizationException(prefixLogger + " : Could not send data to DB-API for Autocrypt service.");
        }
        getLogger().info("{} : sent data to DB-API : {}", prefixLogger, items);
    }
//
//    public void sendData(IJsonToJsonApiResponse methodDelete, IJsonToJsonApiResponse methodUpsert, List<Json> items) throws SynchronizationException {
//        List<Json> itemsToDelete = new ArrayList<>();
//        List<Json> itemsToUpsert = new ArrayList<>();
//
//        // Prepare items
//        for (Json item : items) {
//            if (item.has(Common.DELETED) && item.get(Common.DELETED).asBoolean()) {
//                itemsToDelete.add(item);
//            } else {
//                itemsToUpsert.add(item);
//            }
//        }
//
//        // deleting items
//        getLogger().debug("{} : deleting data to DB-API : {}", prefixLogger, items);
//        JsonApiResponse response = methodDelete.apply(Json.make(itemsToDelete));
//        if (!response.isSuccess()) {
//            getLogger().error("{} : Could not delete data from DB-API for Autocrypt service.", prefixLogger);
//            throw new SynchronizationException(prefixLogger + " : Could not delete data to DB-API for Autocrypt service.");
//        }
//
//        // upserting items
//        getLogger().debug("{} : sending data to DB-API : {}", prefixLogger, items);
//        response = methodUpsert.apply(Json.make(itemsToUpsert));
//        if (!response.isSuccess()) {
//            getLogger().error("{} : Could not send data from DB-API for Autocrypt service.", prefixLogger);
//            throw new SynchronizationException(prefixLogger + " : Could not send data to DB-API for Autocrypt service.");
//        }
//
//        getLogger().info("{} : sent data to DB-API : {}", prefixLogger, items);
//    }

    public OffsetDateTime getLastChangeDate(IVoidToJsonApiResponse method) throws SynchronizationException {
        try {
            getLogger().debug("{} : fetching last update time from Dbapi", prefixLogger);
            JsonApiResponse response = method.apply();

            if (!response.isSuccess()) {
                throw new RuntimeException("Could not fetch last update time from Dbapi");
            }

            String lastUpdateDateString = Common.MIN_DATE;
            if (response.getResult()!=null && response.getResult().has(Common.VALUE) && response.getResult().get(Common.VALUE).isString()) {
                lastUpdateDateString = response.getResult().get(Common.VALUE).asString();
            }
            getLogger().debug("{} : fetched last update time from Dbapi : {}", prefixLogger, lastUpdateDateString);

            return DbapiUtilsForCSLScan.dbapiDateToLocal(lastUpdateDateString);
        } catch (Exception e) {
            getLogger().error("{} : Could not get last update date from DB-API for Autocrypt service.", prefixLogger);
            throw new SynchronizationException(prefixLogger + " : Could not get last update date from DB-API for Autocrypt service", e);
        }
    }

    @Override
    public boolean shouldSendEmptyData() {
        return true;
    }

    @Override
    public void syncData() throws SynchronizationException {
        getLogger().debug("{} : synchronizing ...", prefixLogger);
        super.syncData();
        getLogger().info("{} : Synchronization was successful", prefixLogger);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
