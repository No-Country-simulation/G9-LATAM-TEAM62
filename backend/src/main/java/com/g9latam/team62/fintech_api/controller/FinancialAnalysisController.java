package com.g9latam.team62.fintech_api.controller;

import com.g9latam.team62.fintech_api.dto.FinancialAnalysisRequest;
import com.g9latam.team62.fintech_api.dto.FinancialAnalysisResponse;
import com.g9latam.team62.fintech_api.model.User;
import com.g9latam.team62.fintech_api.security.AuthorizationHelper;
import com.g9latam.team62.fintech_api.service.FinancialAnalysisService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@Tag(name = "Análisis Financiero", description = "Endpoints para el análisis financiero y diagnóstico presupuestario del usuario")
public class FinancialAnalysisController {

    private final FinancialAnalysisService financialAnalysisService;
    private final AuthorizationHelper authorizationHelper;

    public FinancialAnalysisController(FinancialAnalysisService financialAnalysisService,
                                       AuthorizationHelper authorizationHelper) {
        this.financialAnalysisService = financialAnalysisService;
        this.authorizationHelper = authorizationHelper;
    }

    @PostMapping({"/api/analisis-financiero", "/analisis-financiero"})
    @Operation(summary = "Analizar el comportamiento financiero del usuario",
               description = "Analiza los movimientos registrados en la base de datos (todas las transacciones del usuario o una lista de IDs seleccionados), actualiza el perfil y genera recomendaciones presupuestarias.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Análisis completado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FinancialAnalysisResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido")
    })
    public ResponseEntity<FinancialAnalysisResponse> analyze(
            @Valid @RequestBody FinancialAnalysisRequest request, Principal principal) {
        User user = authorizationHelper.getAuthenticatedUser(principal);
        return ResponseEntity.ok(financialAnalysisService.analyze(user.getId(), request));
    }
}
