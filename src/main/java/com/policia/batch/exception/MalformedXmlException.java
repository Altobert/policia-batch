package com.policia.batch.exception;

/**
 * Excepción lanzada cuando se detecta XML mal formado
 */
public class MalformedXmlException extends InvalidDataException {
    
    public MalformedXmlException(String message, String xmlData) {
        super(message, xmlData);
    }
    
    public MalformedXmlException(String message, String xmlData, Throwable cause) {
        super(message, xmlData, cause);
    }
}
