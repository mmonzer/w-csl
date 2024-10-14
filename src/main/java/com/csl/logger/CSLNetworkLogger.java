package com.csl.logger;

import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 * Logger specialized in logging incoming and outgoing requests/responses.
 */
public class CSLNetworkLogger {

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
    public static void infoInboundRequest(Logger logger, String ip, Integer port, String method, String endpoint, String protocol, String message) {
        setVariablesSource(ip, port);
        setVariables(method, endpoint, protocol, null);
        logger.info(message);
        removeVariables();
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
    public static void infoOutboundResponse(Logger logger, String ip, Integer port, String method, String endpoint, String protocol, Integer statusCode, String message) {
        infoMessage(logger, ip, port, method, endpoint, protocol, statusCode, message);
    }

    private static void infoMessage(Logger logger, String ip, Integer port, String method, String endpoint, String protocol, Integer statusCode, String message) {
        setVariablesDestination(ip, port);
        setVariables(method, endpoint, protocol, statusCode);
        logger.info(message);
        removeVariables();
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
    public static void debugInboundResponse(Logger logger, String ip, Integer port, String method, String endpoint, String protocol, Integer statusCode, String message) {
        setVariablesSource(ip, port);
        setVariables(endpoint, method, protocol, statusCode);
        logger.debug(message);
        removeVariables();
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
        debugInboundResponse(logger, ip, port, method, endpoint, protocol, statusCode, LoggerConstants.HTTP_RESPONSE_RECV);
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
    public static void debugOutboundRequest(Logger logger, String ip, Integer port, String method, String endpoint, String protocol, String message) {
        setVariablesDestination(ip, port);
        setVariables(endpoint, method, protocol, null);
        logger.debug(message);
        removeVariables();
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
        debugOutboundRequest(logger, ip, port, method, endpoint, protocol, LoggerConstants.HTTP_REQUEST_SENT);
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
    public static void debugInboundResponse(CSLApplicativeLogger logger, String ip, Integer port, String method, String endpoint, String protocol, Integer statusCode) {
        setVariablesSource(ip, port);
        setVariables(endpoint, method, protocol, statusCode);
        logger.debug("HTTP response received.");
        removeVariables();
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
    public static void debugOutboundRequest(CSLApplicativeLogger logger, String ip, Integer port, String method, String endpoint, String protocol) {
        setVariablesDestination(ip, port);
        setVariables(endpoint, method, protocol, null);
        logger.debug("HTTP request sent.");
        removeVariables();
    }


    /**
     * Sets the network log label on
     */
    public static void addNetworkLog() {
        MDC.put(LoggerConstants.LOG_TYPE, LoggerConstants.NETWORK);
    }

    /**
     * Sets the network log label off
     */
    public static void removeNetworkLog() {
        MDC.remove(LoggerConstants.LOG_TYPE);
    }

    /**
     * Removes all the set of MDC variables from the logger environment
     */
    private static void removeVariables() {
        MDC.remove(LoggerConstants.IP_SRC);
        MDC.remove(LoggerConstants.PORT_SRC);
        MDC.remove(LoggerConstants.IP_DST);
        MDC.remove(LoggerConstants.PORT_DST);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
        MDC.remove(LoggerConstants.STATUS_CODE);
        removeNetworkLog();
    }

    /**
     * Sets some variables to the MDC logger environment
     * @param method method of the request
     * @param endpoint endpoint of the request
     * @param protocol protocol of the communication
     * @param statusCode code of the response
     */
    private static void setVariables(String method, String endpoint, String protocol, Integer statusCode) {
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        MDC.put(LoggerConstants.STATUS_CODE, statusCode.toString());
        addNetworkLog();
    }

    /**
     * Set the ip and port of the source to MDC logger environment
     * @param ip ip source of the communication
     * @param port port source of the communication
     */
    private static void setVariablesSource(String ip, Integer port) {
        MDC.put(LoggerConstants.IP_SRC, ip);
        MDC.put(LoggerConstants.PORT_SRC, port.toString());
    }


    /**
     * Set the ip and port of the destination to MDC logger environment
     * @param ip ip destination of the communication
     * @param port port destination of the communication
     */
    private static void setVariablesDestination(String ip, Integer port) {
        MDC.put(LoggerConstants.IP_DST, ip);
        MDC.put(LoggerConstants.PORT_DST, port.toString());
    }
}
