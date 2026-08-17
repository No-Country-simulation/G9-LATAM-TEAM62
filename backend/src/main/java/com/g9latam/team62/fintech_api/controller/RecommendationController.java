package com.g9latam.team62.fintech_api.controller;

import com.g9latam.team62.fintech_api.model.Recommendation;
import com.g9latam.team62.fintech_api.service.BudgetRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.g9latam.team62.fintech_api.security.AuthorizationHelper;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@Tag(name = "Recomendaciones", description = "Endpoints para la generación y gestión de recomendaciones presupuestarias")
public class RecommendationController {

    private final BudgetRecommendationService recommendationService;
    private final com.g9latam.team62.fintech_api.service.RecommendationService recommendationHistoryService;
    private final AuthorizationHelper authorizationHelper;

    public RecommendationController(BudgetRecommendationService recommendationService,
                                    com.g9latam.team62.fintech_api.service.RecommendationService recommendationHistoryService,
                                    AuthorizationHelper authorizationHelper) {
        this.recommendationService = recommendationService;
        this.recommendationHistoryService = recommendationHistoryService;
        this.authorizationHelper = authorizationHelper;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generar recomendaciones presupuestarias",
               description = "Evalúa la distribución de gastos del usuario contra referencias INE (Chile) en un período dado y genera alertas de sobregasto, validando que el userId pertenezca al usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recomendaciones generadas exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Recommendation.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos o usuario no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<List<Recommendation>> generateRecommendations(
            @Parameter(description = "ID del usuario") @NonNull @RequestParam Long userId,
            @Parameter(description = "Fecha de inicio del período (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @Parameter(description = "Fecha de término del período (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            Principal principal) {

        authorizationHelper.verifyUserOwnership(principal, userId);

        LocalDate end = periodEnd != null ? periodEnd : LocalDate.now();
        LocalDate start = periodStart != null ? periodStart : end.minusDays(30);

        List<Recommendation> created = recommendationService.generateRecommendations(userId, start, end);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Obtener historial de recomendaciones",
               description = "Retorna el historial de recomendaciones generadas y guardadas para el usuario, validando la propiedad del recurso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial de recomendaciones retornado con éxito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Recommendation.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<List<Recommendation>> getRecommendationsHistory(
            @PathVariable @NonNull Long userId, Principal principal) {
        authorizationHelper.verifyUserOwnership(principal, userId);
        List<Recommendation> history = recommendationHistoryService.findByUserId(userId);
        return ResponseEntity.ok(history);
    }
}
