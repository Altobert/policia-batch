package com.policia.batch.writer;

import com.policia.batch.model.PoliciaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Writer que procesa los datos validados de la cola IBM MQ
 * Por ahora solo logea - sin base de datos
 */
@Component
public class PoliciaDataWriter implements ItemWriter<PoliciaData> {
    
    private static final Logger logger = LoggerFactory.getLogger(PoliciaDataWriter.class);
    
    @Override
    public void write(List<? extends PoliciaData> items) throws Exception {
        logger.info("=== Procesando {} elementos de la cola IBM MQ ===", items.size());
        
        for (PoliciaData item : items) {
            logger.info("✅ Datos procesados correctamente:");
            logger.info("   ID: {}", item.getId());
            logger.info("   Nombre: {}", item.getNombre());
            logger.info("   Rango: {}", item.getRango());
            logger.info("   Unidad: {}", item.getUnidad());
            logger.info("   Estado: {}", item.getEstado());
            logger.info("   ----------------------------------------");
        }
        
        logger.info("=== ✅ Batch completado exitosamente - {} elementos procesados ===", items.size());
    }
}
