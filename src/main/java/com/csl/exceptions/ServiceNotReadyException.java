package com.csl.exceptions;

import java.net.ConnectException;

/**
 * Exception when the service to connect is not ready
 */
public class ServiceNotReadyException extends ConnectException {
    public ServiceNotReadyException(String message){
        super(message);
    }

    public ServiceNotReadyException(){
        super("Service is not ready");
    }
}
