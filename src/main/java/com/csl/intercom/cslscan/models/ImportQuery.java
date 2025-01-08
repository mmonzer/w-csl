package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.enums.ImportQueryStatus;
import com.csl.logger.LoggerConstants;
import com.ucsl.json.Json;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Represents an import state from CSL-Scan.
 */
public class ImportQuery {
    private static final Logger logger = LoggerFactory.getLogger(ImportQuery.class);
    public static final String STATUS = "status";
    public static final String MESSAGE = "message";
    public static final String CREATED_AT = "createdAt";
    @Getter
    private final UUID id;
    @Getter
    private ImportQueryStatus importQueryStatus;
    String importQueryMessage;
    @Getter
    String xCorrelationId="";  // TODO: need cleaning

    private ImportQuery(UUID id, ImportQueryStatus status, String message) {
        this.id = id;
        this.importQueryStatus = status;
        this.importQueryMessage = message;
        xCorrelationId = MDC.get(LoggerConstants.X_CORRELATION_ID);
    }

    public static ImportQuery fromScannerJson(Json json) {
        UUID id;
        ImportQueryStatus status;
        String message;

        if (json.has("uuid") && json.get("uuid").isString()) {
            id = UUID.fromString(json.get("uuid").asString());
        } else {
            logger.error("ImportQuery.fromScannerJson: uuid is not a string");
            return null;
        }

        if (json.has(STATUS) && json.get(STATUS).isString()) {
            status = ImportQueryStatus.valueOf(json.get(STATUS).asString());
        } else {
            logger.error("ImportQuery.fromScannerJson: status is not a string");
            return null;
        }

        if (json.has(MESSAGE)) {
            if (json.get(MESSAGE).isString()) {
                message = json.get(MESSAGE).asString();
            } else if (json.get(MESSAGE).isNull()) {
                message = null;
            } else {
                logger.error("ImportQuery.fromScannerJson: message is not a string");
                return null;
            }
        } else {
            logger.error("ImportQuery.fromScannerJson: message is not a string");
            return null;
        }

        if (!json.has(CREATED_AT) || !json.get(CREATED_AT).isString()) {
            logger.error("ImportQuery.fromScannerJson: startTime is not a string");
            return null;
        }

        return new ImportQuery(id, status, message);
    }

    public ImportQuery setImportQueryStatus(ImportQueryStatus importQueryStatus) {
        this.importQueryStatus = importQueryStatus;
        return this;
    }
}
