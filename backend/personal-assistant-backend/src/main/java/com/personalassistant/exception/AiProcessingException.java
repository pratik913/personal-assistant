package com.personalassistant.exception;

import lombok.Getter;

@Getter
public class AiProcessingException extends RuntimeException {

    private final AiErrorType errorType;

    public AiProcessingException(
            AiErrorType errorType,
            String message
    ) {
        super(message);
        this.errorType = errorType;
    }

    public AiProcessingException(
            AiErrorType errorType,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.errorType = errorType;
    }
}