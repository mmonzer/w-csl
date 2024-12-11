package com.csl.exceptions;

/**
 * Exception when the configuration is wrong.
 */
public class WrongConfigurationException extends RuntimeException {
    public WrongConfigurationException(String message){
        super(message);
    }

    public WrongConfigurationException(){
        super("Wrong configuration");
    }
}
