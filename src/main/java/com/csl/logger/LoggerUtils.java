package com.csl.logger;

import org.slf4j.Logger;
import org.slf4j.MDC;

public class LoggerUtils {

    /**
     * Logs for inbound responses in the APIHandler
     *
     * @param logger     logger to log
     * @param ip         ip source of the response
     * @param port       port source of the response
     * @param method     method of the response
     * @param endpoint   endpoint of the response
     * @param protocol   protocol of the response
     * @param statusCode HTTP code of the response
     */
    public static void infoInboundResponse(Logger logger, String ip, Integer port, String method, String endpoint, String protocol, Integer statusCode) {
        MDC.put(LoggerConstants.IP_SRC, ip);
        MDC.put(LoggerConstants.PORT_SRC, port.toString());
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        MDC.put(LoggerConstants.STATUS_CODE, statusCode.toString());
        logger.info("HTTP response received.");
        MDC.remove(LoggerConstants.IP_SRC);
        MDC.remove(LoggerConstants.PORT_SRC);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
        MDC.remove(LoggerConstants.STATUS_CODE);
    }

    /**
     * Logs for outbound requests in the APIHandler
     *
     * @param logger   logger for logging the request
     * @param ip       ip destination of the request
     * @param port     port destination of the request
     * @param method   method of the request
     * @param endpoint endpoint of the request
     * @param protocol protocol of the request
     */
    public static void infoOutboundRequest(Logger logger, String ip, Integer port, String method, String endpoint, String protocol) {
        MDC.put(LoggerConstants.IP_DST, ip);
        MDC.put(LoggerConstants.PORT_DST, "" + port);
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        logger.info("HTTP request sent.");
        MDC.remove(LoggerConstants.IP_DST);
        MDC.remove(LoggerConstants.PORT_DST);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
    }

    /**
     * Logs for outbound requests in the APIHandler
     *
     * @param logger   logger for logging the request
     * @param ip       ip destination of the request
     * @param port     port destination of the request
     * @param method   method of the request
     * @param endpoint endpoint of the request
     * @param protocol protocol of the request
     */
    public static void infoInboundRequest(Logger logger, String ip, Integer port, String method, String endpoint, String protocol) {
        MDC.put(LoggerConstants.IP_DST, ip);
        MDC.put(LoggerConstants.PORT_DST, "" + port);
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        logger.info("HTTP request received");
        MDC.remove(LoggerConstants.IP_DST);
        MDC.remove(LoggerConstants.PORT_DST);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
    }

    /**
     * Logs for inbound responses in the APIHandler
     *
     * @param logger     logger to log
     * @param ip         ip source of the response
     * @param port       port source of the response
     * @param method     method of the response
     * @param endpoint   endpoint of the response
     * @param protocol   protocol of the response
     * @param statusCode HTTP code of the response
     */
    public static void infoOutboundResponse(Logger logger, String ip, Integer port, String method, String endpoint, String protocol, Integer statusCode) {
        MDC.put(LoggerConstants.IP_SRC, ip);
        MDC.put(LoggerConstants.PORT_SRC, port.toString());
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        MDC.put(LoggerConstants.STATUS_CODE, statusCode.toString());
        logger.info("HTTP response sent.");
        MDC.remove(LoggerConstants.IP_SRC);
        MDC.remove(LoggerConstants.PORT_SRC);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
        MDC.remove(LoggerConstants.STATUS_CODE);
    }

    /**
     * Logs for inbound responses in the APIHandler
     *
     * @param logger     logger to log
     * @param ip         ip source of the response
     * @param port       port source of the response
     * @param method     method of the response
     * @param endpoint   endpoint of the response
     * @param protocol   protocol of the response
     * @param statusCode HTTP code of the response
     */
    public static void infoInboundResponse(CustomLogger logger, String ip, Integer port, String method, String endpoint, String protocol, Integer statusCode) {
        MDC.put(LoggerConstants.IP_SRC, ip);
        MDC.put(LoggerConstants.PORT_SRC, port.toString());
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        MDC.put(LoggerConstants.STATUS_CODE, statusCode.toString());
        logger.info("HTTP response received.");
        MDC.remove(LoggerConstants.IP_SRC);
        MDC.remove(LoggerConstants.PORT_SRC);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
        MDC.remove(LoggerConstants.STATUS_CODE);
    }

