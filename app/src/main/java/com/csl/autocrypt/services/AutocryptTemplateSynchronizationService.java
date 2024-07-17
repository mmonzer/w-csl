package com.csl.autocrypt.services;

import com.csl.autocrypt.AutoCrypt;
import com.csl.autocrypt.enums.AutocryptConstants.Common;
import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.dbapi.DbapiUtilsForCSLScan;
import com.csl.intercom.services.PaginatedSynchronizationService;
import com.csl.intercom.services.exceptions.SynchronizationException;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import org.slf4j.Logger;

import java.time.OffsetDateTime;
import java.util.List;

public abstract class AutocryptTemplateSynchronizationService extends PaginatedSynchronizationService<Json> {
    protected final AutoCrypt autocrypt;
    protected final String prefixLogger;
    protected Logger logger;

    public AutocryptTemplateSynchronizationService(AutoCrypt autocrypt, String prefixLogger) {
        this.autocrypt = autocrypt;
        this.prefixLogger = prefixLogger;
    }

    public List<Json> retrieveData(IJsonToJsonApiResponse method, OffsetDateTime since, int limit, int offset) throws SynchronizationException {
        Json params = Json.object();
        if (since != null) {
            params.set(Common.AFTER_UPDATED_DATE, ScanUtils.localTimeToScan(since).toString());
        }
        JsonApiResponse listIssuers = method.apply(params);

        if (!listIssuers.isSuccess() || listIssuers.getResult().isNull() || !listIssuers.getResult().isArray()) {
            getLogger().error("{} : Could not get retrieve data from Autocrypt.", prefixLogger);
            throw new SynchronizationException("Could not get retrieve data from Autocrypt");
        }

        return listIssuers.getResult().asJsonList();
    }

    public void sendData(IJsonToJsonApiResponse method, Json items) throws SynchronizationException {
        JsonApiResponse response = method.apply(items);
        if (!response.isSuccess()) {
            getLogger().error("{} : Could not send data to DB-API for Autocrypt service.", prefixLogger);
            throw new SynchronizationException(prefixLogger+" : Could not send data to DB-API for Autocrypt service.");
        }
    }

    public OffsetDateTime getLastChangeDate(IVoidToJsonApiResponse method) throws SynchronizationException {
        try {
            JsonApiResponse response = method.apply();

            if (!response.isSuccess() || response.getResult().has(Common.VALUE)) {
                throw new RuntimeException("Could not fetch last update time from Dbapi");
            }
            String lastUpdateDateString = response.getResult().get(Common.VALUE).asString();

            return DbapiUtilsForCSLScan.dbapiDateToLocal(lastUpdateDateString);
        } catch (Exception e) {
            getLogger().error("{} : Could not get last update date from DB-API for Autocrypt service.", prefixLogger);
            throw new SynchronizationException(prefixLogger+" : Could not get last update date from DB-API for Autocrypt service", e);
        }
    }

    @Override
    public boolean shouldSendEmptyData() {
        return true;
    }

    @Override
    public void syncData() throws SynchronizationException {
        super.syncData();
        getLogger().info("{} : Synchronization was successful", prefixLogger);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
