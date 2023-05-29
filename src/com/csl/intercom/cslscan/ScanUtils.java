package com.csl.intercom.cslscan;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Class containing static functions for various tasks related to CSL-Scan.
 */
public class ScanUtils {
    /**
     * Translate local date to UTC, as used by CSL-Scan.
     *
     * @param localDateTime The local date time.
     * @return The same date time in UTC.
     */
    public static LocalDateTime localTimeToScan(OffsetDateTime localDateTime) {
        if (localDateTime == null) return null;
//        OffsetDateTime utcDateTime = localDateTime.atOffset(ZoneOffset.UTC);
        return localDateTime.atZoneSameInstant(ScanConstants.zoneId).toLocalDateTime();
    }

    /**
     * Translate time received from CSL-Scan in UTC, as used locally.
     *
     * @param localDateTime The parsed date as received from CSL-Scan.
     * @return The same date in UTC format.
     */
    public static OffsetDateTime scanTimeToLocal(LocalDateTime localDateTime) {
        return localDateTime.atZone(ScanConstants.zoneId).toInstant().atOffset(ZoneOffset.UTC);
//        return localDateTime.atOffset(ZoneOffset.UTC);
    }

    /**
     * Extract a CPE Item's modification date from its {@link Json} form
     *
     * @param cpeItem The CPE Item we want to read
     * @return A {@link LocalDateTime} with the last modification date of the CPE Item
     */
    public static OffsetDateTime getCpeItemDateTime(Json cpeItem) {
        String cpeItemDate = JsonUtil.getStringFromJson(cpeItem, "updatedAt", null);
        return cpeItemDate == null ? null : scanTimeToLocal(LocalDateTime.parse(cpeItemDate));
    }
}
