package com.policia.batch.service;

import com.policia.batch.exception.InvalidDataException;
import com.policia.batch.exception.MalformedXmlException;
import com.policia.batch.model.PoliciaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import java.io.StringReader;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class XmlValidationService {

    private static final Logger logger = LoggerFactory.getLogger(XmlValidationService.class);
    
    private final JAXBContext jaxbContext;
    private final Validator validator;

    @Autowired
    public XmlValidationService(Validator validator) throws JAXBException {
        this.jaxbContext = JAXBContext.newInstance(PoliciaData.class);
        this.validator = validator;
    }

    /**
     * Valida y convierte XML a objeto PoliciaData
     */
    public PoliciaData validateAndParseXml(String xmlContent) {
        try {
            logger.debug("Iniciando validación de XML");
            
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            
            // Configurar manejador de validación
            unmarshaller.setEventHandler(new ValidationEventHandler() {
                @Override
                public boolean handleEvent(ValidationEvent event) {
                    ValidationEventLocator locator = event.getLocator();
                    String errorMessage = String.format(
                        "Error de validación XML en línea %d, columna %d: %s",
                        locator.getLineNumber(),
                        locator.getColumnNumber(),
                        event.getMessage()
                    );
                    logger.error(errorMessage);
                    throw new MalformedXmlException(errorMessage, xmlContent);
                }
            });
            
            // Parsear XML a objeto
            PoliciaData policiaData = (PoliciaData) unmarshaller.unmarshal(new StringReader(xmlContent));
            
            // Validar anotaciones de Bean Validation
            validateBusinessRules(policiaData, xmlContent);
            
            logger.info("XML validado y parseado exitosamente: ID={}, Nombre={}", 
                       policiaData.getId(), policiaData.getNombre());
            
            return policiaData;
            
        } catch (JAXBException e) {
            String errorMessage = "Error al parsear XML: " + e.getMessage();
            logger.error(errorMessage, e);
            throw new MalformedXmlException(errorMessage, xmlContent, e);
        } catch (InvalidDataException e) {
            // Re-lanzar excepciones de datos inválidos
            throw e;
        } catch (Exception e) {
            String errorMessage = "Error inesperado durante validación XML: " + e.getMessage();
            logger.error(errorMessage, e);
            throw new InvalidDataException(errorMessage, xmlContent, e);
        }
    }

    /**
     * Valida reglas de negocio usando Bean Validation
     */
    private void validateBusinessRules(PoliciaData policiaData, String xmlContent) {
        Set<ConstraintViolation<PoliciaData>> violations = validator.validate(policiaData);
        
        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Errores de validación de datos: ");
            
            for (ConstraintViolation<PoliciaData> violation : violations) {
                errorMessage.append(String.format("[%s: %s] ", 
                    violation.getPropertyPath(), 
                    violation.getMessage()));
            }
            
            logger.error("Datos inválidos encontrados: {}", errorMessage.toString());
            throw new InvalidDataException(errorMessage.toString(), xmlContent);
        }
        
        // Validaciones adicionales de negocio
        validateAdditionalBusinessRules(policiaData, xmlContent);
    }
    
    /**
     * Validaciones adicionales específicas del dominio
     */
    private void validateAdditionalBusinessRules(PoliciaData policiaData, String xmlContent) {
        // Validar formato del ID
        if (policiaData.getId() != null && !policiaData.getId().matches("^POL\\d{6}$")) {
            String errorMessage = "ID de policía debe tener formato POL seguido de 6 dígitos: " + policiaData.getId();
            logger.error(errorMessage);
            throw new InvalidDataException(errorMessage, xmlContent);
        }
        
        // Validar rangos válidos
        String[] rangosValidos = {"AGENTE", "CABO", "SARGENTO", "TENIENTE", "CAPITAN", "MAYOR", "CORONEL"};
        boolean rangoValido = false;
        for (String rango : rangosValidos) {
            if (rango.equalsIgnoreCase(policiaData.getRango())) {
                rangoValido = true;
                break;
            }
        }
        
        if (!rangoValido) {
            String errorMessage = "Rango inválido: " + policiaData.getRango() + 
                                ". Rangos válidos: " + String.join(", ", rangosValidos);
            logger.error(errorMessage);
            throw new InvalidDataException(errorMessage, xmlContent);
        }
    }
}
