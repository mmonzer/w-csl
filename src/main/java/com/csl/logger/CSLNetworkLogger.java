package com.csl.logger;

import org.apache.commons.math3.ml.neuralnet.Network;
import org.slf4j.Logger;
import org.slf4j.MDC;

import static com.csl.logger.LoggerConstants.*;

/**
 * Logger specialized in logging incoming and outgoing requests/responses.
 */
public class CSLNetworkLogger {

    /**
     * Logs for  info
     *
     * @param logger   logger for logging the request
     * @param endpoint endpoint of the request
     * @param protocol protocol of the request
     */
    public static void info(Logger logger, String endpoint, String protocol, String message) {
        infoInboundRequest(logger, null, null, null, endpoint, protocol, message);
    }

    /**
     * Logs for debug
     *
     * @param logger   logger for logging the request
     * @param endpoint endpoint of the request
     * @param protocol protocol of the request
     */
    public static void debug(Logger logger, String endpoint, String protocol, String message) {
        debugOutboundRequest(logger, null, null, null, endpoint, protocol, message);
    }

    /**
     * Logs for warn
     *
     * @param logger   logger for logging the request
     * @param endpoint endpoint of the request
     * @param protocol protocol of the request
     */
    public static void warn(Logger logger, String endpoint, String protocol, String message) {
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        MDC.put(LOG_TYPE, NETWORK);
        logger.warn(message);
        MDC.put(LOG_TYPE, APPLICATIVE);
        MDC.remove(LoggerConstants.ENDPOINT);
        MDC.remove(LoggerConstants.PROTOCOL);
    }

    /**
     * Logs for error
     *
     * @param logger   logger for logging the request
     * @param endpoint endpoint of the request
     * @param protocol protocol of the request
     */
    public static void error(Logger logger, String endpoint, String protocol, String message) {
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        MDC.put(LOG_TYPE, NETWORK);
        logger.error(message);
        MDC.put(LOG_TYPE, APPLICATIVE);
        MDC.remove(LoggerConstants.ENDPOINT);
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
    public static void infoInboundRequest(Logger logger, String ip, Integer port, String method, String endpoint, String protocol, String message) {
        setVariablesSource(ip, port);
        String oldEndpoint = setVariables(method, endpoint, protocol, null);
        MDC.put(LOG_TYPE, NETWORK);
        logger.info(message);
        MDC.put(LOG_TYPE, APPLICATIVE);
        removeVariables(oldEndpoint);
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
    public static void infoInboundRequest(CSLApplicativeLogger logger, String ip, Integer port, String method, String endpoint, String protocol, String message) {
        setVariablesSource(ip, port);
        String oldEndpoint = setVariables(method, endpoint, protocol, null);
        MDC.put(LOG_TYPE, NETWORK);
        logger.info(message);
        MDC.put(LOG_TYPE, APPLICATIVE);
        removeVariables(oldEndpoint);
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
    public static void infoOutboundResponse(CSLApplicativeLogger logger, String ip, Integer port, String method, String endpoint, String protocol, Integer statusCode, String message) {
        infoMessage(logger, ip, port, method, endpoint, protocol, statusCode, message);
    }

    private static void infoMessage(Logger logger, String ip, Integer port, String method, String endpoint, String protocol, Integer statusCode, String message) {
        setVariablesDestination(ip, port);
        String oldEndpoint = setVariables(method, endpoint, protocol, statusCode);
        MDC.put(LOG_TYPE, NETWORK);
        logger.info(message);
        MDC.put(LOG_TYPE, APPLICATIVE);
        removeVariables(oldEndpoint);
    }

    private static void infoMessage(CSLApplicativeLogger logger, String ip, Integer port, String method, String endpoint, String protocol, Integer statusCode, String message) {
        setVariablesDestination(ip, port);
        String oldEndpoint = setVariables(method, endpoint, protocol, statusCode);
        MDC.put(LOG_TYPE, NETWORK);
        logger.info(message);
        MDC.put(LOG_TYPE, APPLICATIVE);
        removeVariables(oldEndpoint);
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
        String oldEndpoint = setVariables( method, endpoint,protocol, statusCode);
        MDC.put(LOG_TYPE, NETWORK);
        logger.debug(message);
        MDC.put(LOG_TYPE, APPLICATIVE);
        removeVariables(oldEndpoint);
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
        String oldEndpoint = setVariables( method, endpoint,protocol, null);
        MDC.put(LOG_TYPE, NETWORK);
        logger.debug(message);
        MDC.put(LOG_TYPE, APPLICATIVE);
        removeVariables(oldEndpoint);
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
        String oldEndpoint = setVariables(method, endpoint, protocol, statusCode);
        MDC.put(LOG_TYPE, NETWORK);
        logger.debug("HTTP response received.");
        MDC.put(LOG_TYPE, APPLICATIVE);
        removeVariables(oldEndpoint);
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
        String oldEndpoint = setVariables(method, endpoint, protocol, null);
        MDC.put(LOG_TYPE, NETWORK);
        logger.debug("HTTP request sent.");
        MDC.put(LOG_TYPE, APPLICATIVE);
        removeVariables(oldEndpoint);
    }

    /**
     * Sets the network log label on
     */
    public static void addNetworkLog() {
        MDC.put(LOG_TYPE, NETWORK);
    }

    /**
     * Sets the network log label off
     */
    public static void removeNetworkLog() {
        MDC.remove(LOG_TYPE);
    }

    /**
     * Removes all the set of MDC variables from the logger environment
     */
    private static void removeVariables(String oldEndpoint) {
        MDC.remove(LoggerConstants.IP_SRC);
        MDC.remove(LoggerConstants.PORT_SRC);
        MDC.remove(LoggerConstants.IP_DST);
        MDC.remove(LoggerConstants.PORT_DST);
        MDC.remove(LoggerConstants.METHOD);
        MDC.remove(LoggerConstants.PROTOCOL);
        MDC.remove(LoggerConstants.STATUS_CODE);
        removeNetworkLog();
        MDC.put(LoggerConstants.ENDPOINT, oldEndpoint);
    }

    /**
     * Sets some variables to the MDC logger environment
     *
     * @param method     method of the request
     * @param endpoint   endpoint of the request
     * @param protocol   protocol of the communication
     * @param statusCode code of the response
     */
    private static String setVariables(String method, String endpoint, String protocol, Integer statusCode) {
        String oldEndpoint = MDC.get(LoggerConstants.ENDPOINT);
        MDC.put(LoggerConstants.ENDPOINT, endpoint);
        MDC.put(LoggerConstants.METHOD, method);
        MDC.put(LoggerConstants.PROTOCOL, protocol);
        MDC.put(LoggerConstants.STATUS_CODE, (statusCode == null) ? null : statusCode.toString());
        addNetworkLog();
        return oldEndpoint;
    }

    /**
     * Set the ip and port of the source to MDC logger environment
     *
     * @param ip   ip source of the communication
     * @param port port source of the communication
     */
    private static void setVariablesSource(String ip, Integer port) {
        MDC.put(LoggerConstants.IP_SRC, ip);
        MDC.put(LoggerConstants.PORT_SRC, (port == null) ? null : port.toString());
    }

    /**
     * Set the ip and port of the destination to MDC logger environment
     *
     * @param ip   ip destination of the communication
     * @param port port destination of the communication
     */
    private static void setVariablesDestination(String ip, Integer port) {
        MDC.put(LoggerConstants.IP_DST, ip);
        MDC.put(LoggerConstants.PORT_DST, (port == null) ? null : port.toString());
    }
}
