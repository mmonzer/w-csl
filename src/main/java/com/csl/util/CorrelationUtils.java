package com.csl.util;

import com.csl.logger.LoggerConstants;
import com.csl.logger.LoggerInterfaces;
import org.slf4j.MDC;

import java.util.UUID;

public class CorrelationUtils {
    /**
     * Creates an X-Correlation-ID.
     * @return return the created X-Correlation-ID
     */
    public static String createXCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Sets X-Correlation-ID to environment
     * @param xCorrelationId X-Correlation-ID
     */
    public static void setXCorrelationId(String xCorrelationId) {
        MDC.put(LoggerConstants.X_CORRELATION_ID, xCorrelationId);
    }

    /**
     * Sets the initializer service to the logging variables
     * @param service initializer service
     */
    public static void setInitializerService(String service) {
        MDC.put(LoggerConstants.INIT_SERVICE, service);
    }

    /**
     * Sets endpoint to environment
     * @param endpoint endpoint
     */
    public static void setEndpoint(String endpoint) {
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
    }

    /**
     * Creates and sets X-Correlation-ID to environment
     */
    public static String setXCorrelationId() {
        String xCorrelationId = createXCorrelationId();
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


}
