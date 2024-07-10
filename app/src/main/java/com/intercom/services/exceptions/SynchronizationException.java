package com.csl.intercom.services.exceptions;

public class SynchronizationException extends Exception {
    public SynchronizationException(String message) {
        super(message);
    }

    public SynchronizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