    /**
     * Logs for outbound requests in the APIHandler
     *
     * @param logger   logger for logging the request
     * @param ip       ip destination of the request
     * @param port     port destination of the request
     * @param method   method of the request
     * @param endpoint endpoint of the request
     * @param protocol protocol of the request
     */
    public static void infoOutboundRequest(CustomLogger logger, String ip, Integer port, String method, String endpoint, String protocol) {
        MDC.put(LoggerConstants.IP_DST, ip);
        MDC.put(LoggerConstants.PORT_DST, "" + port);
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        logger.info("HTTP request sent.");
        MDC.remove(LoggerConstants.IP_DST);
        MDC.remove(LoggerConstants.PORT_DST);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
    }

    /**
     * Logs for outbound requests in the APIHandler
     *
     * @param logger   logger for logging the request
     * @param ip       ip destination of the request
     * @param port     port destination of the request
     * @param method   method of the request
     * @param endpoint endpoint of the request
     * @param protocol protocol of the request
     */
    public static void infoInboundRequest(CustomLogger logger, String ip, Integer port, String method, String endpoint, String protocol) {
        MDC.put(LoggerConstants.IP_DST, ip);
        MDC.put(LoggerConstants.PORT_DST, "" + port);
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        logger.info("HTTP request received.");
        MDC.remove(LoggerConstants.IP_DST);
        MDC.remove(LoggerConstants.PORT_DST);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
    }

    /**
     * Logs for inbound responses in the APIHandler
     *
     * @param logger     logger to log
     * @param ip         ip source of the response
     * @param port       port source of the response
     * @param method     method of the response
     * @param endpoint   endpoint of the response
     * @param protocol   protocol of the response
     * @param statusCode HTTP code of the response
     */
    public static void infoOutboundResponse(CustomLogger logger, String ip, Integer port, String method, String endpoint, String protocol, Integer statusCode) {
        MDC.put(LoggerConstants.IP_SRC, ip);
        MDC.put(LoggerConstants.PORT_SRC, port.toString());
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        MDC.put(LoggerConstants.STATUS_CODE, statusCode.toString());
        logger.info("HTTP response sent.");
        MDC.remove(LoggerConstants.IP_SRC);
        MDC.remove(LoggerConstants.PORT_SRC);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
        MDC.remove(LoggerConstants.STATUS_CODE);
    }

    /**
     * Logs for inbound responses in the APIHandler
     *
     * @param logger     logger to log
     * @param ip         ip source of the response
     * @param port       port source of the response
     * @param method     method of the response
     * @param endpoint   endpoint of the response
     * @param protocol   protocol of the response
     * @param statusCode HTTP code of the response
     */
    public static void debugInboundResponse(Logger logger, String ip, Integer port, String method, String endpoint, String protocol, Integer statusCode) {
        MDC.put(LoggerConstants.IP_SRC, ip);
        MDC.put(LoggerConstants.PORT_SRC, port.toString());
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        MDC.put(LoggerConstants.STATUS_CODE, statusCode.toString());
        logger.debug("HTTP response received.");
        MDC.remove(LoggerConstants.IP_SRC);
        MDC.remove(LoggerConstants.PORT_SRC);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
        MDC.remove(LoggerConstants.STATUS_CODE);
    }

    /**
     * Logs for outbound requests in the APIHandler
     *
     * @param logger   logger for logging the request
     * @param ip       ip destination of the request
     * @param port     port destination of the request
     * @param method   method of the request
     * @param endpoint endpoint of the request
     * @param protocol protocol of the request
     */
    public static void debugOutboundRequest(Logger logger, String ip, Integer port, String method, String endpoint, String protocol) {
        MDC.put(LoggerConstants.IP_DST, ip);
        MDC.put(LoggerConstants.PORT_DST, "" + port);
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        logger.debug("HTTP request sent.");
        MDC.remove(LoggerConstants.IP_DST);
        MDC.remove(LoggerConstants.PORT_DST);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
    }

    /**
     * Logs for outbound requests in the APIHandler
     *
     * @param logger   logger for logging the request
     * @param ip       ip destination of the request
     * @param port     port destination of the request
     * @param method   method of the request
     * @param endpoint endpoint of the request
     * @param protocol protocol of the request
     */
    public static void debugInboundRequest(Logger logger, String ip, Integer port, String method, String endpoint, String protocol) {
        MDC.put(LoggerConstants.IP_DST, ip);
        MDC.put(LoggerConstants.PORT_DST, "" + port);
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        logger.debug("HTTP request received.");
        MDC.remove(LoggerConstants.IP_DST);
        MDC.remove(LoggerConstants.PORT_DST);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
    }

