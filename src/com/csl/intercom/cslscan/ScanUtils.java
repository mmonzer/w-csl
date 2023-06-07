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
    public static OffsetDateTime localTimeToScan(OffsetDateTime localDateTime) {
        return localDateTime;
    }

    /**
     * Translate time received from CSL-Scan in UTC, as used locally.
     *
     * @param scanDateTime The parsed date as received from CSL-Scan.
     * @return The same date in UTC format.
     */
    public static OffsetDateTime scanTimeToLocal(OffsetDateTime scanDateTime) {
        return scanDateTime;
//        return scanDateTime.atOffset(ZoneOffset.UTC);
    }

    /**
     * Extract a CPE Item's modification date from its {@link Json} form
     *
     * @param cpeItem The CPE Item we want to read
     * @return A {@link LocalDateTime} with the last modification date of the CPE Item
     */
    public static OffsetDateTime getCpeItemDateTime(Json cpeItem) {
        String cpeItemDate = JsonUtil.getStringFromJson(cpeItem, "updatedAt", null);
        return cpeItemDate == null ? null : scanTimeToLocal(OffsetDateTime.parse(cpeItemDate));
    }

    /**
     * Extract the scan's progress of a notification sent by CSL-Scan.
     *
     * @param notification The notification sent by CSL-Scan.
     * @return The progress of the scan, between 0 and 1, defaulting to 0.
     */
    public static double getProgressFromScanNotification(Json notification) {
        double progress = JsonUtil.getDoubleFromJson(notification, "completion", 0.0);
        if (progress < 0) {
            progress = 0.0;
        }
        if (progress > 1) {
            progress = 1.0;
        }
        return progress;
    }
}
