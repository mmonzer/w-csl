package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.dbapi.DbapiUtilsForCSLScan;
import com.csl.interfaces.models.IDbapiSerializable;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Class to retain the required fields of a CPE Items.
 */
public class CpeItem implements IDbapiSerializable {
    // The fields to include in the cpeData array
    private static final Logger logger = LoggerFactory.getLogger(CpeItem.class);
    private static final Set<String> dataFields = new HashSet<>(10) {{
        add("part");
        add("vendor");
        add("product");
        add("version");
        add("update");
        add("edition");
        add("lang");
        add("softwareEdition");
        add("targetSoftware");
        add("targetHardware");
        add("others");
    }};
    private final Json cpeData;
    @Getter
    private final OffsetDateTime discoveredDate;
    @Getter
    private final String mongoEntityId;
    @Getter
    private final String deviceId;
    private final boolean isDeleted;
    private final boolean isMain;
    private final int discoveryConnectionId;

    /**
     * Create an object from the direct data. Should not be used directly, thus is private.
     * Create an object with a factory method instead.
     *
     * @param cpeData        The {@link Json} object with the data. Will be sanitized before being included.
     * @param discoveredDate The date of discovery.
     * @param mongoEntityId  The uuid of the CPI Item in CSL-Scan's Mongodb.
     * @param deviceId       The uuid of the device associated with this CPE Item.
     */
    private CpeItem(Json cpeData, OffsetDateTime discoveredDate, String mongoEntityId, String deviceId, boolean isDeleted, boolean isMain, int discoveryConnectionId) {
        this.cpeData = Json.object();
        this.discoveredDate = discoveredDate;
        this.mongoEntityId = mongoEntityId;
        this.deviceId = deviceId;
        this.isDeleted = isDeleted;
        this.isMain = isMain;
        this.discoveryConnectionId = discoveryConnectionId;

        for (String field : dataFields) {
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
    public static CpeItem fromScannerJson(Json data) throws IllegalArgumentException {
        OffsetDateTime discoveredDate = null;
        String mongoEntityId = null;
        String deviceId = null;
        boolean isDeleted = false;
        boolean isMain = false;
        int discoveryConnectionId = 0;

        try {
            discoveredDate = ScanUtils.getDateFieldFromJson(data, "updatedAt");
            mongoEntityId = data.get("uuid").asString();
            deviceId = data.get("entityUuid").asString();
            isDeleted = JsonUtil.getBooleanFromJson(data, "deleted", false);
            Json isMainJson = data.get("isMain");
            // isMain is optional, so we need to check if it is null, and if it is, leave it as false
            if (isMainJson != null && !isMainJson.isNull()) {
                isMain = isMainJson.asBoolean();
            }

            // Get the connection's id if it is valid
            try {
                discoveryConnectionId = Integer.parseInt(JsonUtil.getStringFromJson(data, "connectionInfoUuid", "0"));
            } catch (NumberFormatException | UnsupportedOperationException e) {
                discoveryConnectionId = -1;
            }
        } catch (NullPointerException e) {
            // If any of the fields are missing, throw an exception
            logger.error("The fields 'updatedAt', 'uuid' and 'entityUuid' are required to build a CPE Item", e);
            throw new IllegalArgumentException("The fields 'updatedAt', 'uuid' and 'entityUuid' are required to build a CPE Item");
        }

        return new CpeItem(data, discoveredDate, mongoEntityId, deviceId, isDeleted, isMain, discoveryConnectionId);
    }

    public Json getCpeData() {
        return cpeData;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public boolean isMain() {
        return isMain;
    }

    /**
     * Serialize the CpeItem to a {@link Json} object that can be sent to CSL-Scan.
     *
     * @return The {@link Json} object that can be sent to CSL-Scan.
     */
    public Json serializeForDbapi() {
        Json serialization = Json.object(
                "cpe_data", this.cpeData,
                "discovered_date", DbapiUtilsForCSLScan.localDateToDbapi(this.discoveredDate).toString(),
                "mongo_entity_id", this.mongoEntityId,
                "is_main_configuration", this.isMain
        );

        // Add the connection's id if it is valid
        if (this.discoveryConnectionId > 0) {
            serialization.set("connection_id", this.discoveryConnectionId);
        }
        return serialization;
    }
}