    /**
     * Logs for inbound responses in the APIHandler
     *
     * @param logger     logger to log
     * @param ip         ip source of the response
     * @param port       port source of the response
     * @param method     method of the response
     * @param endpoint   endpoint of the response
     * @param protocol   protocol of the response
     * @param statusCode HTTP code of the response
     */
    public static void debugOutboundResponse(Logger logger, String ip, Integer port, String method, String endpoint, String protocol, Integer statusCode) {
        MDC.put(LoggerConstants.IP_SRC, ip);
        MDC.put(LoggerConstants.PORT_SRC, port.toString());
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        MDC.put(LoggerConstants.STATUS_CODE, statusCode.toString());
        logger.debug("HTTP response sent.");
        MDC.remove(LoggerConstants.IP_SRC);
        MDC.remove(LoggerConstants.PORT_SRC);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
        MDC.remove(LoggerConstants.STATUS_CODE);
    }

    /**
     * Logs for inbound responses in the APIHandler
     *
     * @param logger     logger to log
     * @param ip         ip source of the response
     * @param port       port source of the response
     * @param method     method of the response
     * @param endpoint   endpoint of the response
     * @param protocol   protocol of the response
     * @param statusCode HTTP code of the response
     */
    public static void debugInboundResponse(CustomLogger logger, String ip, Integer port, String method, String endpoint, String protocol, Integer statusCode) {
        MDC.put(LoggerConstants.IP_SRC, ip);
        MDC.put(LoggerConstants.PORT_SRC, port.toString());
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        MDC.put(LoggerConstants.STATUS_CODE, statusCode.toString());
        logger.debug("HTTP response received.");
        MDC.remove(LoggerConstants.IP_SRC);
        MDC.remove(LoggerConstants.PORT_SRC);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
        MDC.remove(LoggerConstants.STATUS_CODE);
    }

    /**
     * Logs for outbound requests in the APIHandler
     *
     * @param logger   logger for logging the request
     * @param ip       ip destination of the request
     * @param port     port destination of the request
     * @param method   method of the request
     * @param endpoint endpoint of the request
     * @param protocol protocol of the request
     */
    public static void debugOutboundRequest(CustomLogger logger, String ip, Integer port, String method, String endpoint, String protocol) {
        MDC.put(LoggerConstants.IP_DST, ip);
        MDC.put(LoggerConstants.PORT_DST, "" + port);
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        logger.debug("HTTP request sent.");
        MDC.remove(LoggerConstants.IP_DST);
        MDC.remove(LoggerConstants.PORT_DST);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
    }

    /**
     * Logs for outbound requests in the APIHandler
     *
     * @param logger   logger for logging the request
     * @param ip       ip destination of the request
     * @param port     port destination of the request
     * @param method   method of the request
     * @param endpoint endpoint of the request
     * @param protocol protocol of the request
     */
    public static void debugInboundRequest(CustomLogger logger, String ip, Integer port, String method, String endpoint, String protocol) {
        MDC.put(LoggerConstants.IP_DST, ip);
        MDC.put(LoggerConstants.PORT_DST, "" + port);
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        logger.debug("HTTP request received.");
        MDC.remove(LoggerConstants.IP_DST);
        MDC.remove(LoggerConstants.PORT_DST);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
    }

    /**
     * Logs for inbound responses in the APIHandler
     *
     * @param logger     logger to log
     * @param ip         ip source of the response
     * @param port       port source of the response
     * @param method     method of the response
     * @param endpoint   endpoint of the response
     * @param protocol   protocol of the response
     * @param statusCode HTTP code of the response
     */
    public static void debugOutboundResponse(CustomLogger logger, String ip, Integer port, String method, String endpoint, String protocol, Integer statusCode) {
        MDC.put(LoggerConstants.IP_SRC, ip);
        MDC.put(LoggerConstants.PORT_SRC, port.toString());
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        MDC.put(LoggerConstants.STATUS_CODE, statusCode.toString());
        logger.debug("HTTP response sent.");
        MDC.remove(LoggerConstants.IP_SRC);
        MDC.remove(LoggerConstants.PORT_SRC);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
        MDC.remove(LoggerConstants.STATUS_CODE);
    }

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
}
