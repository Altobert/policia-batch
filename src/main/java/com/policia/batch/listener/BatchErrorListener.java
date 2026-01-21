package com.policia.batch.listener;

import com.policia.batch.exception.InvalidDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.stereotype.Component;

@Component
public class BatchErrorListener implements JobExecutionListener, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(BatchErrorListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("=== INICIANDO BATCH POLICIA-XML ===");
        logger.info("Job ID: {}", jobExecution.getId());
        logger.info("Parámetros: {}", jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        logger.info("=== FINALIZANDO BATCH POLICIA-XML ===");
        logger.info("Status: {}", jobExecution.getStatus());
        if (jobExecution.getEndTime() != null && jobExecution.getStartTime() != null) {
            long durationMs = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();
            logger.info("Tiempo de ejecución: {}ms", durationMs);
        }
        
        if (jobExecution.getStatus().isUnsuccessful()) {
            logger.error("JOB FALLIDO - Estado: {}", jobExecution.getStatus());
            
            // Verificar si el fallo fue por datos inválidos
            jobExecution.getAllFailureExceptions().forEach(exception -> {
                if (exception instanceof InvalidDataException) {
                    logger.error("FALLO POR DATOS INVÁLIDOS: {}", exception.getMessage());
                    InvalidDataException invalidDataException = (InvalidDataException) exception;
                    logger.error("Datos problemáticos: {}", invalidDataException.getInvalidData());
                } else {
                    logger.error("Error inesperado: {}", exception.getMessage(), exception);
                }
            });
        } else {
            logger.info("JOB COMPLETADO EXITOSAMENTE");
        }
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        logger.info("--- Iniciando Step: {} ---", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        logger.info("--- Step Completado: {} ---", stepExecution.getStepName());
        logger.info("Items leídos: {}, Procesados: {}, Escritos: {}",
                   stepExecution.getReadCount(),
                   stepExecution.getFilterCount(),
                   stepExecution.getWriteCount());
        
        if (stepExecution.getReadSkipCount() > 0 || stepExecution.getWriteSkipCount() > 0) {
            logger.warn("Items saltados - Lectura: {}, Escritura: {}",
                       stepExecution.getReadSkipCount(),
                       stepExecution.getWriteSkipCount());
        }
        
        if (stepExecution.getStatus().isUnsuccessful()) {
            logger.error("STEP FALLIDO: {}", stepExecution.getExitStatus().getExitDescription());
        }
        
        return stepExecution.getExitStatus();
    }
}
