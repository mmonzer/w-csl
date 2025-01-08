package com.csl.logger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Logger filter common to File and Console Logger
 */
public class LoggerFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        // Logs that were created by Jetty websocket when could not connect the server. They included
        // traces by default. Filter will do the work while finding a cleaner way.
        if (event.getMessage().startsWith("Unhandled Error: com.csl.web.WebsocketClientEndpoint@")) {
            return FilterReply.DENY;
        }

        // STOMP websocket too many logs
        if (event.getMessage().startsWith("ERROR DefaultTransportRequest No more fallback transports after TransportRequest")) {
            return FilterReply.DENY;
        }

        // Logs created by Jetty in the new version are not silenciables by the setLog(NoLogging).
        if (event.getLoggerName().startsWith("org.eclipse.jetty")) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }
}
