package com.csl.web.jettyutils;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * This class prints logs when a server error occurs

 */
public class JettyServerErrorHandler extends ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(JettyServerErrorHandler.class);

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Throwable th = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (th != null) {
            logger.error("Exception: ", th);
            logger.error("Exception message: " + th.getMessage());
        } else {
            // No exception was thrown, log the status and error message of the response
            int status = response.getStatus();
            String errorMessage = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
            logger.error("Response status: " + status);
            logger.error("Error message: " + errorMessage);
        }
        logger.error("Request URI: " + request.getRequestURI());
        logger.error("Request method: " + request.getMethod());
        logger.error("Request parameters: " + request.getParameterMap().toString());
        logger.error("Request attributes: " + Collections.list(request.getAttributeNames()).toString());
        try {
            super.handle(target, baseRequest, request, response);
        }catch (ServletException e) {
            logger.error("Error handling server error: ", e);
        }

    }
}
