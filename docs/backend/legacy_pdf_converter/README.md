# Módulo de Conversión PDF a Excel (Legacy)

Esta carpeta contiene el respaldo de las clases Java utilizadas para la conversión directa de archivos PDF a Excel (`.xlsx`) mediante Apache PDFBox y Apache POI:

- `PdfController.java`: Endpoint `/api/converter/pdf-to-excel`
- `PdfToExcelService.java`: Servicio de parseo y generación de libro Excel
- `PdfToExcelServiceTests.java`: Tests unitarios de validación de conversión

## Razón de Resguardo
La ingesta de cartolas bancarias (PDF, Excel y CSV) fue unificada en el backend bajo `StatementIngestionService` utilizando el script de Python `procesar_cartola_cli.py`. Estas clases se respaldan aquí por si se requiere reactivar la conversión directa standalone de PDF a Excel en Java en el futuro.
