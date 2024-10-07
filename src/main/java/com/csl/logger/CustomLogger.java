package com.csl.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;

import java.util.Objects;

public class CustomLogger {
    private final Logger logger;

    // Private constructor to prevent direct instantiation
    private CustomLogger(Logger logger) {
        this.logger = logger;
    }

    // Static factory method to create CustomLogger instances
    public static CustomLogger getLogger(Class<?> clazz) {
        return new CustomLogger(LoggerFactory.getLogger(clazz));
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
        MDC.put("action", action);
        MDC.put("interface", requestInterface);
        logger.trace(msg);
        MDC.remove("interface");
        MDC.remove("action");
    }

    private void debug(String action, String requestInterface, String msg) {
        MDC.put("action", action);
        MDC.put("interface", requestInterface);
        logger.debug(msg);
        MDC.remove("interface");
        MDC.remove("action");
    }

    private void info(String action, String requestInterface, String msg) {
        MDC.put("action", action);
        MDC.put("interface", requestInterface);
        logger.info(msg);
        MDC.remove("interface");
        MDC.remove("action");
    }

    private void warn(String action, String requestInterface, String msg) {
        MDC.put("action", action);
        MDC.put("interface", requestInterface);
        logger.warn(msg);
        MDC.remove("interface");
        MDC.remove("action");
    }

    private void error(String action, String requestInterface, String msg) {
        MDC.put("action", action);
        MDC.put("interface", requestInterface);
        logger.error(msg);
        MDC.remove("interface");
        MDC.remove("action");
    }

    // endregion wrappers LoggerActions and LoggerInterfaces

    // endregion LoggerActions and LoggerInterfaces

    // region usual level commands

    public void trace(String msg) {
        logger.trace(msg);
    }

    public void debug(String msg) {
        logger.debug(msg);
    }

    public void info(String msg) {
        logger.info(msg);
    }

    public void warn(String msg) {
        logger.warn(msg);
    }

    public void error(String msg) {
        logger.error(msg);
    }

    public void trace(String format, Object... objects) {
        logger.trace(format, objects);
    }

    public void debug(String format, Object... objects) {
        logger.debug(format, objects);
    }

    public void info(String format, Object... objects) {
        logger.info(format, objects);
    }

    public void warn(String format, Object... objects) {
        logger.warn(format, objects);
    }

    public void error(String format, Object... objects) {
        logger.error(format, objects);
    }

    public void trace(String format, Throwable throwable) {
        logger.trace(format, throwable);
    }

    public void debug(String format, Throwable throwable) {
        logger.debug(format, throwable);
    }

    public void info(String format, Throwable throwable) {
        logger.info(format, throwable);
    }

    public void warn(String format, Throwable throwable) {
        logger.warn(format, throwable);
    }

    public void error(String format, Throwable throwable) {
        logger.error(format, throwable);
    }

    // endregion usual level commands
}
