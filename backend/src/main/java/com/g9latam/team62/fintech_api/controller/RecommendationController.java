package com.g9latam.team62.fintech_api.controller;

import com.g9latam.team62.fintech_api.dto.RecommendationResponse;
import com.g9latam.team62.fintech_api.model.Recommendation;
import com.g9latam.team62.fintech_api.security.AuthorizationHelper;
import com.g9latam.team62.fintech_api.service.BudgetRecommendationService;
import com.g9latam.team62.fintech_api.service.ClientEvolutionService;
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

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@Tag(name = "Recomendaciones", description = "Endpoints para la generación y gestión de recomendaciones presupuestarias")
public class RecommendationController {

    private final BudgetRecommendationService recommendationService;
    private final com.g9latam.team62.fintech_api.service.RecommendationService recommendationHistoryService;
    private final AuthorizationHelper authorizationHelper;
    // -- aporte --
    private final ClientEvolutionService clientEvolutionService;

    public RecommendationController(BudgetRecommendationService recommendationService,
                                    com.g9latam.team62.fintech_api.service.RecommendationService recommendationHistoryService,
                                    AuthorizationHelper authorizationHelper,
                                    ClientEvolutionService clientEvolutionService) {
        this.recommendationService = recommendationService;
        this.recommendationHistoryService = recommendationHistoryService;
        this.authorizationHelper = authorizationHelper;
        this.clientEvolutionService = clientEvolutionService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generar recomendaciones presupuestarias",
               description = "Evalúa la distribución de gastos del usuario contra referencias INE (Chile) en un período dado y genera alertas de sobregasto, validando que el userId pertenezca al usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recomendaciones generadas exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RecommendationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos o usuario no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<List<RecommendationResponse>> generateRecommendations(
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
        return ResponseEntity.ok(RecommendationResponse.fromEntities(created));
    }

    // -- aporte -- Evolutivo del cliente: compara el mes evaluado contra el
    // propio promedio histórico del usuario, no contra el % fijo del INE
    // (ese es el endpoint de arriba, sigue igual). Requiere al menos 3 meses
    // de historial previo -- si no hay suficiente, devuelve una lista vacía,
    // no un error.
    @PostMapping("/generate-evolution")
    @Operation(summary = "Generar recomendaciones del evolutivo del cliente",
               description = "Compara el gasto del mes evaluado contra el propio promedio histórico del usuario (media y desviación estándar, sin ML). Requiere al menos 3 meses de historial previo con datos; si no los hay, devuelve una lista vacía. Parámetros ajustados y validados contra un dataset dummy con ruido controlado -- pendiente de re-validar con datos reales una vez haya suficiente historial cargado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recomendaciones generadas (puede ser una lista vacía si falta historial)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RecommendationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos o usuario no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<List<RecommendationResponse>> generateEvolutionRecommendations(
            @Parameter(description = "ID del usuario") @NonNull @RequestParam Long userId,
            @Parameter(description = "Cualquier fecha dentro del mes a evaluar (YYYY-MM-DD); por defecto, hoy")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @Parameter(description = "Cuántos meses hacia atrás considerar como historial (por defecto 6)")
            @RequestParam(required = false) Integer lookbackMonths,
            Principal principal) {

        authorizationHelper.verifyUserOwnership(principal, userId);

        YearMonth evaluationMonth = YearMonth.from(month != null ? month : LocalDate.now());
        int lookback = lookbackMonths != null ? lookbackMonths : 6;

        List<Recommendation> created = clientEvolutionService.generateEvolutionRecommendations(
                userId, evaluationMonth, lookback);
        return ResponseEntity.ok(RecommendationResponse.fromEntities(created));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Obtener historial de recomendaciones",
               description = "Retorna el historial de recomendaciones generadas y guardadas para el usuario, validando la propiedad del recurso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial de recomendaciones retornado con éxito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RecommendationResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<List<RecommendationResponse>> getRecommendationsHistory(
            @PathVariable @NonNull Long userId, Principal principal) {
        authorizationHelper.verifyUserOwnership(principal, userId);
        List<Recommendation> history = recommendationHistoryService.findByUserId(userId);
        return ResponseEntity.ok(RecommendationResponse.fromEntities(history));
    }
}
