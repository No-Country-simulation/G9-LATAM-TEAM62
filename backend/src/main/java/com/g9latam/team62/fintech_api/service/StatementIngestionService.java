package com.g9latam.team62.fintech_api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g9latam.team62.fintech_api.dto.StatementIngestionResult;
import com.g9latam.team62.fintech_api.dto.TransactionResponse;
import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.CategoryMethod;
import com.g9latam.team62.fintech_api.model.Currency;
import com.g9latam.team62.fintech_api.model.LinkStatus;
import com.g9latam.team62.fintech_api.model.PaymentMethod;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.model.TransactionSource;
import com.g9latam.team62.fintech_api.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class StatementIngestionService {

    private static final Logger log = LoggerFactory.getLogger(StatementIngestionService.class);

    private static final int PARSER_TIMEOUT_SECONDS = 30;

    private static final String PARSER_UNAVAILABLE =
            "El parser de cartolas no está disponible en este despliegue. "
          + "Se esperaba el script en \"%s\"; revisa la propiedad statement.parser.script.";

    /**
     * Ubicación del script de ingesta. Por defecto se lee del classpath, donde el build lo
     * deja empaquetado dentro del jar (ver maven-resources-plugin en pom.xml), de modo que
     * la ruta no depende del directorio desde el que se ejecute la aplicación. Admite
     * cualquier prefijo de Spring: {@code classpath:...} o {@code file:...}.
     */
    @Value("${statement.parser.script:classpath:scripts/procesar_cartola_cli.py}")
    private Resource scriptResource;

    /** Intérprete a usar. Vacío deja que se deduzca del sistema operativo. */
    @Value("${statement.parser.python:}")
    private String configuredPython;

    /** Ruta ya resuelta en disco, o {@code null} si el script no se pudo localizar al arrancar. */
    private Path scriptPath;

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

    /**
     * Deja el script accesible como archivo antes de la primera petición. Dentro del jar es una
     * entrada comprimida y no una ruta del sistema de archivos, así que hay que extraerlo; se
     * hace una sola vez al arrancar y no en cada subida.
     *
     * <p>Un fallo aquí no impide arrancar: solo se pierde la ingesta de cartolas, y el resto de
     * la API sigue siendo útil. Queda registrado en el log y la petición correspondiente lo
     * informa con el mismo texto.
     */
    @PostConstruct
    void resolveParserScript() {
        try {
            if (!scriptResource.exists()) {
                log.error(PARSER_UNAVAILABLE.formatted(scriptResource));
                return;
            }
            try {
                // Classpath explotado o ruta file:, utilizable tal cual.
                scriptPath = scriptResource.getFile().toPath();
            } catch (IOException insideJar) {
                Path extracted = Files.createTempFile("procesar_cartola_", ".py");
                try (InputStream in = scriptResource.getInputStream()) {
                    Files.copy(in, extracted, StandardCopyOption.REPLACE_EXISTING);
                }
                extracted.toFile().deleteOnExit();
                scriptPath = extracted;
            }
            log.info("Parser de cartolas resuelto en {}", scriptPath);
        } catch (IOException e) {
            log.error(PARSER_UNAVAILABLE.formatted(scriptResource), e);
        }
    }

    public StatementIngestionResult ingestStatement(MultipartFile file, @NonNull Long userId, Integer defaultYear, String country) {
        if (scriptPath == null) {
            throw new IllegalStateException(PARSER_UNAVAILABLE.formatted(scriptResource));
        }
        validateUserAndFile(userId, file);
        
        File tempFile = null;
        try {
            validateFileSignature(file);
            tempFile = createSecureTempFile(file);
            JsonNode rootNode = executePythonScript(tempFile, defaultYear, country);
            List<Transaction> createdTransactions = processAndSaveTransactions(rootNode, userId, country);

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
        command.add(pythonExecutable());
        command.add(scriptPath.toAbsolutePath().toString());
        command.add(tempFile.getAbsolutePath());

        if (defaultYear != null) {
            command.add("--anio-defecto");
            command.add(defaultYear.toString());
        }
        if (country != null && !country.isBlank()) {
            command.add("--pais");
            command.add(country);
        }

        Process process = new ProcessBuilder(command).start();

        // Los dos flujos se drenan en paralelo. Leer uno hasta el final antes de tocar el otro
        // bloquea al proceso hijo en cuanto llena el buffer de la tubería que nadie está
        // vaciando, y en ese estado el timeout de abajo ni siquiera llega a evaluarse.
        CompletableFuture<String> stdout = readAsync(process.getInputStream());
        CompletableFuture<String> stderr = readAsync(process.getErrorStream());

        if (!process.waitFor(PARSER_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException(
                    "El procesamiento de la cartola excedió el tiempo límite de "
                  + PARSER_TIMEOUT_SECONDS + " segundos.");
        }

        int exitCode = process.exitValue();
        String jsonOutput = stdout.join();
        String errorOutput = stderr.join();

        // El traceback completo va al log y nunca al cliente: ahí están las rutas del servidor.
        if (!errorOutput.isBlank()) {
            log.warn("El parser de cartolas escribió en stderr (código {}):\n{}", exitCode, errorOutput);
        }

        JsonNode root = tryParseJson(jsonOutput);
        String status = root != null ? root.path("status").asText("") : "";

        if (exitCode != 0 || "error".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException(
                    "Error procesando la cartola: " + describeFailure(root, errorOutput, exitCode));
        }
        if (root == null) {
            log.error("El parser terminó con código 0 pero su salida no es JSON:\n{}", jsonOutput);
            throw new IllegalStateException(
                    "El parser de cartolas terminó correctamente pero no devolvió un JSON válido.");
        }

        return root;
    }

    private String pythonExecutable() {
        if (configuredPython != null && !configuredPython.isBlank()) {
            return configuredPython;
        }
        return System.getProperty("os.name").toLowerCase().contains("win") ? "python" : "python3";
    }

    private CompletableFuture<String> readAsync(InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream in = stream) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.warn("No se pudo leer la salida del parser de cartolas", e);
                return "";
            }
        });
    }

    private JsonNode tryParseJson(String output) {
        if (output == null || output.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(output);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Arma el mensaje que verá quien subió la cartola. El script reporta sus errores previstos
     * como JSON en {@code mensaje}; cuando muere antes de llegar a imprimirlo —dependencia que
     * falta, archivo ilegible— lo único que queda es el traceback, cuya última línea es
     * justamente la que nombra la causa.
     */
    private String describeFailure(JsonNode root, String errorOutput, int exitCode) {
        if (root != null) {
            String reported = root.path("mensaje").asText("");
            if (!reported.isBlank()) {
                return reported;
            }
        }
        String lastLine = errorOutput.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .reduce((first, second) -> second)
                .orElse("");
        if (!lastLine.isBlank()) {
            return lastLine;
        }
        return "el parser terminó con código " + exitCode + " sin describir el error";
    }

    private List<Transaction> processAndSaveTransactions(JsonNode root, Long userId, String requestedCountry) {
        List<Transaction> createdTransactions = new ArrayList<>();
        String detectedBank = root.path("banco").asText(null);
        String detectedCountry = root.path("pais").asText(null);
        String currencyCode = resolveCurrencyCodeForCountry(detectedCountry, requestedCountry);
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
            
            Currency currency = new Currency();
            currency.setNameCurrency(currencyCode);
            transaction.setCurrency(currency);

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
                transaction.setCategoryMethod(classification.method());
                transaction.setCategoryConfidence(classification.confidence());
            } else {
                transaction.setCategory(Category.OTHER_EXPENSE);
                transaction.setCategoryMethod(CategoryMethod.FALLBACK);
            }

            createdTransactions.add(transactionService.create(transaction));
        }
        return createdTransactions;
    }

    private String resolveCurrencyCodeForCountry(String detectedCountry, String requestedCountry) {
        String country = (detectedCountry != null && !detectedCountry.isBlank())
                ? detectedCountry.trim().toUpperCase()
                : ((requestedCountry != null && !requestedCountry.isBlank()) ? requestedCountry.trim().toUpperCase() : "CL");

        return switch (country) {
            case "CL" -> "CLP";
            case "AR" -> "ARS";
            case "MX" -> "MXN";
            case "CO" -> "COP";
            case "PE" -> "PEN";
            case "BR" -> "BRL";
            case "UY" -> "UYU";
            case "PY" -> "PYG";
            case "BO" -> "BOB";
            case "CR" -> "CRC";
            case "DO" -> "DOP";
            case "GT" -> "GTQ";
            case "HN" -> "HNL";
            case "NI" -> "NIO";
            case "VE" -> "VES";
            case "PA" -> "PAB";
            case "US" -> "USD";
            case "EU", "ES" -> "EUR";
            default -> "CLP";
        };
    }

    private StatementIngestionResult buildResult(JsonNode root, String originalFilename, String country, List<Transaction> createdTransactions) {
        List<String> warnings = new ArrayList<>();
        JsonNode warningsNode = root.path("avisos");
        if (warningsNode.isArray()) {
            warningsNode.forEach(w -> warnings.add(w.asText()));
        }

        String effectiveCountry = root.hasNonNull("pais") && !root.path("pais").asText().isBlank()
                ? root.path("pais").asText()
                : (country != null && !country.isBlank() ? country : "CL");

        return new StatementIngestionResult(
                "ok",
                root.path("archivo").asText(originalFilename != null ? originalFilename : "statement"),
                effectiveCountry,
                root.path("anio").asInt(LocalDate.now().getYear()),
                root.path("filas_crudas").asInt(0),
                root.path("filas_validas").asInt(0),
                root.path("filas_descartadas").asInt(0),
                warnings,
                TransactionResponse.fromEntities(createdTransactions)
        );
    }

    private void cleanUpTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            if (!tempFile.delete()) {
                log.warn("No se pudo eliminar el archivo temporal: {}", tempFile.getAbsolutePath());
            }
        }
    }

}
