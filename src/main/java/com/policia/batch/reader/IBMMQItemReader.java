package com.policia.batch.reader;

import com.policia.batch.exception.InvalidDataException;
import com.policia.batch.exception.MalformedXmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import org.xml.sax.InputSource;

@Component
public class IBMMQItemReader implements ItemReader<String> {

    private static final Logger logger = LoggerFactory.getLogger(IBMMQItemReader.class);
    
    private final JmsTemplate jmsTemplateRta;
    private final JmsTemplate jmsTemplateMas;
    private boolean stopProcessing = false;
    private boolean useRtaQueue = true; // Alternar entre colas

    @Autowired
    public IBMMQItemReader(@Qualifier("jmsTemplateRta") JmsTemplate jmsTemplateRta,
                          @Qualifier("jmsTemplateMas") JmsTemplate jmsTemplateMas) {
        this.jmsTemplateRta = jmsTemplateRta;
        this.jmsTemplateMas = jmsTemplateMas;
    }

    @Override
    public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        
        if (stopProcessing) {
            logger.info("Procesamiento detenido debido a errores previos");
            return null;
        }
        
        try {
            String message = null;
            JmsTemplate currentTemplate = useRtaQueue ? jmsTemplateRta : jmsTemplateMas;
            String queueName = useRtaQueue ? "NOTIFICA.EOL.RTA" : "NOTIFICA.EOL.MAS";
            
            logger.info("Intentando leer mensaje de cola: {}", queueName);
            
            // Leer mensaje de la cola IBM MQ
            message = (String) currentTemplate.receiveAndConvert();
            
            if (message == null) {
                // Si no hay mensajes en la cola actual, cambiar a la otra
                useRtaQueue = !useRtaQueue;
                currentTemplate = useRtaQueue ? jmsTemplateRta : jmsTemplateMas;
                queueName = useRtaQueue ? "NOTIFICA.EOL.RTA" : "NOTIFICA.EOL.MAS";
                
                logger.info("Cambiando a cola: {}", queueName);
                message = (String) currentTemplate.receiveAndConvert();
                
                if (message == null) {
                    logger.info("No hay más mensajes en ninguna cola");
                    return null;
                }
            }
            
            logger.info("Mensaje leído de cola {}: {}", 
                       queueName, 
                       message.length() > 100 ? message.substring(0, 100) + "..." : message);
            
            // Validar que el mensaje no esté vacío
            if (message.trim().isEmpty()) {
                handleInvalidData("Mensaje vacío encontrado en la cola " + queueName, message);
            }
            
            // Validar que el XML esté bien formado
            validateXmlFormat(message);
            
            return message;
            
        } catch (Exception e) {
            logger.error("Error al leer mensaje de la cola: {}", e.getMessage(), e);
            
            if (e instanceof InvalidDataException) {
                stopProcessing = true;
                throw e;
            }
            
            stopProcessing = true;
            throw new InvalidDataException("Error crítico al leer de la cola IBM MQ", "", e);
        }
    }
    
    /**
     * Simula mensajes de las colas para desarrollo
     * En producción, este método no se usa
     */
    private int rtaIndex = 0;
    private int masIndex = 0;
    
    private String simulateQueueMessage(String queueName) {
        // Mensajes simulados con datos policiales reales para testing
        String[] rtaMessages = {
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><policia-data><id>POL123456</id><nombre>Juan Pérez García</nombre><rango>SARGENTO</rango><unidad>Comisaría Central</unidad><estado>ACTIVO</estado></policia-data>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><policia-data><id>POL123457</id><nombre>María González López</nombre><rango>AGENTE</rango><unidad>Unidad Norte</unidad><estado>ACTIVO</estado></policia-data>"
        };
        
        String[] masMessages = {
            "INVALID_XML_MALFORMED", // Este XML mal formado detendrá el batch
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><policia-data><id>POL123458</id><nombre>Carlos López</nombre><rango>CAPITAN</rango><unidad>Unidad Sur</unidad><estado>ACTIVO</estado></policia-data>"
        };
        
        if (queueName.equals("NOTIFICA.EOL.RTA")) {
            if (rtaIndex < rtaMessages.length) {
                return rtaMessages[rtaIndex++];
            }
        } else {
            if (masIndex < masMessages.length) {
                return masMessages[masIndex++];
            }
        }
        
        // Cambiar de cola cuando no hay más mensajes
        useRtaQueue = !useRtaQueue;
        return null;
    }
    
    /**
     * Valida que el XML esté bien formado
     */
    private void validateXmlFormat(String xmlContent) {
        try {
            logger.debug("Validando XML formato: [{}...]", 
                xmlContent.length() > 50 ? xmlContent.substring(0, 50) : xmlContent);
            
            // Limpiar contenido antes de validar
            String cleanedXml = cleanXmlContent(xmlContent);
            
            // Verificar si hubo limpieza
            if (!xmlContent.equals(cleanedXml)) {
                logger.warn("XML contenía prefijos no válidos. Limpiado de {} a {} caracteres", 
                    xmlContent.length(), cleanedXml.length());
                logger.warn("Contenido original (primeros 100 chars): {}", 
                    xmlContent.length() > 100 ? xmlContent.substring(0, 100) : xmlContent);
                logger.warn("Contenido limpiado (primeros 100 chars): {}", 
                    cleanedXml.length() > 100 ? cleanedXml.substring(0, 100) : cleanedXml);
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new InputSource(new StringReader(cleanedXml)));
            
            logger.debug("XML validado correctamente");
            
        } catch (Exception e) {
            String errorDetails = analyzeXmlError(xmlContent, e);
            logger.error("ERROR XML DETALLADO: {}", errorDetails);
            handleInvalidData(errorDetails, xmlContent);
        }
    }
    
    /**
     * Limpia el contenido XML removiendo prefijos comunes no válidos
     */
    private String cleanXmlContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        String cleaned = content.trim();
        
        // Remover prefijos comunes que causan "Content is not allowed in prolog"
        String[] commonPrefixes = {
            "Trx:", "TRX:", "MSG:", "Data:", "XML:", 
            "Response:", "Request:", "Message:"
        };
        
        for (String prefix : commonPrefixes) {
            if (cleaned.startsWith(prefix)) {
                logger.warn("Removiendo prefijo no válido: '{}'", prefix);
                cleaned = cleaned.substring(prefix.length()).trim();
                break;
            }
        }
        
        // Remover BOM (Byte Order Mark) si existe
        if (cleaned.startsWith("\uFEFF")) {
            logger.warn("Removiendo BOM (Byte Order Mark)");
            cleaned = cleaned.substring(1);
        }
        
        // Verificar que comience con '<' (inicio válido de XML)
        if (!cleaned.startsWith("<")) {
            // Buscar el primer '<' y remover todo lo anterior
            int xmlStart = cleaned.indexOf('<');
            if (xmlStart > 0) {
                logger.warn("Removiendo {} caracteres antes del XML válido", xmlStart);
                cleaned = cleaned.substring(xmlStart);
            }
        }
        
        return cleaned;
    }
    
    /**
     * Analiza errores XML para proporcionar diagnóstico detallado
     */
    private String analyzeXmlError(String xmlContent, Exception e) {
        StringBuilder diagnosis = new StringBuilder();
        diagnosis.append("DIAGNÓSTICO XML ERROR:\n");
        diagnosis.append("- Error: ").append(e.getMessage()).append("\n");
        diagnosis.append("- Tipo Exception: ").append(e.getClass().getSimpleName()).append("\n");
        diagnosis.append("- Longitud contenido: ").append(xmlContent.length()).append(" caracteres\n");
        
        // Analizar primeros caracteres
        if (xmlContent.length() > 0) {
            diagnosis.append("- Primer carácter: '").append(xmlContent.charAt(0)).append("' (ASCII: ")
                     .append((int) xmlContent.charAt(0)).append(")\n");
            
            String first20 = xmlContent.length() > 20 ? xmlContent.substring(0, 20) : xmlContent;
            diagnosis.append("- Primeros 20 caracteres: '").append(first20).append("'\n");
            
            // Verificar si empieza con XML válido
            if (!xmlContent.trim().startsWith("<")) {
                diagnosis.append("- PROBLEMA: El contenido no empieza con '<' (no es XML válido)\n");
                int xmlStart = xmlContent.indexOf('<');
                if (xmlStart > 0) {
                    diagnosis.append("- El primer '<' está en posición: ").append(xmlStart).append("\n");
                    diagnosis.append("- Contenido antes de XML: '").append(xmlContent.substring(0, xmlStart)).append("'\n");
                } else {
                    diagnosis.append("- NO SE ENCONTRÓ '<' en todo el contenido\n");
                }
            }
            
            // Detectar caracteres invisibles
            char firstChar = xmlContent.charAt(0);
            if (Character.isISOControl(firstChar) && firstChar != '\n' && firstChar != '\r' && firstChar != '\t') {
                diagnosis.append("- PROBLEMA: Carácter de control invisible al inicio\n");
            }
        }
        
        diagnosis.append("- Contenido completo (100 primeros chars): ");
        if (xmlContent.length() > 100) {
            diagnosis.append(xmlContent.substring(0, 100)).append("...");
        } else {
            diagnosis.append(xmlContent);
        }
        
        return diagnosis.toString();
    }
    
    /**
     * Maneja datos inválidos deteniendo el procesamiento
     */
    private void handleInvalidData(String errorMessage, String invalidData) {
        logger.error("📋 DATOS INVÁLIDOS DETECTADOS - DETENIENDO BATCH");
        logger.error("🔍 Error detallado: {}", errorMessage);
        logger.error("📄 Datos problemáticos guardados para análisis");
        
        // Guardar datos problemáticos en archivo para análisis
        try {
            java.nio.file.Files.write(
                java.nio.file.Paths.get("error-xml-" + System.currentTimeMillis() + ".txt"),
                invalidData.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            logger.error("📁 Archivo de error guardado en directorio actual");
        } catch (Exception e) {
            logger.error("❌ No se pudo guardar archivo de error: {}", e.getMessage());
        }
        
        stopProcessing = true;
        throw new MalformedXmlException(errorMessage, invalidData);
    }
    
    /**
     * Método para verificar si el procesamiento debe detenerse
     */
    public boolean isStopped() {
        return stopProcessing;
    }
    
    /**
     * Método para reiniciar el estado del reader (para testing)
     */
    public void reset() {
        stopProcessing = false;
        useRtaQueue = true;
    }
}
