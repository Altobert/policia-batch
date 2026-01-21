# Proyecto Spring Batch - Policia Batch

## Descripción del Proyecto
Este proyecto implementa un batch de Spring Boot que lee mensajes de una cola IBM MQ, valida XML y procesa datos relacionados con información policial. El sistema incluye validaciones robustas y manejo de errores para detener el procesamiento cuando encuentra datos inválidos.

## Características Principales
- Lectura de mensajes desde IBM MQ
- Validación de XML antes del procesamiento
- Manejo de errores que detiene el batch en caso de datos corruptos
- Configuración de Spring Batch para procesamiento por lotes
- Logging detallado para monitoreo

## Estructura del Proyecto
- `src/main/java/com/policia/batch/` - Código fuente principal
- `src/main/resources/` - Archivos de configuración
- `src/test/java/` - Pruebas unitarias

## Instrucciones de Desarrollo
- Usar Java 17+
- Spring Boot 3.x con Spring Batch
- IBM MQ Client para conectividad
- Validación XML con JAXB
- Manejo de excepciones personalizado