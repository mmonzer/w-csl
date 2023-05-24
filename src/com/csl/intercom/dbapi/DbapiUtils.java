package com.csl.intercom.dbapi;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class DbapiUtils {
    /**
     * Translate a date from DB-API's datetime format to LocalDateTime.
     *
     * @param dateTime A serialized date as received from DB-API.
     * @return The {@link LocalDateTime} corresponding to the date provided by DB-API.
     */
    static OffsetDateTime dbapiDateToLocal(String dateTime) {
        return dbapiDateToLocal(OffsetDateTime.parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    /**
     * Translate a date from DB-API's datetime format to LocalDateTime.
     *
     * @param dateTime An {@link OffsetDateTime} date as received from DB-API.
     * @return The {@link LocalDateTime} corresponding to the date provided by DB-API.
     */
    private static OffsetDateTime dbapiDateToLocal(OffsetDateTime dateTime) {
        return dateTime;
    }

    /**
     * Translate local date to UTC, as used by CSL-Scan.
     *
     * @param localDateTime The local date time.
     * @return The same date time in UTC.
     */
    static OffsetDateTime localDateToDbapi(OffsetDateTime localDateTime) {
        return localDateTime;
    }
}
