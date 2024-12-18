package com.csl.util;

import com.csl.logger.LoggerConstants;
import com.csl.logger.LoggerInterfaces;
import org.slf4j.MDC;

import java.util.UUID;

public class CorrelationUtils {
    private CorrelationUtils() {}

    /**
     * Creates an X-Correlation-ID.
     *
     * @return return the created X-Correlation-ID
     */
    public static String createXCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Sets X-Correlation-ID to environment
     *
     * @param xCorrelationId X-Correlation-ID
     */
    public static void setXCorrelationId(String xCorrelationId) {
        MDC.put(LoggerConstants.X_CORRELATION_ID, xCorrelationId);
    }

    /**
     * Sets endpoint to environment
     *
     * @param endpoint endpoint
     */
    public static void setEndpoint(String endpoint) {
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
    }

    /**
     * Creates and sets X-Correlation-ID to environment
     */
    public static String setXCorrelationId() {
        String xCorrelationId = getFormattedXCorrelationId();
        MDC.put(LoggerConstants.X_CORRELATION_ID, xCorrelationId);
        return xCorrelationId;
    }

    /**
     * Creates and sets X-Correlation-ID to environment
     */
    public static void setLoggingVariables() {
        setXCorrelationId();
        setEndpoint("??");
    }
    /**
     * Formats the XCorrelationId with the initializer service.
     * @param uuid unique identifier for the XCorrelationId
     * @param initializerService action initializer service. For example, periodic synchronizations.
     * @return a new formated X-Correlation-ID
     */
    public static String getFormattedXCorrelationId(String uuid, String initializerService) {
        return uuid+"("+initializerService+")";
    }

    /**
     * Formats the XCorrelationId with the initializer service.
     * @param uuid unique identifier for the XCorrelationId
     * @return a new formated X-Correlation-ID
     */
    public static String getFormattedXCorrelationId(String uuid) {
        return getFormattedXCorrelationId(uuid, LoggerInterfaces.CSL_CLIENT.toString());
    }

    /**
     * Formats the XCorrelationId with the initializer service.
     * @return a new formated X-Correlation-ID
     */
    public static String getFormattedXCorrelationId() {
        return getFormattedXCorrelationId(createXCorrelationId(), LoggerInterfaces.CSL_CLIENT.toString());
    }
}
