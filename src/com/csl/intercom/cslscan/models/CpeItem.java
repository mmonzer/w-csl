package com.csl.intercom.cslscan.models;

import com.csl.core.CSLContext;
import com.csl.intercom.cslscan.ScanUtils;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Class to retain the required fields of a CPE Items.
 */
public class CpeItem {
    private Json cpeData;
    private OffsetDateTime discoveredDate;
    private String mongoEntityId;
    private String deviceId;
    private boolean isDeleted;
    static private ZoneId zoneId = CSLContext.instance.getZoneId();

    // The fields to include in the cpeData array
    private static Set<String> dataFields = new HashSet<>(10) {{
        add("part");
        add("vendor");
        add("product");
        add("version");
        add("update");
        add("edition");
        add("lang");
        add("softwareEdition");
        add("targetSoftware");
        add("others");
    }};

    /**
     * Create an object from the direct data. Should not be used directly, thus is private.
     * Create an object with a factory method instead.
     *
     * @param cpeData The {@link Json} object with the data. Will be sanitized before being included.
     * @param discoveredDate The date of discovery.
     * @param mongoEntityId The uuid of the CPI Item in CSL-Scan's Mongodb.
     * @param deviceId The uuid of the device associated with this CPE Item.
     */
    private CpeItem(Json cpeData, OffsetDateTime discoveredDate, String mongoEntityId, String deviceId, boolean isDeleted) {
        this.cpeData = Json.object();
        this.discoveredDate = discoveredDate;
        this.mongoEntityId = mongoEntityId;
        this.deviceId = deviceId;
        this.isDeleted = isDeleted;

        for (String field: dataFields) {
            this.cpeData.set(field, cpeData.get(field));
        }
    }

    /**
     * Create a CpeItem based on the {@link Json} received from CSL-Scan.
     *
     * @param data The {@link Json} received from CSL-Scan.
     * @return The newly created CpeItem.
     * @throws IllegalArgumentException if mandatory fields are missing in the provided data.
     */
    public static CpeItem fromScanCpeItem(Json data) throws IllegalArgumentException {
        OffsetDateTime discoveredDate;
        String mongoEntityId;
        String deviceId;
        boolean isDeleted;

        try {
            discoveredDate = ScanUtils.getCpeItemDateTime(data);
            mongoEntityId = data.get("uuid").asString();
            deviceId = data.get("entityUuid").asString();
            isDeleted = JsonUtil.getBooleanFromJson(data, "deleted", false);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("The fields 'updatedAt', 'uuid' and 'entityUuid' are required to build a CPE Item");
        }

        return new CpeItem(data, discoveredDate, mongoEntityId, deviceId, isDeleted);
    }

    public Json getCpeData() {
        return cpeData;
    }

    public OffsetDateTime getDiscoveredDate() {
        return discoveredDate;
    }

    public String getMongoEntityId() {
        return mongoEntityId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public boolean isDeleted() {
        return isDeleted;
    }
}
