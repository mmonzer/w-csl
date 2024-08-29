package com.csl.logger;

public enum LoggerInterfaces {
    API("API"),
    CSL_SERVER("CSL_SERVER"),
    CSL_CLIENT("CSL_CLIENT"),
    CSL_SCAN_API("CSL_SCAN_API"),
    CSL_SCAN_WS("CSL_SCAN_WS"),
    CSL_AUTOCRYPT_API("CSL_AUTOCRYPT_API"),
    ;
    private final String name;

    LoggerInterfaces(String name) {
        this.name = name;
    }
    public String toString() {
        return name;
    }
}
