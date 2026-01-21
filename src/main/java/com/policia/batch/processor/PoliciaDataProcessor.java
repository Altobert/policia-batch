package com.policia.batch.processor;

import com.policia.batch.exception.InvalidDataException;
import com.policia.batch.model.PoliciaData;
import com.policia.batch.service.XmlValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PoliciaDataProcessor implements ItemProcessor<String, PoliciaData> {

    private static final Logger logger = LoggerFactory.getLogger(PoliciaDataProcessor.class);
    
    private final XmlValidationService xmlValidationService;

    @Autowired
    public PoliciaDataProcessor(XmlValidationService xmlValidationService) {
        this.xmlValidationService = xmlValidationService;
    }

    @Override
    public PoliciaData process(String xmlMessage) throws Exception {
        logger.info("Procesando mensaje XML");
        
        try {
            // Validar y parsear el XML
            PoliciaData policiaData = xmlValidationService.validateAndParseXml(xmlMessage);
            
            // Procesar datos adicionales si es necesario
            enrichPoliciaData(policiaData);
            
            logger.info("Mensaje procesado exitosamente para policía ID: {}", policiaData.getId());
            return policiaData;
            
        } catch (InvalidDataException e) {
            logger.error("ERROR CRÍTICO: Datos inválidos detectados, deteniendo procesamiento");
            throw e; // Re-lanzar para detener el batch
        } catch (Exception e) {
            logger.error("Error inesperado durante procesamiento: {}", e.getMessage(), e);
            throw new InvalidDataException("Error inesperado durante procesamiento", xmlMessage, e);
        }
    }
    
    /**
     * Enriquece los datos con información adicional
     */
    private void enrichPoliciaData(PoliciaData policiaData) {
        // Normalizar datos
        if (policiaData.getRango() != null) {
            policiaData.setRango(policiaData.getRango().toUpperCase());
        }
        
        if (policiaData.getEstado() == null || policiaData.getEstado().trim().isEmpty()) {
            policiaData.setEstado("ACTIVO");
        }
        
        logger.debug("Datos enriquecidos para: {}", policiaData);
    }
}
