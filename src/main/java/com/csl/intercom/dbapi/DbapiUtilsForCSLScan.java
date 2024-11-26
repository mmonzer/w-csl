package com.csl.intercom.dbapi;

import com.csl.intercom.dbapi.models.Connection;
import com.csl.intercom.dbapi.models.Device;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DbapiUtilsForCSLScan {
    /**
     * Translate a date from DB-API's datetime format to LocalDateTime.
     *
     * @param dateTime A serialized date as received from DB-API.
     * @return The {@link LocalDateTime} corresponding to the date provided by DB-API.
     */
    public static OffsetDateTime dbapiDateToLocal(String dateTime) {
        return dbapiDateToLocal(OffsetDateTime.parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    /**
     * Translate a date from DB-API's datetime format to LocalDateTime.
     *
     * @param dateTime An {@link OffsetDateTime} date as received from DB-API.
     * @return The {@link LocalDateTime} corresponding to the date provided by DB-API.
     */
    public static OffsetDateTime dbapiDateToLocal(OffsetDateTime dateTime) {
        return dateTime;
    }

    /**
     * Translate local date to UTC, as used by CSL-Scan.
     *
     * @param localDateTime The local date time.
     * @return The same date time in UTC.
     */
    public static OffsetDateTime localDateToDbapi(OffsetDateTime localDateTime) {
        return localDateTime;
    }

    /**
     * Get a connection from its id from a list of connection.
     * @param connections The list of connections to filter.
     * @param id The id we seek.
     * @return The connection with the corresponding id if any, null otherwise.
     */
    public static Connection getConnectionById(List<Connection> connections, String id) {
        for (Connection connection : connections) {
            if (connection.getId() != null) {
                if (connection.getId().equals(id)) {
                    return connection;
                }
            }
        }
        return null;
    }

    public static Device getDeviceById(List<Device> devices, String id) {
        for (Device device: devices) {
            if (device.getId().equals(id)) {
                return device;
            }
        }
        return null;
    }
}
