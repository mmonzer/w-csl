package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.enums.ExportQueryStatus;
import com.csl.logger.LoggerConstants;
import com.ucsl.json.Json;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.OffsetDateTime;
import java.util.UUID;

import static com.csl.util.FileUtils.FILENAME;
import static com.ucsl.json.JsonUtil.getValueStringOrNull;

/**
 * Represents an export state from CSL-Scan.
 */
public class ExportQuery {
    private static final Logger logger = LoggerFactory.getLogger(ExportQuery.class);
    public static final String STATUS = "status";
    public static final String MESSAGE = "message";
    public static final String CREATED_AT = "createdAt";
    public static final String UUID = "uuid";
    private UUID id;
    private ExportQueryStatus queryStatus;
    private OffsetDateTime startTime;
    private String filename;
    private String content;
    @Getter
    String xCorrelationId = null;  // TODO: need cleaning

    private ExportQuery(UUID id, ExportQueryStatus queryStatus, String content, OffsetDateTime startTime, String filename) {
        this.id = id;
        this.queryStatus = queryStatus;
        this.content = content;
        this.startTime = startTime;
        this.filename = filename;
        xCorrelationId = MDC.get(LoggerConstants.X_CORRELATION_ID);
    }

    public static ExportQuery fromScannerJson(Json json) {
        ExportQueryStatus status;
        String message;
        OffsetDateTime startTime;
        String filename;

        String  id = getValueStringOrNull(json, UUID);
        if (id==null) {
            logger.error("ExportQuery.fromScannerJson: uuid is not a string");
            return null;
        }

        if (json.has(STATUS) && json.get(STATUS).isString()) {
            status = ExportQueryStatus.valueOf(json.get(STATUS).asString());
        } else {
            logger.error("ExportQuery.fromScannerJson: status is not a string");
            return null;
        }

        if (json.has(MESSAGE)) {
            if (json.get(MESSAGE).isString()) {
                message = json.get(MESSAGE).asString();
            } else if (json.get(MESSAGE).isNull()) {
                message = null;
            } else {
                logger.error("ExportQuery.fromScannerJson: message is not a string");
                return null;
            }
        } else {
            logger.error("ExportQuery.fromScannerJson: message is not a string");
            return null;
        }

        if (json.has(CREATED_AT) && json.get(CREATED_AT).isString()) {
            startTime = OffsetDateTime.parse(json.get(CREATED_AT).asString());
        } else {
            logger.error("ExportQuery.fromScannerJson: startTime is not a string");
            return null;
        }

        if (json.has(FILENAME) && json.get(FILENAME).isString()) {
            filename = json.get(FILENAME).asString();
        } else {
            logger.error("ExportQuery.fromScannerJson: filename is not a string");
            return null;
        }

        return new ExportQuery(java.util.UUID.fromString(id), status, message, startTime, filename);
    }

    public UUID getId() {
        return id;
    }

    public ExportQueryStatus getStatus() {
        return queryStatus;
    }

    public ExportQuery setStatus(ExportQueryStatus status) {
        this.queryStatus = status;
        return this;
    }

    public String getMessage() {
        return content;
    }

    public String getFilename() {
        return filename;
    }
}
