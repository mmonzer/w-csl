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
     * Shows partially the password
     *
     * @param password password to hide
     * @return hidden password
     */
     public static String hide(String password) {
         String defaultPassword = "********";
         if (password==null) {
             return "null";
         }
         if (password.length()>3) {
             return password.substring(0,3)+defaultPassword.substring(0,5);
         } else {
             return password+defaultPassword.substring(0,8-password.length());
         }
     }
}
