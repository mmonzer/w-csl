package com.csl.web.jettyutils;

import com.csl.logger.LoggerConstants;
import com.csl.web.HTTPConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.ServerException;

import static com.csl.logger.CSLNetworkLogger.*;

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
        preHandle(httpRequest);

        // execute controller handling
        chain.doFilter(httpRequest, httpResponse);

        // actions after the execution of the request (controller handling)
        postHandle(httpRequest, httpResponse);
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
    private void preHandle(HttpServletRequest request) {
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
    private void postHandle(HttpServletRequest request, HttpServletResponse response) {
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
        response.setContentType(HTTPConstants.JSON_FORMAT);
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
        infoInboundRequest(logger, request.getRemoteAddr(), request.getRemotePort(), "POST", request.getRequestURI(), request.getProtocol(), LoggerConstants.API_REQUEST_RECV);

    }

    /**
     * Log message for the outgoing HTTP response. It logs the dst of the response
     * @param request HTTP response received
     * @param response HTTP response sent
     */
    private void logOutgoingResponse(HttpServletRequest request, HttpServletResponse response) {
        infoOutboundResponse(logger, request.getRemoteAddr(), request.getRemotePort(), "POST", request.getRequestURI(), request.getProtocol(), response.getStatus(), LoggerConstants.API_RESPONSE_SENT);
    }

    /**
     * It sets the logger variables from the request data : X-Correlation-ID,  method, URI.
     * @param request HTTP response received
     */
    private void setVariablesToMDC(HttpServletRequest request) {
        MDC.put(LoggerConstants.X_CORRELATION_ID, request.getHeader(LoggerConstants.X_CORRELATION_ID));
        MDC.put(LoggerConstants.ENDPOINT, request.getRequestURI());
        MDC.put(LoggerConstants.METHOD, request.getMethod());
    }
}
