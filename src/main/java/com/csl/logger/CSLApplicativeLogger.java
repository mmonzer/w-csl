package com.csl.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class CSLApplicativeLogger {
    public static final String LOG_LEVEL_TRACE = "TRACE";
    public static final String LOG_LEVEL_DEBUG = "DEBUG";
    public static final String LOG_LEVEL_INFO = "INFO";
    public static final String LOG_LEVEL_WARN = "WARN";
    public static final String LOG_LEVEL_ERROR = "ERROR";
    private final Logger logger;

    // Private constructor to prevent direct instantiation
    private CSLApplicativeLogger(Logger logger) {
        this.logger = logger;
    }

    // Static factory method to create CustomLogger instances
    public static CSLApplicativeLogger getLogger(Class<?> clazz) {
        return new CSLApplicativeLogger(LoggerFactory.getLogger(clazz));
    }

    // region LoggerActions and LoggerInterfaces

    /**
     * Remove variables from MDC logger environment
     */
    private static void removeVariables() {
        MDC.remove("interface");
        MDC.remove("action");
        MDC.remove(LoggerConstants.LOG_TYPE);
    }

    /**
     * Add variables to MDC logger environment
     *
     * @param action           action variable
     * @param requestInterface communication interfaces variable
     */
    private static void addVariablesToMDC(String action, String requestInterface) {
        MDC.put("action", action);
        MDC.put("interface", requestInterface);
        if (MDC.get(LoggerConstants.LOG_TYPE) == null) {
            MDC.put(LoggerConstants.LOG_TYPE, LoggerConstants.APPLICATIVE);
        }
    }

    /**
     * Add variables to MDC logger environment
     */
    private static void addVariablesToMDC() {
        addVariablesToMDC(null, null);
    }

    public void trace(LoggerActions action, LoggerInterfaces interfce, String format, Object... objects) {
        this.trace(action.toString(), interfce.toString(), String.format(format, objects));
    }

    public void debug(LoggerActions action, LoggerInterfaces interfce, String format, Object... objects) {
        this.debug(action.toString(), interfce.toString(), String.format(format, objects));
    }

    public void info(LoggerActions action, LoggerInterfaces interfce, String format, Object... objects) {
        this.info(action.toString(), interfce.toString(), String.format(format, objects));
    }

    public void warn(LoggerActions action, LoggerInterfaces interfce, String format, Object... objects) {
        this.warn(action.toString(), interfce.toString(), String.format(format, objects));
    }

    public void error(LoggerActions action, LoggerInterfaces interfce, String format, Object... objects) {
        this.error(action.toString(), interfce.toString(), String.format(format, objects));
    }

    public void trace(LoggerActions action, LoggerInterfaces interfce, String msg) {
        this.trace(action.toString(), interfce.toString(), msg);
    }

    public void debug(LoggerActions action, LoggerInterfaces interfce, String msg) {
        this.debug(action.toString(), interfce.toString(), msg);
    }

    // region quickers LoggerActions and LoggerInterfaces

    // TODO : generalize with a interface string -> void

    public void info(LoggerActions action, LoggerInterfaces interfce, String msg) {
        this.info(action.toString(), interfce.toString(), msg);
    }

    public void warn(LoggerActions action, LoggerInterfaces interfce, String msg) {
        this.warn(action.toString(), interfce.toString(), msg);
    }

    public void error(LoggerActions action, LoggerInterfaces interfce, String msg) {
        this.error(action.toString(), interfce.toString(), msg);
    }

    public void traceResp(LoggerInterfaces interfce, String format, Object... objects) {
        this.trace(LoggerActions.RESPONSE, interfce, format, objects);
    }

    public void traceReq(LoggerInterfaces interfce, String format, Object... objects) {
        this.trace(LoggerActions.REQUEST, interfce, format, objects);
    }

    public void debugResp(LoggerInterfaces interfce, String format, Object... objects) {
        this.debug(LoggerActions.RESPONSE, interfce, format, objects);
    }

    public void debugReq(LoggerInterfaces interfce, String format, Object... objects) {
        this.debug(LoggerActions.REQUEST, interfce, format, objects);
    }

    public void infoResp(LoggerInterfaces interfce, String format, Object... objects) {
        this.info(LoggerActions.RESPONSE, interfce, format, objects);
    }

    public void infoReq(LoggerInterfaces interfce, String format, Object... objects) {
        this.info(LoggerActions.REQUEST, interfce, format, objects);
    }

    public void warnResp(LoggerInterfaces interfce, String format, Object... objects) {
        this.warn(LoggerActions.RESPONSE, interfce, format, objects);
    }

    public void warnReq(LoggerInterfaces interfce, String format, Object... objects) {
        this.warn(LoggerActions.REQUEST, interfce, format, objects);
    }

    public void errorResp(LoggerInterfaces interfce, String format, Object... objects) {
        this.error(LoggerActions.RESPONSE, interfce, format, objects);
    }

    public void errorReq(LoggerInterfaces interfce, String format, Object... objects) {
        this.error(LoggerActions.REQUEST, interfce, format, objects);
    }

    public void traceResp(LoggerInterfaces interfce, String msg) {
        this.trace(LoggerActions.RESPONSE, interfce, msg);
    }

    public void traceReq(LoggerInterfaces interfce, String msg) {
        this.trace(LoggerActions.REQUEST, interfce, msg);
    }

    public void debugResp(LoggerInterfaces interfce, String msg) {
        this.debug(LoggerActions.RESPONSE, interfce, msg);
    }

    public void debugReq(LoggerInterfaces interfce, String msg) {
        this.debug(LoggerActions.REQUEST, interfce, msg);
    }

    public void infoResp(LoggerInterfaces interfce, String msg) {
        this.info(LoggerActions.RESPONSE, interfce, msg);
    }

    public void infoReq(LoggerInterfaces interfce, String msg) {
        this.info(LoggerActions.REQUEST, interfce, msg);
    }

    public void warnResp(LoggerInterfaces interfce, String msg) {
        this.warn(LoggerActions.RESPONSE, interfce, msg);
    }

    // endregion quickers LoggerActions and LoggerInterfaces

    // region wrappers LoggerActions and LoggerInterfaces

    public void warnReq(LoggerInterfaces interfce, String msg) {
        this.warn(LoggerActions.REQUEST, interfce, msg);
    }

    public void errorResp(LoggerInterfaces interfce, String msg) {
        this.error(LoggerActions.RESPONSE, interfce, msg);
    }

    public void errorReq(LoggerInterfaces interfce, String msg) {
        this.error(LoggerActions.REQUEST, interfce, msg);
    }

    // TODO : generalize with a interface string -> void
    private void trace(String action, String requestInterface, String msg) {
        log(LOG_LEVEL_TRACE, action, requestInterface, msg);
    }

    private void debug(String action, String requestInterface, String msg) {
        log(LOG_LEVEL_DEBUG, action, requestInterface, msg);
    }

    // endregion wrappers LoggerActions and LoggerInterfaces

    // endregion LoggerActions and LoggerInterfaces

    // region usual level commands

    private void info(String action, String requestInterface, String msg) {
        log(LOG_LEVEL_INFO, action, requestInterface, msg);
    }

    private void warn(String action, String requestInterface, String msg) {
        log(LOG_LEVEL_WARN, action, requestInterface, msg);
    }

    private void error(String action, String requestInterface, String msg) {
        log(LOG_LEVEL_ERROR, action, requestInterface, msg);
    }

    public void trace(String format, Object... objects) {
        log(LOG_LEVEL_TRACE, format, objects);
    }

    public void debug(String format, Object... objects) {
        log(LOG_LEVEL_DEBUG, format, objects);
    }

    public void info(String format, Object... objects) {
        log(LOG_LEVEL_INFO, format, objects);
    }

    public void warn(String format, Object... objects) {
        log(LOG_LEVEL_WARN, format, objects);
    }

    public void error(String format, Object... objects) {
        log(LOG_LEVEL_ERROR, format, objects);
    }

    public void trace(String format, Throwable throwable) {
        log(LOG_LEVEL_TRACE, format, throwable);
    }

    public void debug(String format, Throwable throwable) {
        log(LOG_LEVEL_DEBUG, format, throwable);
    }

    // endregion usual level commands

    public void info(String format, Throwable throwable) {
        log(LOG_LEVEL_INFO, format, throwable);
    }

    public void warn(String format, Throwable throwable) {
        log(LOG_LEVEL_WARN, format, throwable);
    }

    public void error(String format, Throwable throwable) {
        log(LOG_LEVEL_ERROR, format, throwable);
    }

    private void log(String level, String action, String requestInterface, String message, Object... objects) {
        addVariablesToMDC(action, requestInterface);

        logWithLevel(level, message, objects);

        removeVariables();
    }

    private void log(String level, String message, Object... objects) {
        addVariablesToMDC();

        logWithLevel(level, message, objects);

        removeVariables();
    }

    /**
     * Log in the right log level the given message and given objects
     * @param level log level
     * @param message message to log
     * @param objects eventual objects to print
     */
    private void logWithLevel(String level, String message, Object[] objects) {
        switch (level.toUpperCase()) {
            case LOG_LEVEL_TRACE:
                logger.trace(message, objects);
                break;
            case LOG_LEVEL_DEBUG:
                logger.debug(message, objects);
                break;
            case LOG_LEVEL_INFO:
                logger.info(message, objects);
                break;
            case LOG_LEVEL_WARN:
                logger.warn(message, objects);
                break;
            case LOG_LEVEL_ERROR:
                logger.error(message, objects);
                break;
            default:
                break;
        }
    }

    private void log(String level, String message, Throwable throwable) {
        addVariablesToMDC();

        switch (level.toUpperCase()) {
            case LOG_LEVEL_TRACE:
                logger.trace(message, throwable);
                break;
            case LOG_LEVEL_DEBUG:
                logger.debug(message, throwable);
                break;
            case LOG_LEVEL_INFO:
                logger.info(message, throwable);
                break;
            case LOG_LEVEL_WARN:
                logger.warn(message, throwable);
                break;
            case LOG_LEVEL_ERROR:
                logger.error(message, throwable);
                break;
            default:
                break;
        }

        removeVariables();
    }
}
