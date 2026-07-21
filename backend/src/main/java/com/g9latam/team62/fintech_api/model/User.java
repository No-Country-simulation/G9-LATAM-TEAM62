package com.g9latam.team62.fintech_api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class User {

    private Long id;

    @NotBlank
    private String name;

    // login identifier, unique across users
    @NotBlank
    @Email
    private String email;

    // stored as a BCrypt hash; write-only so it never appears in responses
    @NotBlank
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @PositiveOrZero
    private BigDecimal monthlyIncome;

    private SavingFrequency savingFrequency;

    // financialProfile, profileAccuracy and profileUpdatedAt are written by the
    // external profiling app through PUT /api/users/{id}/profile, not by API clients
    private FinancialProfile financialProfile;

    // confidence of the assigned profile, 0.0 to 1.0; null until first computed
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double profileAccuracy;

    private LocalDateTime profileUpdatedAt;

    public User() {
    }

    public User(Long id, String name, String email, String password, BigDecimal monthlyIncome,
                SavingFrequency savingFrequency, FinancialProfile financialProfile,
                Double profileAccuracy, LocalDateTime profileUpdatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.monthlyIncome = monthlyIncome;
        this.savingFrequency = savingFrequency;
        this.financialProfile = financialProfile;
        this.profileAccuracy = profileAccuracy;
        this.profileUpdatedAt = profileUpdatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public SavingFrequency getSavingFrequency() {
        return savingFrequency;
    }

    public void setSavingFrequency(SavingFrequency savingFrequency) {
        this.savingFrequency = savingFrequency;
    }

    public FinancialProfile getFinancialProfile() {
        return financialProfile;
    }

    public void setFinancialProfile(FinancialProfile financialProfile) {
        this.financialProfile = financialProfile;
    }

    public Double getProfileAccuracy() {
        return profileAccuracy;
    }

    public void setProfileAccuracy(Double profileAccuracy) {
        this.profileAccuracy = profileAccuracy;
    }

    public LocalDateTime getProfileUpdatedAt() {
        return profileUpdatedAt;
    }

    public void setProfileUpdatedAt(LocalDateTime profileUpdatedAt) {
        this.profileUpdatedAt = profileUpdatedAt;
    }
}
