package com.csl.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class CSLApplicativeLogger {
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

    public void info(LoggerActions action, LoggerInterfaces interfce, String msg) {
        this.info(action.toString(), interfce.toString(), msg);
    }

    public void warn(LoggerActions action, LoggerInterfaces interfce, String msg) {
        this.warn(action.toString(), interfce.toString(), msg);
    }

    public void error(LoggerActions action, LoggerInterfaces interfce, String msg) {
        this.error(action.toString(), interfce.toString(), msg);
    }

    // region quickers LoggerActions and LoggerInterfaces

    // TODO : generalize with a interface string -> void

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

    public void warnReq(LoggerInterfaces interfce, String msg) {
        this.warn(LoggerActions.REQUEST, interfce, msg);
    }

    public void errorResp(LoggerInterfaces interfce, String msg) {
        this.error(LoggerActions.RESPONSE, interfce, msg);
    }

    public void errorReq(LoggerInterfaces interfce, String msg) {
        this.error(LoggerActions.REQUEST, interfce, msg);
    }

    // endregion quickers LoggerActions and LoggerInterfaces

    // region wrappers LoggerActions and LoggerInterfaces

    // TODO : generalize with a interface string -> void
    private void trace(String action, String requestInterface, String msg) {
        log("TRACE", action, requestInterface, msg);
    }

    private void debug(String action, String requestInterface, String msg) {
        log("DEBUG", action, requestInterface, msg);
    }

    private void info(String action, String requestInterface, String msg) {
        log("INFO", action, requestInterface, msg);
    }

    private void warn(String action, String requestInterface, String msg) {
        log("WARN", action, requestInterface, msg);
    }

    private void error(String action, String requestInterface, String msg) {
        log("ERROR", action, requestInterface, msg);
    }

    // endregion wrappers LoggerActions and LoggerInterfaces

    // endregion LoggerActions and LoggerInterfaces

    // region usual level commands

    public void trace(String format, Object... objects) {
        log("TRACE", format, objects);
    }

    public void debug(String format, Object... objects) {
        log("DEBUG", format, objects);
    }

    public void info(String format, Object... objects) {
        log("INFO", format, objects);
    }

    public void warn(String format, Object... objects) {
        log("WARN", format, objects);
    }

    public void error(String format, Object... objects) {
        log("ERROR", format, objects);
    }

    public void trace(String format, Throwable throwable) {
        log("TRACE", format, throwable);
    }

    public void debug(String format, Throwable throwable) {
        log("DEBUG", format, throwable);
    }

    public void info(String format, Throwable throwable) {
        log("INFO", format, throwable);
    }

    public void warn(String format, Throwable throwable) {
        log("WARN", format, throwable);
    }

    public void error(String format, Throwable throwable) {
        log("ERROR", format, throwable);
    }

    // endregion usual level commands

    private void log(String level, String action, String requestInterface, String message, Object... objects) {
        addVariablesToMDC(action, requestInterface);

        switch (level.toLowerCase()) {
            case "trace":
                logger.trace(message, objects);
                break;
            case "debug":
                logger.debug(message, objects);
                break;
            case "info":
                logger.info(message, objects);
                break;
            case "warn":
                logger.warn(message, objects);
                break;
            case "error":
                logger.error(message, objects);
                break;
            default:
                break;
        }
        removeVariables();
    }

    private void log(String level, String message, Object... objects) {
        addVariablesToMDC();

        switch (level.toLowerCase()) {
            case "trace":
                logger.trace(message, objects);
                break;
            case "debug":
                logger.debug(message, objects);
                break;
            case "info":
                logger.info(message, objects);
                break;
            case "warn":
                logger.warn(message, objects);
                break;
            case "error":
                logger.error(message, objects);
                break;
            default:
                break;
        }
        removeVariables();
    }

    private void log(String level, String message, Throwable throwable) {
        addVariablesToMDC();

        switch (level.toLowerCase()) {
            case "trace":
                logger.trace(message, throwable);
                break;
            case "debug":
                logger.debug(message, throwable);
                break;
            case "info":
                logger.info(message, throwable);
                break;
            case "warn":
                logger.warn(message, throwable);
                break;
            case "error":
                logger.error(message, throwable);
                break;
            default:
                break;
        }

        removeVariables();
    }

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
        MDC.put(LoggerConstants.LOG_TYPE, LoggerConstants.APPLICATIVE);
    }

    /**
     * Add variables to MDC logger environment
     */
    private static void addVariablesToMDC() {
        addVariablesToMDC(null, null);
    }
}
