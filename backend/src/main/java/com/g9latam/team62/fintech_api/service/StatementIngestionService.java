package com.g9latam.team62.fintech_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g9latam.team62.fintech_api.dto.ClassificationResult;
import com.g9latam.team62.fintech_api.dto.StatementIngestionResult;
import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.CategoryMethod;
import com.g9latam.team62.fintech_api.model.Currency;
import com.g9latam.team62.fintech_api.model.LinkStatus;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.model.TransactionSource;
import com.g9latam.team62.fintech_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class StatementIngestionService {

    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final CategoryClassifierService classifierService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StatementIngestionService(UserRepository userRepository,
                                     TransactionService transactionService,
                                     CategoryClassifierService classifierService) {
        this.userRepository = userRepository;
        this.transactionService = transactionService;
        this.classifierService = classifierService;
    }

    public StatementIngestionResult ingestStatement(MultipartFile file, Long userId, Integer defaultYear, String country) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("El usuario " + userId + " no existe");
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo enviado está vacío");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "statement";
        File tempFile = null;

        try {
            // Guardar archivo temporalmente para que el script Python procese su ruta
            tempFile = File.createTempFile("cartola_", "_" + originalFilename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Invocación del script procesar_cartola_cli.py vía ProcessBuilder
            List<String> command = new ArrayList<>();
            // Intentar detectar ejecutable de python (python3 / python)
            String pythonExec = System.getProperty("os.name").toLowerCase().contains("win") ? "python" : "python3";
            command.add(pythonExec);
            
            // Ruta del script dentro del proyecto
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

            String detectedBank = root.path("banco").asText(null);

            // Parsear transacciones válidas obtenidas
            List<Transaction> createdTransactions = new ArrayList<>();
            JsonNode txArray = root.path("transacciones");
            if (txArray.isArray()) {
                for (JsonNode txNode : txArray) {
                    String desc = txNode.path("descripcion").asText("");
                    double rawAmount = txNode.path("monto").asDouble(0.0);
                    BigDecimal amount = BigDecimal.valueOf(Math.abs(rawAmount));
                    if (amount.signum() <= 0) {
                        continue; // Evitar transacciones en 0
                    }

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
            }

            List<String> warnings = new ArrayList<>();
            JsonNode warningsNode = root.path("avisos");
            if (warningsNode.isArray()) {
                for (JsonNode w : warningsNode) {
                    warnings.add(w.asText());
                }
            }

            return new StatementIngestionResult(
                    "ok",
                    root.path("archivo").asText(originalFilename),
                    root.path("pais").asText(country != null ? country : "CL"),
                    root.path("anio").asInt(LocalDate.now().getYear()),
                    root.path("filas_crudas").asInt(0),
                    root.path("filas_validas").asInt(0),
                    root.path("filas_descartadas").asInt(0),
                    warnings,
                    createdTransactions
            );

        } catch (Exception e) {
            throw new RuntimeException("Fallo al ejecutar la ingesta de cartola: " + e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete(); // Borrar archivo temporal siempre
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
