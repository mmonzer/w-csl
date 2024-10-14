package com.csl.logger;

import org.slf4j.Logger;
import org.slf4j.MDC;

public class LoggerUtils {
    public static void log(Logger logger, String level, String message) {
        switch (level.toLowerCase()) {
            case "trace":
                logger.trace(message);
                break;
            case "debug":
                logger.debug(message);
                break;
            case "info":
                logger.info(message);
                break;
            case "warn":
                logger.warn(message);
                break;
            case "error":
                logger.error(message);
                break;
            default:
                break;
        }
    }

    public static void traceAlertReceived(Logger logger, String ip, Integer port, String endpoint, String protocol) {
        MDC.put(LoggerConstants.IP_SRC, ip);
        MDC.put(LoggerConstants.PORT_SRC, port.toString());
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        logger.trace("Alert received");
        MDC.remove(LoggerConstants.IP_SRC);
        MDC.remove(LoggerConstants.PORT_SRC);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.PROTOCOL);
    }

    /**
     * Sets the applicative log label on
     */
    public static void addApplicativeLog() {
        MDC.put(LoggerConstants.LOG_TYPE, LoggerConstants.APPLICATIVE);
    }

    /**
     * Sets the applicative log label off
     */
    public static void removeApplicativeLog() {
        MDC.remove(LoggerConstants.LOG_TYPE);
    }

}
