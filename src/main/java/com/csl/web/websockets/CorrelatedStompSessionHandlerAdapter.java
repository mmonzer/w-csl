package com.csl.web.websockets;

import com.csl.logger.LoggerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import com.csl.logger.LoggerUtils;

import static com.csl.logger.LoggerConstants.X_CORRELATION_ID;

/**
 * Class that modifies @link{StompSessionHandlerAdapter} to transfer the X_CORRELATION_ID from headers of message
 * to MDC environment (thread local)..
 */
public class CorrelatedStompSessionHandlerAdapter extends StompSessionHandlerAdapter {
    Logger logger = LoggerFactory.getLogger("WS filter");

    /**
     * Method called at the reception of a message
     * @param headers headers of the message
     * @param payloadRaw payload of the message
     */
    public void onFrame(StompHeaders headers, Object payloadRaw) {}

    /**
     * Native method called at the reception of a message.
     * Should not be used.  Use @link{onFrame} instead.
     *
     * @param headers headers of the message
     * @param payloadRaw payload of the message
     */
    @Override
    public void handleFrame(StompHeaders headers, Object payloadRaw) {
        // Variables to logger : X-Correlation-ID ...
        setVariablesToMDC(headers);
        // Log info message received
        logMessage("DEBUG",headers, "Incoming message in WS");
        // Handle frame
        super.handleFrame(headers, payloadRaw);
        onFrame(headers, payloadRaw);
    }

    /**
     * Native method called after connecting.
     *
     * @param session websocket session
     * @param headers headers of the message
     */
    public void onConnect(StompSession session, StompHeaders headers) {
    }

    /**
     * Native method called after connecting.
     * Should not be used. Use @link{onConnect} instead
     *
     * @param session websocket session
     * @param headers headers of the message
     */
    @Override
    public void afterConnected(StompSession session, StompHeaders headers) {
        // Variables to logger : X-Correlation-ID ...
        setVariablesToMDC(headers);
        // Log connection
        logMessage("INFO", headers, "Connected to WS");
        // Handles after connection
        super.afterConnected(session, headers);
        onConnect(session, headers);
    }

    /**
     * Native method called when exception happens.
     *
     * @param session websocket session
     * @param command stomp command
     * @param headers headers of the message
     * @param payload payload of the message
     * @param exception exception arose when exception
     */
    public void onException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
    }

    /**
     * Native method called when exception happens.
     * Should not be used. Use @link{onException} instead
     *
     * @param session websocket session
     * @param command stomp command
     * @param headers headers of the message
     * @param payload payload of the message
     * @param exception exception arose when exception
     */
    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        // Variables to logger : X-Correlation-ID ...
        setVariablesToMDC(headers);
        // Log exception in message
        logMessage("WARN", headers, exception.getMessage());
        // Handles the exception
        onException(session, command, headers, payload, exception);
    }

    /**
     * Native method called when error transport.
     *
     * @param session websocket session
     * @param exception exception arose when transmitting message
     */
    public void onTransportError(StompSession session, Throwable exception) {
    }

    /**
     * Native method called when error transport.
     * Should not be used. Use @link{onTransportError} instead
     *
     * @param session websocket session
     * @param exception exception arose when transmitting message
     */
    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        onTransportError(session, exception);
    }

    /**
     * Wrapper pour logging a message with the connection variables
     * @param logger logger to use
     * @param level log level
     * @param headers headers to fetch the information
     * @param message message to print
     */
    private static void logMessage(Logger logger, String level, StompHeaders headers, String message) {
        if (headers.get("destination") != null && !headers.get("destination").isEmpty()) {
            MDC.put(LoggerConstants.ENDPOINT, headers.get("destination").get(0));
        }
        MDC.put(LoggerConstants.PROTOCOL, "WS");
        LoggerUtils.log(logger,"debug", message);
        MDC.remove(LoggerConstants.PROTOCOL);
        MDC.remove(LoggerConstants.ENDPOINT);
    }

    /**
     * Wrapper pour logging a message with the connection variables
     * @param level log level
     * @param headers headers to fetch the information
     * @param message message to print
     */
    private void logMessage(String level, StompHeaders headers, String message) {
        logMessage(logger, level, headers, message);
    }

    /**
     * Set the logging variables to the thread environment
     * @param headers headers to fetch the variables
     */
    private static void setVariablesToMDC(StompHeaders headers) {
        if (headers.get(X_CORRELATION_ID)!=null && !headers.get(X_CORRELATION_ID).isEmpty()) {
            MDC.put(X_CORRELATION_ID, headers.get(X_CORRELATION_ID).get(0));
        }
    }
}
