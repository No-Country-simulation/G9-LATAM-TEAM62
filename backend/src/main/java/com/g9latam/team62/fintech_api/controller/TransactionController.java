package com.g9latam.team62.fintech_api.controller;

import com.g9latam.team62.fintech_api.dto.CategoryCorrectionRequest;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.Collection;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transacciones", description = "Endpoints para crear, consultar, actualizar y eliminar transacciones financieras")
public class TransactionController {

    private final TransactionService service;
    private final com.g9latam.team62.fintech_api.service.StatementIngestionService statementIngestionService;

    public TransactionController(TransactionService service,
                                 com.g9latam.team62.fintech_api.service.StatementIngestionService statementIngestionService) {
        this.service = service;
        this.statementIngestionService = statementIngestionService;
    }

    @GetMapping
    @Operation(summary = "Obtener todas las transacciones", description = "Retorna una colección de transacciones. Opcionalmente se puede filtrar por el ID del usuario.")
    public Collection<Transaction> findAll(
            @Parameter(description = "ID del usuario para filtrar las transacciones")
            @RequestParam(required = false) Long userId) {
        if (userId != null) {
            return service.findByUserId(userId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener transacción por ID", description = "Retorna la transacción correspondiente al ID provisto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transacción encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Transaction.class))),
            @ApiResponse(responseCode = "404", description = "Transacción no encontrada")
    })
    public ResponseEntity<Transaction> findById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Registrar una nueva transacción", description = "Crea una transacción financiera. Si incluye descripción, la categoría se asigna automáticamente mediante el clasificador jerárquico de 4 niveles.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transacción registrada con éxito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Transaction.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    public ResponseEntity<Transaction> create(@Valid @RequestBody Transaction transaction) {
        Transaction created = service.create(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/manual")
    @Operation(summary = "Registrar transacción manual", description = "Registra una entrada manual de dinero (efectivo/débito) asignando la fecha de hoy.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transacción manual registrada con éxito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Transaction.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    public ResponseEntity<Transaction> createManual(@Valid @RequestBody com.g9latam.team62.fintech_api.dto.ManualTransactionRequest request) {
        Transaction created = service.createManual(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(value = "/upload-statement", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir e ingestar cartola bancaria", description = "Recibe un archivo (Excel, CSV o PDF) de cartola bancaria, invoca el script Python de limpieza y guarda las transacciones clasificadas automáticamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cartola procesada e ingesta completada con éxito"),
            @ApiResponse(responseCode = "400", description = "Error procesando el archivo de la cartola")
    })
    public ResponseEntity<com.g9latam.team62.fintech_api.dto.StatementIngestionResult> uploadStatement(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "defaultYear", required = false) Integer defaultYear,
            @RequestParam(value = "country", required = false) String country) {
        com.g9latam.team62.fintech_api.dto.StatementIngestionResult result =
                statementIngestionService.ingestStatement(file, userId, defaultYear, country);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar transacción", description = "Actualiza toda la información de una transacción existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transacción actualizada con éxito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Transaction.class))),
            @ApiResponse(responseCode = "404", description = "Transacción no encontrada")
    })
    public ResponseEntity<Transaction> update(@PathVariable Long id, @Valid @RequestBody Transaction transaction) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.update(id, transaction));
    }

    @PutMapping("/{id}/category")
    @Operation(summary = "Corregir categoría de transacción",
               description = "Permite al usuario corregir la categoría asignada automáticamente. "
                           + "La corrección se guarda como aprendizaje colaborativo para beneficiar "
                           + "futuras clasificaciones de todos los usuarios.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría actualizada con éxito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Transaction.class))),
            @ApiResponse(responseCode = "404", description = "Transacción no encontrada"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    public ResponseEntity<Transaction> updateCategory(
            @Parameter(description = "ID de la transacción a corregir") @PathVariable Long id,
            @Valid @RequestBody CategoryCorrectionRequest request) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Transaction updated = service.updateCategory(id, request.category());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar transacción", description = "Elimina físicamente la transacción con el ID especificado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transacción eliminada con éxito"),
            @ApiResponse(responseCode = "404", description = "Transacción no encontrada")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

