package com.policia.batch.exception;

/**
 * Excepción lanzada cuando se encuentran datos inválidos que requieren detener el procesamiento del batch
 */
public class InvalidDataException extends RuntimeException {
    
    private final String invalidData;
    
    public InvalidDataException(String message, String invalidData) {
        super(message);
        this.invalidData = invalidData;
    }
    
    public InvalidDataException(String message, String invalidData, Throwable cause) {
        super(message, cause);
        this.invalidData = invalidData;
    }
    
    public String getInvalidData() {
        return invalidData;
    }
}
