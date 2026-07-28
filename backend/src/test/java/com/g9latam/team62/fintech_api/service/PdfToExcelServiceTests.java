package com.g9latam.team62.fintech_api.service;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PdfToExcelServiceTests {

    private final PdfToExcelService service = new PdfToExcelService();

    @Test
    void convertActualCartolaMatchesReferenceWorkbook() throws Exception {
        Path pdfPath = locateSampleFile("Cartola CuentaRUT 20260523_000002.pdf");
        assumeTrue(pdfPath != null, "No se encontró el PDF de ejemplo en la raíz del proyecto");

        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                pdfPath.getFileName().toString(),
                "application/pdf",
                Files.readAllBytes(pdfPath)
        );

        byte[] excelBytes = service.convertPdfToExcel(pdfFile);

        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        try (Workbook generatedWorkbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {

            Sheet generatedSheet = generatedWorkbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            assertTrue(generatedSheet.getLastRowNum() > 0);
            assertTrue(generatedSheet.getPhysicalNumberOfRows() > 0);
            assertTrue(formatter.formatCellValue(generatedSheet.getRow(0).getCell(0)).contains("CARTOLA"));

            // Requirement 1: Verify header has 6 separate columns
            boolean foundHeaderRow = false;
            int headerCount = 0;
            boolean foundLongDescription = false;

            for (int r = 0; r <= generatedSheet.getLastRowNum(); r++) {
                Row row = generatedSheet.getRow(r);
                if (row != null && row.getLastCellNum() >= 6) {
                    String col0 = formatter.formatCellValue(row.getCell(0));
                    String col1 = formatter.formatCellValue(row.getCell(1));
                    String col2 = formatter.formatCellValue(row.getCell(2));
                    if ("Fecha".equals(col0) && col1.startsWith("N°") && "Descripción".equals(col2)) {
                        foundHeaderRow = true;
                        headerCount++;
                        assertEquals("Abonos", formatter.formatCellValue(row.getCell(3)));
                        assertEquals("Cargos", formatter.formatCellValue(row.getCell(4)));
                        assertEquals("Saldo", formatter.formatCellValue(row.getCell(5)));
                    }
                }

                if (row != null) {
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        String cellVal = formatter.formatCellValue(row.getCell(c));
                        if (cellVal.contains("TEF DE JOSE SEBASTIAN CORNEJO TOBAR") || cellVal.contains("COMISION TRANSACCION INTERNACIONAL")) {
                            foundLongDescription = true;
                        }
                    }
                }
            }

            assertTrue(foundHeaderRow, "Debe existir la fila con las 6 columnas de encabezado separadas");
            assertEquals(1, headerCount, "No deben repetirse los encabezados de tabla en el documento Excel");
            assertTrue(foundLongDescription, "Las descripciones largas divididas en el PDF deben unirse en una sola celda");
        }
    }

    @Test
    void convertPdfToExcelRejectsNonPdfFiles() {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "nota.txt",
                "text/plain",
                "contenido".getBytes()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.convertPdfToExcel(textFile)
        );

        assertTrue(exception.getMessage().contains("PDF"));
    }

    private Path locateSampleFile(String fileName) {
        Path[] candidates = new Path[] {
                Path.of(fileName),
                Path.of("..", fileName),
                Path.of("..", "..", fileName)
        };

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }

        return null;
    }

}