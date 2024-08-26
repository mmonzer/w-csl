package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.enums.ExportQueryStatus;
import com.ucsl.json.Json;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents an export state from CSL-Scan.
 */
public class ExportQuery {
    private static final Logger logger = LoggerFactory.getLogger(ExportQuery.class);
    private UUID id;
    private ExportQueryStatus status;
    private OffsetDateTime startTime;
    private String filename;
    private String message;
    @Getter
    @Setter
    String xCorrelationId="";  // TODO: need cleaning

    private ExportQuery(UUID id, ExportQueryStatus status, String message, OffsetDateTime startTime, String filename) {
        this.id = id;
        this.status = status;
        this.message = message;
        this.startTime = startTime;
        this.filename = filename;
    }

    public static ExportQuery fromScannerJson(Json json) {
        UUID id;
        ExportQueryStatus status;
        String message;
        OffsetDateTime startTime;
        String filename;

        if (json.has("uuid") && json.get("uuid").isString()) {
            id = UUID.fromString(json.get("uuid").asString());
        } else {
            logger.error("ExportQuery.fromScannerJson: uuid is not a string");
            return null;
        }

        if (json.has("status") && json.get("status").isString()) {
            status = ExportQueryStatus.valueOf(json.get("status").asString());
        } else {
            logger.error("ExportQuery.fromScannerJson: status is not a string");
            return null;
        }

        if (json.has("message")) {
            if (json.get("message").isString()) {
                message = json.get("message").asString();
            } else if (json.get("message").isNull()) {
                message = null;
            } else {
                logger.error("ExportQuery.fromScannerJson: message is not a string");
                return null;
            }
        } else {
            logger.error("ExportQuery.fromScannerJson: message is not a string");
            return null;
        }

        if (json.has("createdAt") && json.get("createdAt").isString()) {
            startTime = OffsetDateTime.parse(json.get("createdAt").asString());
        } else {
            logger.error("ExportQuery.fromScannerJson: startTime is not a string");
            return null;
        }

        if (json.has("filename") && json.get("filename").isString()) {
            filename = json.get("filename").asString();
        } else {
            logger.error("ExportQuery.fromScannerJson: filename is not a string");
            return null;
        }

        return new ExportQuery(id, status, message, startTime, filename);
    }

    public UUID getId() {
        return id;
    }

    public ExportQueryStatus getStatus() {
        return status;
    }

    public ExportQuery setStatus(ExportQueryStatus status) {
        this.status = status;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public String getFilename() {
        return filename;
    }
}
