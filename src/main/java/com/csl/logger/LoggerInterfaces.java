package com.csl.logger;

public enum LoggerInterfaces {
    API("API"),
    CSL_SERVER("CSL_SERVER"),
    CSL_CLIENT("CSL_CLIENT"),
    CSL_SCAN_API("CSL_SCAN_API"),
    CSL_SCAN_WS("CSL_SCAN_WS"),
    CSL_WS("CSL_WS"),
    CSL_AUTOCRYPT_API("CSL_AUTOCRYPT_API"),
    CSL_DBAPI_API("CSL_DBAPI_API"),
    NULL("NULL"),
    CSL_NGINX("CSL_NGINX"),
    LOCAL("LOCAL"),
    WS("WS"),
    ;
    private final String name;

    LoggerInterfaces(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
