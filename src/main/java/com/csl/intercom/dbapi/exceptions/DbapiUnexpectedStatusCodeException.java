package com.csl.intercom.dbapi.exceptions;

public class DbapiUnexpectedStatusCodeException extends Exception {
    private Integer statusCode;
    public DbapiUnexpectedStatusCodeException(String message) {
        super(message);
    }

    public DbapiUnexpectedStatusCodeException(String message, Integer statusCode) {
        super(message + " (status code: " + statusCode + ")");
        this.statusCode = statusCode;
    }
}
