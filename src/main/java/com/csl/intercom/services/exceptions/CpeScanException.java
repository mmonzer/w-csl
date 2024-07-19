package com.csl.intercom.services.exceptions;

public class CpeScanException extends Exception {
    public CpeScanException(String message) {
        super(message);
    }

    public CpeScanException(String message, Throwable cause) {
        super(message, cause);
    }
}
