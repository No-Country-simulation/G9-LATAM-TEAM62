package com.g9latam.team62.fintech_api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class Recommendation {

    private Long id;

    @NotBlank
    private String text;

    private LocalDateTime generatedAt;

    // profile the user had when this recommendation was generated, so it can be
    // explained or invalidated after the profile changes
    private FinancialProfile profileAtGeneration;

    @NotNull
    private Long userId;

    public Recommendation() {
    }

    public Recommendation(Long id, String text, LocalDateTime generatedAt,
                          FinancialProfile profileAtGeneration, Long userId) {
        this.id = id;
        this.text = text;
        this.generatedAt = generatedAt;
        this.profileAtGeneration = profileAtGeneration;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public FinancialProfile getProfileAtGeneration() {
        return profileAtGeneration;
    }

    public void setProfileAtGeneration(FinancialProfile profileAtGeneration) {
        this.profileAtGeneration = profileAtGeneration;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
