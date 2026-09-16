package com.personalassistant.exception;

public class CaptureNotFoundException extends RuntimeException {

    public CaptureNotFoundException(String message) {
        super(message);
    }
}