package com.csl.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;

import java.util.Objects;

import static com.csl.logger.LoggerUtils.addApplicativeLog;
import static com.csl.logger.LoggerUtils.removeApplicativeLog;

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
        addVariabalesToMDC(action, requestInterface);
        logger.trace(msg);
        removeVariables();
    }

    private void debug(String action, String requestInterface, String msg) {
        addVariabalesToMDC(action, requestInterface);
        logger.debug(msg);
        removeVariables();
    }

    private void info(String action, String requestInterface, String msg) {
        addVariabalesToMDC(action, requestInterface);
        logger.info(msg);
        removeVariables();
    }

    private void warn(String action, String requestInterface, String msg) {
        addVariabalesToMDC(action, requestInterface);
        logger.warn(msg);
        removeVariables();
    }

    private void error(String action, String requestInterface, String msg) {
        addVariabalesToMDC(action, requestInterface);
        logger.error(msg);
        removeVariables();
    }

    private static void removeVariables() {
        MDC.remove("interface");
        MDC.remove("action");
        removeApplicativeLog();
    }

    private static void addVariabalesToMDC(String action, String requestInterface) {
        MDC.put("action", action);
        MDC.put("interface", requestInterface);
        addApplicativeLog();
    }

    // endregion wrappers LoggerActions and LoggerInterfaces

    // endregion LoggerActions and LoggerInterfaces

    // region usual level commands

    public void trace(String format, Object... objects) {
        addApplicativeLog();
        logger.trace(format, objects);
        removeApplicativeLog();
    }

    public void debug(String format, Object... objects) {
        addApplicativeLog();
        logger.debug(format, objects);
        removeApplicativeLog();
    }

    public void info(String format, Object... objects) {
        addApplicativeLog();
        logger.info(format, objects);
        removeApplicativeLog();
    }

    public void warn(String format, Object... objects) {
        addApplicativeLog();
        logger.warn(format, objects);
        removeApplicativeLog();
    }

    public void error(String format, Object... objects) {
        addApplicativeLog();
        logger.error(format, objects);
        removeApplicativeLog();
    }

    public void trace(String format, Throwable throwable) {
        addApplicativeLog();
        logger.trace(format, throwable);
        removeApplicativeLog();
    }

    public void debug(String format, Throwable throwable) {
        addApplicativeLog();
        logger.debug(format, throwable);
        removeApplicativeLog();
    }

    public void info(String format, Throwable throwable) {
        addApplicativeLog();
        logger.info(format, throwable);
        removeApplicativeLog();
    }

    public void warn(String format, Throwable throwable) {
        addApplicativeLog();
        logger.warn(format, throwable);
        removeApplicativeLog();
    }

    public void error(String format, Throwable throwable) {
        addApplicativeLog();
        logger.error(format, throwable);
        removeApplicativeLog();
    }

    // endregion usual level commands
}
