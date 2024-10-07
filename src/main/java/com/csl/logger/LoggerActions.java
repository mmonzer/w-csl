package com.csl.logger;

public enum LoggerActions {
    REQUEST("request"),
    RESPONSE("response"),
    NULL("null"),
    SYNC("sync"),
    ;

    private final String name;

    LoggerActions(String name) {
        this.name = name;
    }
    public String toString() {
        return name;
    }
}
