package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.enums.ImportQueryStatus;
import com.csl.logger.LoggerConstants;
import com.ucsl.json.Json;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents an import state from CSL-Scan.
 */
public class ImportQuery {
    private static final Logger logger = LoggerFactory.getLogger(ImportQuery.class);
    private UUID id;
    private ImportQueryStatus status;
    private OffsetDateTime startTime;
    String message;
    @Getter
    String xCorrelationId="";  // TODO: need cleaning

    private ImportQuery(UUID id, ImportQueryStatus status, String message) {
        this.id = id;
        this.status = status;
        this.message = message;
        xCorrelationId = MDC.get(LoggerConstants.X_CORRELATION_ID);
    }

    public static ImportQuery fromScannerJson(Json json) {
        UUID id;
        ImportQueryStatus status;
        String message;
        OffsetDateTime startTime;

        if (json.has("uuid") && json.get("uuid").isString()) {
            id = UUID.fromString(json.get("uuid").asString());
        } else {
            logger.error("ImportQuery.fromScannerJson: uuid is not a string");
            return null;
        }

        if (json.has("status") && json.get("status").isString()) {
            status = ImportQueryStatus.valueOf(json.get("status").asString());
        } else {
            logger.error("ImportQuery.fromScannerJson: status is not a string");
            return null;
        }

        if (json.has("message")) {
            if (json.get("message").isString()) {
                message = json.get("message").asString();
            } else if (json.get("message").isNull()) {
                message = null;
            } else {
                logger.error("ImportQuery.fromScannerJson: message is not a string");
                return null;
            }
        } else {
            logger.error("ImportQuery.fromScannerJson: message is not a string");
            return null;
        }

        if (json.has("createdAt") && json.get("createdAt").isString()) {
            startTime = OffsetDateTime.parse(json.get("createdAt").asString());
        } else {
            logger.error("ImportQuery.fromScannerJson: startTime is not a string");
            return null;
        }

        return new ImportQuery(id, status, message);
    }

    public UUID getId() {
        return id;
    }

    public ImportQueryStatus getStatus() {
        return status;
    }

    public ImportQuery setStatus(ImportQueryStatus status) {
        this.status = status;
        return this;
    }

    public String getMessage() {
        return message;
    }
}
