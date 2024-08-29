package com.csl.web.jettyutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This class prints logs before and after each Jetty Server request
 */
public class JettyFilterServlet implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(JettyFilterServlet.class);
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        //Before command execute
        logger.info("Http request received, method={}, uri={}", req.getMethod(), req.getRequestURI());
        res.addHeader("Access-Control-Allow-Origin", "*");
        res.addHeader("Access-Control-Allow-Headers", "*");
        res.setContentType("application/json");

        chain.doFilter(request, response);

        //After command execute
        if (res.getStatus() == HttpServletResponse.SC_OK) {
            logger.info("Http request completed successfully, method={}, uri={}", req.getMethod(), req.getRequestURI());
        } else {
            logger.error("Http request failed, method={}, uri={}, status={}", req.getMethod(), req.getRequestURI(), res.getStatus());
        }
    }

    @Override
    public void destroy() {}
}
