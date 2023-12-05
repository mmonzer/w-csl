package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.ScanUtils;
import com.csl.interfaces.models.IDbapiSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.csl.intercom.cslscan.enums.MicrosoftKBField;
import com.ucsl.json.Json;

import java.time.OffsetDateTime;

public class MicrosoftKB implements IDbapiSerializable {
    private String mongoEntityId;
    private String deviceId;
    private String discoveryConnectionId;
    private OffsetDateTime discoveredDate;
    private boolean isDeleted;
    private String kbNumber;
    private String installedDate;

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftKB.class);

    private MicrosoftKB(String mongoEntityId, String deviceId, String discoveryConnectionId, OffsetDateTime discoveredDate, boolean isDeleted, String kbNumber, String installedDate) {
        this.mongoEntityId = mongoEntityId;
        this.deviceId = deviceId;
        this.discoveryConnectionId = discoveryConnectionId;
        this.discoveredDate = discoveredDate;
        this.isDeleted = isDeleted;
        this.kbNumber = kbNumber;
        this.installedDate = installedDate;
    }

    public static MicrosoftKB fromScannerJson(Json json) {
        try {
            logger.trace("Parsing MicrosoftKB from scanner json: {}", json);
            String mongoEntityId = json.get(MicrosoftKBField.MONGO_ENTITY_ID.scanName()).asString();
            String deviceId = json.get(MicrosoftKBField.DEVICE_ID.scanName()).asString();
            String discoveryConnectionId = json.get(MicrosoftKBField.DISCOVERY_CONNECTION_ID.scanName()).asString();
//            OffsetDateTime discoveredDate = OffsetDateTime.parse(json.get(MicrosoftKBField.DISCOVERED_DATE.scanName()).asString());
            OffsetDateTime discoveredDate = ScanUtils.getDateFieldFromJson(json, MicrosoftKBField.DISCOVERED_DATE.scanName());
            boolean isDeleted = json.get(MicrosoftKBField.IS_DELETED.scanName()).asBoolean();
            String kbNumber = json.get(MicrosoftKBField.KB_NUMBER.scanName()).asString();
            String installedDate = json.get(MicrosoftKBField.INSTALLED_DATE.scanName()).asString();

            return new MicrosoftKB(mongoEntityId, deviceId, discoveryConnectionId, discoveredDate, isDeleted, kbNumber, installedDate);
        } catch (Throwable e) {
            logger.warn("Failed to parse MicrosoftKB from scanner json", e);
            logger.debug("Failed to parse MicrosoftKB from scanner json: {}", json);
            return null;
        }
    }

    public Json serializeForDbapi() {
        Json serialized = Json.object(
                MicrosoftKBField.MONGO_ENTITY_ID.dbapiName(), mongoEntityId,
                MicrosoftKBField.DEVICE_ID.dbapiName(), deviceId,
                MicrosoftKBField.DISCOVERY_CONNECTION_ID.dbapiName(), discoveryConnectionId,
                MicrosoftKBField.DISCOVERED_DATE.dbapiName(), discoveredDate.toString(),
                MicrosoftKBField.IS_DELETED.dbapiName(), isDeleted,
                MicrosoftKBField.KB_NUMBER.dbapiName(), kbNumber,
                MicrosoftKBField.INSTALLED_DATE.dbapiName(), installedDate
        );
        logger.trace("Serialized MicrosoftKB for dbapi: {}", serialized);
        return serialized;
    }

    public String getMongoEntityId() {
        return mongoEntityId;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public OffsetDateTime getDiscoveredDate() {
        return discoveredDate;
    }
}
