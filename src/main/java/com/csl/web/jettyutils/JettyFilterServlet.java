package com.csl.web.jettyutils;

import com.csl.logger.LoggerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.ServerException;

/**
 * This class prints logs before and after each Jetty Server request
 */
public class JettyFilterServlet implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(JettyFilterServlet.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Check right protocol
        if (! (request instanceof HttpServletRequest)) {
            throw new ServerException("Wrong request protocol");
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        setResponseHeaders(httpResponse);

        // actions before the execution of the request (controller handling)
        preHandle(httpRequest, httpResponse, chain);

        // execute controller handling
        chain.doFilter(httpRequest, httpResponse);

        // actions after the execution of the request (controller handling)
        postHandle(httpRequest, httpResponse, chain);
    }

    @Override
    public void destroy() {
    }

    /**
     * Actions to execute before the request in handled by controllers
     * @param request
     * @param response
     * @param chain
     */
    private void preHandle(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        setVariablesToMDC(request);

        // Log the input
        logIncomingRequest(request);
    }

    /**
     * Actions to execute after the request in handled by controllers
     * @param request
     * @param response
     * @param chain
     */
    private void postHandle(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        sendBackXCorrelationId(request, response);

        // Log the output
        logOutgoingResponse(request, response);

    }

    /**
     * Set the headers to the HTTP response
     * @param response HTTP response to add headers
     */
    private static void setResponseHeaders(HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Headers", "*");
        response.setContentType("application/json");
    }

    /**
     * Fetches the X-Correlation-ID from the headers of the HTTP request and set its to logs (local to thread).
     * @param request HTTP request
     */
    private static void sendBackXCorrelationId(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(LoggerConstants.X_CORRELATION_ID, request.getHeader(LoggerConstants.X_CORRELATION_ID));
    }

    /**
     * Log message for the incoming HTTP requests. It logs the src of the request
     * @param request HTTP request received
     */
    private void logIncomingRequest(HttpServletRequest request) {
        MDC.put(LoggerConstants.IP_SRC, request.getRemoteAddr());
        MDC.put(LoggerConstants.PORT_SRC, ""+ request.getRemotePort());
        MDC.put(LoggerConstants.PROTOCOL, request.getProtocol());
        logger.info("Incoming HTTP request");
        MDC.remove(LoggerConstants.IP_SRC);
        MDC.remove(LoggerConstants.PORT_SRC);
        MDC.remove(LoggerConstants.PROTOCOL);
    }

    /**
     * Log message for the outgoing HTTP response. It logs the dst of the response
     * @param request HTTP response received
     * @param response HTTP response sent
     */
    private void logOutgoingResponse(HttpServletRequest request, HttpServletResponse response) {
        MDC.put(LoggerConstants.IP_DST, request.getRemoteAddr());
        MDC.put(LoggerConstants.PORT_DST, ""+ request.getRemotePort());
        MDC.put(LoggerConstants.PROTOCOL, request.getProtocol());
        MDC.put(LoggerConstants.STATUS_CODE, ""+ response.getStatus());
        if (response.getStatus() == HttpServletResponse.SC_OK) {
            logger.info("Http request completed successfully");
        } else {
            logger.error("Http request failed");
        }
        MDC.remove(LoggerConstants.IP_DST);
        MDC.remove(LoggerConstants.PORT_DST);
        MDC.remove(LoggerConstants.PROTOCOL);
        MDC.remove(LoggerConstants.STATUS_CODE);
    }

    /**
     * It sets the logger variables from the request data : X-Correlation-ID,  method, URI.
     * @param request HTTP response received
     */
    private void setVariablesToMDC(HttpServletRequest request) {
        MDC.put(LoggerConstants.X_CORRELATION_ID, request.getHeader("X-Correlation-ID"));
        MDC.put(LoggerConstants.ENDPOINT, request.getRequestURI());
        MDC.put(LoggerConstants.METHOD, request.getMethod());
    }
}
