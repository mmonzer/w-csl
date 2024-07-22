package com.csl.web.jettyutils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This class prints logs before and after each Jetty Server request
 */
public class JettyFilterServlet implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        //Before command execute
        System.out.println("WEB_DEBUG:"+req.getRequestURI()+" "+req.getMethod());
        res.addHeader("Access-Control-Allow-Origin", "*");
        res.addHeader("Access-Control-Allow-Headers", "*");
        res.setContentType("application/json");
        chain.doFilter(request, response);
        //After command execute
        System.out.println("req:"+req.getRequestURI());

    }

    @Override
    public void destroy() {}
}
