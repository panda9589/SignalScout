package com.scout.infrastructure.exception;

/**
 * Exception thrown when extraction from OpenAI fails
 */
public class ExtractionException extends RuntimeException {
    
    public ExtractionException(String message) {
        super(message);
    }
    
    public ExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
