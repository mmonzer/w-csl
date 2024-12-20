package com.csl.intercom.dbapi.exceptions;

public class DbapiUnexpectedStatusCodeException extends Exception {

    public DbapiUnexpectedStatusCodeException(String message, Integer statusCode) {
        super(message + " (status code: " + statusCode + ")");
        this.statusCode = statusCode;
    }
}
