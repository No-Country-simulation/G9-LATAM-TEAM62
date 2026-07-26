package com.g9latam.team62.fintech_api.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import com.g9latam.team62.fintech_api.service.PdfToExcelService;

@RestController
@RequestMapping("/api/converter")
public class PdfController {

    @Autowired
    private PdfToExcelService pdfToExcelService;

    @PostMapping("/pdf-to-excel")
    public ResponseEntity<byte[]> convert(@RequestParam("file") MultipartFile file) {
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

