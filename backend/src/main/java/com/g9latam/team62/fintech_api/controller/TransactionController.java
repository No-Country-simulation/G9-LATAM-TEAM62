package com.g9latam.team62.fintech_api.controller;

import com.g9latam.team62.fintech_api.dto.CategoryCorrectionRequest;
import com.g9latam.team62.fintech_api.dto.TransactionRequest;
import com.g9latam.team62.fintech_api.dto.TransactionResponse;
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
import org.springframework.lang.NonNull;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;

import com.g9latam.team62.fintech_api.security.AuthorizationHelper;
import java.security.Principal;
import java.util.Collection;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transacciones", description = "Endpoints para crear, consultar, actualizar y eliminar transacciones financieras")
public class TransactionController {

    private final TransactionService service;
    private final com.g9latam.team62.fintech_api.service.StatementIngestionService statementIngestionService;
    private final AuthorizationHelper authorizationHelper;

    public TransactionController(TransactionService service,
                                 com.g9latam.team62.fintech_api.service.StatementIngestionService statementIngestionService,
                                 AuthorizationHelper authorizationHelper) {
        this.service = service;
        this.statementIngestionService = statementIngestionService;
        this.authorizationHelper = authorizationHelper;
    }

    @GetMapping
    @Operation(summary = "Obtener todas las transacciones del usuario", description = "Retorna una colección de transacciones pertenecientes únicamente al usuario autenticado actual.")
    public Collection<TransactionResponse> findAll(
            @Parameter(description = "ID del usuario para filtrar las transacciones (debe coincidir con el usuario autenticado)")
            @RequestParam(required = false) Long userId,
            Principal principal) {
        com.g9latam.team62.fintech_api.model.User authUser = authorizationHelper.getAuthenticatedUser(principal);
        if (userId != null) {
            authorizationHelper.verifyUserOwnership(principal, userId);
            return TransactionResponse.fromEntities(service.findByUserId(userId));
        }
        // Si no se provee userId, se retorna por defecto las del usuario autenticado actual
        return TransactionResponse.fromEntities(service.findByUserId(authUser.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener transacción por ID", description = "Retorna la transacción correspondiente al ID provisto, validando que pertenezca al usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transacción encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transacción no encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<TransactionResponse> findById(@PathVariable @NonNull Long id, Principal principal) {
        Transaction tx = service.findById(id)
                .orElse(null);
        if (tx == null) {
            return ResponseEntity.notFound().build();
        }
        authorizationHelper.verifyUserOwnership(principal, tx.getUserId());
        return ResponseEntity.ok(TransactionResponse.fromEntity(tx));
    }

    @PostMapping
    @Operation(summary = "Registrar una nueva transacción", description = "Crea una transacción financiera para el usuario autenticado. Se verifica que el user_id corresponda al usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transacción registrada con éxito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request, Principal principal) {
        authorizationHelper.verifyUserOwnership(principal, request.userId());
        Transaction created = service.create(request.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.fromEntity(created));
    }

    @PostMapping("/manual")
    @Operation(summary = "Registrar transacción manual", description = "Registra una entrada manual de dinero, validando que pertenezca al usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transacción manual registrada con éxito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<TransactionResponse> createManual(@Valid @RequestBody com.g9latam.team62.fintech_api.dto.ManualTransactionRequest request, Principal principal) {
        authorizationHelper.verifyUserOwnership(principal, request.userId());
        Transaction created = service.createManual(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.fromEntity(created));
    }

    @PostMapping(value = "/upload-statement", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir e ingestar cartola bancaria", description = "Recibe una cartola, valida que el userId pertenezca al usuario autenticado, procesa e ingesta transacciones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cartola procesada e ingesta completada con éxito"),
            @ApiResponse(responseCode = "400", description = "Error procesando el archivo de la cartola"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<com.g9latam.team62.fintech_api.dto.StatementIngestionResult> uploadStatement(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("userId") @NonNull Long userId,
            @RequestParam(value = "defaultYear", required = false) Integer defaultYear,
            @RequestParam(value = "country", required = false) String country,
            Principal principal) {
        authorizationHelper.verifyUserOwnership(principal, userId);
        com.g9latam.team62.fintech_api.dto.StatementIngestionResult result =
                statementIngestionService.ingestStatement(file, userId, defaultYear, country);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar transacción", description = "Actualiza toda la información de una transacción existente, validando que pertenezca al usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transacción actualizada con éxito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transacción no encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<TransactionResponse> update(@PathVariable @NonNull Long id, @Valid @RequestBody TransactionRequest request, Principal principal) {
        Transaction tx = service.findById(id).orElse(null);
        if (tx == null) {
            return ResponseEntity.notFound().build();
        }
        authorizationHelper.verifyUserOwnership(principal, tx.getUserId());
        authorizationHelper.verifyUserOwnership(principal, request.userId());
        return ResponseEntity.ok(TransactionResponse.fromEntity(service.update(id, request.toEntity())));
    }

    @PutMapping("/{id}/category")
    @Operation(summary = "Corregir categoría de transacción",
               description = "Permite al usuario corregir la categoría asignada automáticamente, validando la propiedad de la transacción.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría actualizada con éxito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transacción no encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    public ResponseEntity<TransactionResponse> updateCategory(
            @Parameter(description = "ID de la transacción a corregir") @PathVariable @NonNull Long id,
            @Valid @RequestBody CategoryCorrectionRequest request,
            Principal principal) {
        Transaction tx = service.findById(id).orElse(null);
        if (tx == null) {
            return ResponseEntity.notFound().build();
        }
        authorizationHelper.verifyUserOwnership(principal, tx.getUserId());
        Transaction updated = service.updateCategory(id, request.category());
        return ResponseEntity.ok(TransactionResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar transacción", description = "Elimina físicamente la transacción con el ID especificado, validando la propiedad del recurso.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transacción eliminada con éxito"),
            @ApiResponse(responseCode = "404", description = "Transacción no encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<Void> delete(@PathVariable @NonNull Long id, Principal principal) {
        Transaction tx = service.findById(id).orElse(null);
        if (tx == null) {
            return ResponseEntity.notFound().build();
        }
        authorizationHelper.verifyUserOwnership(principal, tx.getUserId());
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

