package com.csl.autocrypt.services;

import com.csl.autocrypt.ApiHandlerForCSLAutoCrypt;
import com.csl.autocrypt.AutoCrypt;
import com.csl.autocrypt.DbapiHandlerForCSLAutoCrypt;
import com.csl.autocrypt.enums.AutocryptConstants.Common;
import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.dbapi.DbapiUtilsForCSLScan;
import com.csl.intercom.services.PaginatedSynchronizationService;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.ucsl.interfaces.IVoidToJsonApiResponse;
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
        getLogger().trace("{} : retrieving data from Autocrypt after {} with limit {} and offset {} : method {}", prefixLogger, since, limit, offset, method.toString());

        Json params = Json.object();

        if (since != null) {
            params.set(Common.AFTER_UPDATED_DATE, ScanUtils.localTimeToScan(since).toString());
        }
        getLogger().debug("{} : retrieving data from Autocrypt after {}", prefixLogger, since);
        JsonApiResponse listOfItems = method.apply(params);
        getLogger().trace("{} : retrieved data from Autocrypt after {} : {}", prefixLogger, since, listOfItems);

        if (!listOfItems.isSuccess() || listOfItems.getResult().isNull() || !listOfItems.getResult().isArray()) {
            getLogger().error("{} : Could not get retrieve data from Autocrypt.", prefixLogger);
            System.out.println(listOfItems);
            throw new SynchronizationException("Could not get retrieve data from Autocrypt");
        }

        getLogger().debug("{} : successfully retrieved data from Autocrypt after {} : {} ", prefixLogger, since, listOfItems.getResult());

        return listOfItems.getResult().asJsonList();
    }

    public void sendData(IJsonToJsonApiResponse method, Json items) throws SynchronizationException {
        getLogger().trace("{} : sending data to DB-API : method {}, items {}", prefixLogger, method.toString(), items);
        JsonApiResponse response = method.apply(items);
        if (!response.isSuccess()) {
            getLogger().error("{} : Could not send data to DB-API for Autocrypt service.", prefixLogger);
            throw new SynchronizationException(prefixLogger + " : Could not send data to DB-API for Autocrypt service.");
        }
        getLogger().debug("{} : sent data to DB-API : {}", prefixLogger, items);
    }

    public List<Json> retrieveData(IJsonToJsonApiResponse methodDelete, IJsonToJsonApiResponse methodUpsert, OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        getLogger().trace("{} : retrieving data from Autocrypt after {} with limit {} and offset {} : methodDelete {} and methodUpsert {}", prefixLogger, since, limit, offset, methodDelete.toString(), methodUpsert.toString());

        Json params = Json.object();
        if (since != null) {
            params.set(Common.AFTER_UPDATED_DATE, ScanUtils.localTimeToScan(since).toString());
        }
        getLogger().debug("{} : retrieving data to delete from Autocrypt after {}", prefixLogger, since);
        JsonApiResponse listOfItemsDelete = methodDelete.apply(params);
        getLogger().trace("{} : retrieved data to delete from Autocrypt after {} : {}", prefixLogger, since, listOfItemsDelete);

        if (!listOfItemsDelete.isSuccess() || listOfItemsDelete.getResult().isNull() || !listOfItemsDelete.getResult().isArray()) {
            getLogger().error("{} : Could not get retrieve data to delete from Autocrypt.", prefixLogger);
            System.out.println(listOfItemsDelete);
            throw new SynchronizationException("Could not get retrieve data to delete from Autocrypt");
        }


        getLogger().debug("{} : retrieving data to upsert from Autocrypt after {}", prefixLogger, since);
        JsonApiResponse listOfItemsUpsert = methodUpsert.apply(params);
        getLogger().trace("{} : retrieved data to upsert from Autocrypt after {} : {}", prefixLogger, since, listOfItemsUpsert);

        if (!listOfItemsUpsert.isSuccess() || listOfItemsUpsert.getResult().isNull() || !listOfItemsUpsert.getResult().isArray()) {
            getLogger().error("{} : Could not get retrieve data to upsert from Autocrypt.", prefixLogger);
            throw new SynchronizationException("Could not get retrieve data to upsert from Autocrypt");
        }

        getLogger().debug("{} : retrieved data_to_delete and data_to_upsert from Autocrypt after {}", prefixLogger, since);

        Json response = Json.object(Common.DELETED, listOfItemsDelete.getResult().asJsonList(), Common.NON_DELETED, listOfItemsUpsert.getResult().asJsonList());
        getLogger().trace("{} : merged data_to_delete and data_to_upsert from Autocrypt after {} : {}", prefixLogger, since, response);

        return Json.array(response).asJsonList();
    }

    public void sendData(IJsonToJsonApiResponse methodDelete, IJsonToJsonApiResponse methodUpsert, List<Json> itemsToUpsertOrDelete) throws SynchronizationException {
        getLogger().trace("{} : sending data to DB-API : with deleteMethod {} and upsertMethod {}. Items to modify {}", prefixLogger, methodDelete.toString(), methodUpsert.toString(), itemsToUpsertOrDelete);
        Json items = itemsToUpsertOrDelete.get(0);
        JsonApiResponse response;
        if (items != null && items.has(Common.DELETED) && items.get(Common.DELETED) != null && items.get(Common.DELETED).isArray()) {
            // deleting items
            getLogger().trace("{} : deleting data to DB-API : {}", prefixLogger, items.get(Common.DELETED));
            response = methodDelete.apply(items.get(Common.DELETED));
            if (!response.isSuccess()) {
                getLogger().error("{} : Could not delete data from DB-API for Autocrypt service : {}", prefixLogger, response.getError());
                throw new SynchronizationException(prefixLogger + " : Could not delete data to DB-API for Autocrypt service.");
            }
            getLogger().debug("{} : deleted data to DB-API : {}", prefixLogger, items);
        }

        if (items != null && items.has(Common.NON_DELETED) && items.get(Common.NON_DELETED) != null && items.get(Common.NON_DELETED).isArray()) {
            // upserting items
            getLogger().trace("{} : sending data to upsert to DB-API : {}", prefixLogger, items.get(Common.NON_DELETED));
            response = methodUpsert.apply(items.get(Common.NON_DELETED));
            if (!response.isSuccess()) {
                getLogger().error("{} : Could not send data to upsert from DB-API for Autocrypt service : {}", prefixLogger, response.getError());
                throw new SynchronizationException(prefixLogger + " : Could not send data to upsert to DB-API for Autocrypt service.");
            }
            getLogger().debug("{} : upserted data to DB-API : {}", prefixLogger, items);
        }
    }

    public OffsetDateTime getLastChangeDate(IVoidToJsonApiResponse method) throws SynchronizationException {
        getLogger().trace("{} : fetching last update time from Dbapi with method {}", prefixLogger, method.toString());
        try {
            getLogger().debug("{} : fetching last update time from Dbapi", prefixLogger);
            JsonApiResponse response = method.apply();
            getLogger().trace("{} : fetched last update time from Dbapi : {}", prefixLogger, response);

            if (!response.isSuccess()) {
                throw new RuntimeException("Could not fetch last update time from Dbapi");
            }

            String lastUpdateDateString = Common.MIN_DATE;
            if (response.getResult() != null && response.getResult().has(Common.VALUE) && response.getResult().get(Common.VALUE).isString()) {
                lastUpdateDateString = response.getResult().get(Common.VALUE).asString().replace("\"", "");
            }
            getLogger().debug("{} : fetched last update time from Dbapi : {}", prefixLogger, lastUpdateDateString);

            getLogger().trace("{} : converted date {} to local : {} ", prefixLogger, lastUpdateDateString, DbapiUtilsForCSLScan.dbapiDateToLocal(lastUpdateDateString));
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
        getLogger().debug("{} : Synchronization was successful", prefixLogger);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
