package com.csl.web.websockets;

import org.slf4j.MDC;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import static com.csl.web.jcmdoversocket.CSLWebSocketForJcmd.X_CORRELATION_ID;

/**
 * Class that modifies @link{StompSessionHandlerAdapter} to transfer the X_CORRELATION_ID from headers of message
 * to MDC environment (thread local)..
 */
public class CorrelatedStompSessionHandlerAdapter extends StompSessionHandlerAdapter {

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
        if (headers.get(X_CORRELATION_ID)!=null && !headers.get(X_CORRELATION_ID).isEmpty()) {
            MDC.put(X_CORRELATION_ID, headers.get(X_CORRELATION_ID).get(0));
        }
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
        if (headers.get(X_CORRELATION_ID)!=null && !headers.get(X_CORRELATION_ID).isEmpty()) {
            MDC.put(X_CORRELATION_ID, headers.get(X_CORRELATION_ID).get(0));
        }
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
        if (headers.get(X_CORRELATION_ID)!=null && !headers.get(X_CORRELATION_ID).isEmpty()) {
            MDC.put(X_CORRELATION_ID, headers.get(X_CORRELATION_ID).get(0));
        }
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
}
