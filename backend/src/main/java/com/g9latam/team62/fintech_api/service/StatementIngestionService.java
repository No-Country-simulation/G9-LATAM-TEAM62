package com.g9latam.team62.fintech_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g9latam.team62.fintech_api.dto.ClassificationResult;
import com.g9latam.team62.fintech_api.dto.StatementIngestionResult;
import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.CategoryMethod;
import com.g9latam.team62.fintech_api.model.Currency;
import com.g9latam.team62.fintech_api.model.LinkStatus;
import com.g9latam.team62.fintech_api.model.PaymentMethod;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.model.TransactionSource;
import com.g9latam.team62.fintech_api.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class StatementIngestionService {

    private static final Logger log = LoggerFactory.getLogger(StatementIngestionService.class);

    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final CategoryClassifierService classifierService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Firmas de bytes estándar (Magic Bytes)
    private static final byte[] PDF_HEADER = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final byte[] ZIP_HEADER = new byte[]{0x50, 0x4B, 0x03, 0x04}; // PK.. (Excel .xlsx / ZIP)
    private static final byte[] OLE2_HEADER = new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}; // Excel clásico .xls
    private static final byte[] EXE_HEADER = new byte[]{0x4D, 0x5A}; // MZ (Ejecutables Windows)
    private static final byte[] ELF_HEADER = new byte[]{0x7F, 0x45, 0x4C, 0x46}; // ELF (Binarios Linux)

    public StatementIngestionService(UserRepository userRepository,
                                     TransactionService transactionService,
                                     CategoryClassifierService classifierService) {
        this.userRepository = userRepository;
        this.transactionService = transactionService;
        this.classifierService = classifierService;
    }

    public StatementIngestionResult ingestStatement(MultipartFile file, @NonNull Long userId, Integer defaultYear, String country) {
        validateUserAndFile(userId, file);
        
        File tempFile = null;
        try {
            validateFileSignature(file);
            tempFile = createSecureTempFile(file);
            JsonNode rootNode = executePythonScript(tempFile, defaultYear, country);
            List<Transaction> createdTransactions = processAndSaveTransactions(rootNode, userId);

            return buildResult(rootNode, file.getOriginalFilename(), country, createdTransactions);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e; 
        } catch (Exception e) {
            log.error("Fallo inesperado al ejecutar la ingesta de cartola", e);
            throw new RuntimeException("Fallo al ejecutar la ingesta de cartola: " + e.getMessage(), e);
        } finally {
            cleanUpTempFile(tempFile);
        }
    }

    private void validateUserAndFile(@NonNull Long userId, MultipartFile file) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("El usuario " + userId + " no existe");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo enviado está vacío");
        }
    }

    private void validateFileSignature(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[4];
            int read = is.read(header);
            
            if (read < 4) {
                throw new IllegalArgumentException("El archivo es demasiado pequeño o está corrupto.");
            }

            boolean isPdf = Arrays.equals(header, PDF_HEADER);
            boolean isXlsx = Arrays.equals(header, ZIP_HEADER);
            boolean isXls = Arrays.equals(header, OLE2_HEADER);

            boolean isExecutable = (header[0] == EXE_HEADER[0] && header[1] == EXE_HEADER[1]) || Arrays.equals(header, ELF_HEADER);
            String filename = file.getOriginalFilename();
            boolean isCsv = filename != null 
                    && filename.toLowerCase().endsWith(".csv") 
                    && !isExecutable 
                    && header[0] != 0x00;

            if (!isPdf && !isXlsx && !isXls && !isCsv) {
                throw new IllegalArgumentException("Tipo de archivo no permitido. Solo se aceptan PDFs, planillas Excel (.xlsx/.xls) o archivos CSV legítimos.");
            }
        }
    }

    private File createSecureTempFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        String originalFilename = (filename != null && !filename.isBlank()) ? filename : "statement.tmp";
        String cleanFilename = UUID.randomUUID().toString() + "_" + StringUtils.cleanPath(originalFilename);
        
        File tempFile = File.createTempFile("cartola_", cleanFilename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return tempFile;
    }

    private JsonNode executePythonScript(File tempFile, Integer defaultYear, String country) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        String pythonExec = System.getProperty("os.name").toLowerCase().contains("win") ? "python" : "python3";
        command.add(pythonExec);
        
        File scriptFile = new File("backend/scripts/procesar_cartola_cli.py");
        if (!scriptFile.exists()) {
            scriptFile = new File("scripts/procesar_cartola_cli.py");
        }
        command.add(scriptFile.getAbsolutePath());
        command.add(tempFile.getAbsolutePath());

        if (defaultYear != null) {
            command.add("--anio-defecto");
            command.add(defaultYear.toString());
        }
        if (country != null && !country.isBlank()) {
            command.add("--pais");
            command.add(country);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        String jsonOutput;
        try (InputStream stdout = process.getInputStream()) {
            jsonOutput = new String(stdout.readAllBytes(), StandardCharsets.UTF_8);
        }

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("El procesamiento de la cartola excedió el tiempo límite de 30 segundos.");
        }

        int exitCode = process.exitValue();
        JsonNode root = objectMapper.readTree(jsonOutput);

        String status = root.path("status").asText();
        if (exitCode != 0 || "error".equalsIgnoreCase(status)) {
            String errorMsg = root.path("mensaje").asText("Error desconocido procesando la cartola bancaria");
            throw new IllegalArgumentException("Error procesando la cartola: " + errorMsg);
        }

        return root;
    }

    private List<Transaction> processAndSaveTransactions(JsonNode root, Long userId) {
        List<Transaction> createdTransactions = new ArrayList<>();
        String detectedBank = root.path("banco").asText(null);
        JsonNode txArray = root.path("transacciones");

        if (!txArray.isArray()) return createdTransactions;

        for (JsonNode txNode : txArray) {
            String desc = txNode.path("descripcion").asText("");
            double rawAmount = txNode.path("monto").asDouble(0.0);
            BigDecimal amount = BigDecimal.valueOf(Math.abs(rawAmount));
            
            if (amount.signum() <= 0) continue;

            String fechaStr = txNode.path("fecha").asText(null);
            LocalDate date = fechaStr != null ? LocalDate.parse(fechaStr) : LocalDate.now();
            Double saldo = txNode.hasNonNull("saldo") ? txNode.path("saldo").asDouble() : null;

            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setDescription(desc);
            transaction.setAmount(amount);
            transaction.setDate(date);
            transaction.setBalanceAfter(saldo != null ? BigDecimal.valueOf(saldo) : null);
            transaction.setCurrency(new Currency(1L, "CLP"));
            transaction.setSource(TransactionSource.BANK);
            transaction.setLinkStatus(LinkStatus.UNLINKED);
            transaction.setBankName(detectedBank);
            transaction.setPaymentMethod(PaymentMethod.DEBIT);

            String opNum = txNode.hasNonNull("nro_operacion") ? txNode.path("nro_operacion").asText(null) : null;
            transaction.setOperationNumber((opNum != null && opNum.isBlank()) ? null : opNum);

            // Clasificación por 4 niveles
            if (desc != null && !desc.isBlank()) {
                ClassificationResult classification = classifierService.classify(desc);
                transaction.setCategory(classification.category());
                transaction.setCategoryMethod(parseCategoryMethod(classification.method()));
                transaction.setCategoryConfidence(classification.confidence());
            } else {
                transaction.setCategory(Category.OTHER_EXPENSE);
            }

            createdTransactions.add(transactionService.create(transaction));
        }
        return createdTransactions;
    }

    private StatementIngestionResult buildResult(JsonNode root, String originalFilename, String country, List<Transaction> createdTransactions) {
        List<String> warnings = new ArrayList<>();
        JsonNode warningsNode = root.path("avisos");
        if (warningsNode.isArray()) {
            warningsNode.forEach(w -> warnings.add(w.asText()));
        }

        return new StatementIngestionResult(
                "ok",
                root.path("archivo").asText(originalFilename != null ? originalFilename : "statement"),
                root.path("pais").asText(country != null ? country : "CL"),
                root.path("anio").asInt(LocalDate.now().getYear()),
                root.path("filas_crudas").asInt(0),
                root.path("filas_validas").asInt(0),
                root.path("filas_descartadas").asInt(0),
                warnings,
                createdTransactions
        );
    }

    private void cleanUpTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            if (!tempFile.delete()) {
                log.warn("No se pudo eliminar el archivo temporal: {}", tempFile.getAbsolutePath());
            }
        }
    }

    private CategoryMethod parseCategoryMethod(String methodStr) {
        if (methodStr == null) return CategoryMethod.FALLBACK;
        try {
            return CategoryMethod.valueOf(methodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CategoryMethod.FALLBACK;
        }
    }
}
