package com.g9latam.team62.fintech_api.controller;

import com.g9latam.team62.fintech_api.model.Recommendation;
import com.g9latam.team62.fintech_api.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/api/recommendations")
@Tag(name = "Recomendaciones", description = "Endpoints para la gestión de recomendaciones financieras para los usuarios")
public class RecommendationController {

    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Obtener todas las recomendaciones", description = "Retorna una colección de recomendaciones financieras. Opcionalmente se puede filtrar por el ID del usuario.")
    public Collection<Recommendation> findAll(
            @Parameter(description = "ID del usuario para filtrar las recomendaciones")
            @RequestParam(required = false) Long userId) {
        if (userId != null) {
            return service.findByUserId(userId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener recomendación por ID", description = "Retorna la recomendación correspondiente al ID provisto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recomendación encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Recommendation.class))),
            @ApiResponse(responseCode = "404", description = "Recomendación no encontrada")
    })
    public ResponseEntity<Recommendation> findById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Crear recomendación", description = "Registra una nueva recomendación financiera en el sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recomendación creada con éxito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Recommendation.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    public ResponseEntity<Recommendation> create(@Valid @RequestBody Recommendation recommendation) {
        Recommendation created = service.create(recommendation);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar recomendación", description = "Elimina físicamente la recomendación con el ID especificado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Recomendación eliminada con éxito"),
            @ApiResponse(responseCode = "404", description = "Recomendación no encontrada")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
