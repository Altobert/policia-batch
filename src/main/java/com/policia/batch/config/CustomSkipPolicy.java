package com.policia.batch.config;

import com.policia.batch.exception.InvalidDataException;
import com.policia.batch.exception.MalformedXmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipPolicy;

public class CustomSkipPolicy implements SkipPolicy {

    private static final Logger logger = LoggerFactory.getLogger(CustomSkipPolicy.class);

    @Override
    public boolean shouldSkip(Throwable exception, int skipCount) {
        
        // NUNCA saltar errores de datos inválidos o XML mal formado
        if (exception instanceof InvalidDataException || 
            exception instanceof MalformedXmlException) {
            
            logger.error("ERROR CRÍTICO DETECTADO - NO SE PUEDE SALTAR: {}", exception.getMessage());
            logger.error("El batch debe detenerse debido a datos inválidos");
            
            return false; // NO saltar, esto causará que el job falle
        }
        
        // Para otros tipos de errores, puedes definir políticas diferentes
        // Por ejemplo, errores de red transitorios podrían ser saltados un número limitado de veces
        if (skipCount < 3) {
            logger.warn("Error transiente, intentando saltar (intento {}): {}", skipCount + 1, exception.getMessage());
            return true;
        }
        
        logger.error("Se ha excedido el límite de errores saltables: {}", exception.getMessage());
        return false;
    }
}
