package com.csl.logger;

public final class LoggerConstants {
    // Fields of MDC
    public static final String X_CORRELATION_ID = "X-Correlation-ID";
    public static final String IP_SRC = "ip_src";
    public static final String PORT_SRC = "port_src";
    public static final String ENDPOINT = "endpoint";
    public static final String METHOD = "method";
    public static final String PROTOCOL = "protocol";
    public static final String STATUS_CODE = "status_code";
    public static final String IP_DST = "ip_dst";
    public static final String PORT_DST = "port_dst";

    // Orthogonal log level
    public static final String LOG_TYPE = "log_type";
    public static final String APPLICATIVE = "applicative";
    public static final String NETWORK = "network";

    // Logger default messages
    public static final String HTTP_RESPONSE_SENT = "HTTP response sent.";
    public static final String HTTP_RESPONSE_RECV = "HTTP response received.";
    public static final String HTTP_REQUEST_SENT = "HTTP request sent.";
    public static final String HTTP_REQUEST_RECV = "HTTP request received.";
    public static final String API_REQUEST_SENT = "API request sent.";
    public static final String API_REQUEST_RECV = "API request received.";
    public static final String API_RESPONSE_RECV = "API response received.";
    public static final String API_RESPONSE_SENT = "API response sent.";
    public static final String WS_REQUEST_SENT = "WS request sent.";
    public static final String WS_REQUEST_RECV = "WS request received.";
    public static final String WS_RESPONSE_RECV = "WS response received.";
    public static final String WS_RESPONSE_SENT = "WS response sent.";
}
