package com.g9latam.team62.fintech_api.controller;

import com.g9latam.team62.fintech_api.model.Recommendation;
import com.g9latam.team62.fintech_api.service.BudgetRecommendationService;
import com.g9latam.team62.fintech_api.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService service;
    private final BudgetRecommendationService budgetRecommendationService;

    public RecommendationController(RecommendationService service,
                                     BudgetRecommendationService budgetRecommendationService) {
        this.service = service;
        this.budgetRecommendationService = budgetRecommendationService;
    }

    @GetMapping
    public Collection<Recommendation> findAll(@RequestParam(required = false) Long userId) {
        if (userId != null) {
            return service.findByUserId(userId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recommendation> findById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Recommendation> create(@Valid @RequestBody Recommendation recommendation) {
        Recommendation created = service.create(recommendation);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Motor de Recomendaciones Presupuestarias: compara el gasto por categoría
    // del usuario en [from, to] contra los porcentajes de referencia y genera
    // hasta 3 recomendaciones nuevas (más la de tasa de ahorro, si corresponde).
    // Si no hay suficientes transacciones en el período, o el usuario está en
    // cooldown, devuelve una lista vacía -- no es un error.
    @PostMapping("/generate")
    public ResponseEntity<List<Recommendation>> generate(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<Recommendation> created = budgetRecommendationService.generateRecommendations(userId, from, to);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
