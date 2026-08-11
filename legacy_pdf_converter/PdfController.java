package com.g9latam.team62.fintech_api.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.Parameter;

import java.io.IOException;

import com.g9latam.team62.fintech_api.service.PdfToExcelService;

@RestController
@RequestMapping("/api/converter")
@Tag(name = "Conversor de PDF", description = "Endpoints para la conversión de archivos de formato PDF a otros formatos compatibles")
public class PdfController {

    private final PdfToExcelService pdfToExcelService;

    public PdfController(PdfToExcelService pdfToExcelService) {
        this.pdfToExcelService = pdfToExcelService;
    }

    @PostMapping(value = "/pdf-to-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Convertir PDF a Excel", description = "Recibe un archivo PDF, extrae su contenido y devuelve un archivo Excel (.xlsx).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archivo convertido con éxito. Retorna el archivo Excel.",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "400", description = "El archivo enviado no existe, está vacío o no es un PDF válido"),
            @ApiResponse(responseCode = "500", description = "Error interno durante la conversión del archivo")
    })
    public ResponseEntity<byte[]> convert(
            @Parameter(description = "Archivo PDF a convertir", required = true)
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty() || !MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType())) {
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] excelContent = pdfToExcelService.convertPdfToExcel(file);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", buildDownloadName(file.getOriginalFilename()));

            return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String buildDownloadName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "archivo_convertido.xlsx";
        }

        int lastDot = originalFileName.lastIndexOf('.');
        String baseName = lastDot > 0 ? originalFileName.substring(0, lastDot) : originalFileName;
        return baseName + ".xlsx";
    }
}
