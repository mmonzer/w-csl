package com.csl.logger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.MDC;

/**
 * Logger filter specific to Console Logger
 */
public class LoggerFilterConsole extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        // Avoid printing all the network logs in the console
        if (LoggerConstants.NETWORK.equals(MDC.get(LoggerConstants.LOG_TYPE))) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }
}
