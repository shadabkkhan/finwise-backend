package com.finwise.dto;

import java.time.LocalDateTime;

public class FraudAlertResponse {
    private Long id;

    private String reason;

    private String severity;

    private boolean dismissed;

    private LocalDateTime createdAt;

    private String transactionTitle;

    private Double transactionAmount;

    public FraudAlertResponse(Long id, String reason, String severity, boolean dismissed, LocalDateTime createdAt, String transactionTitle, Double transactionAmount) {
        this.id = id;
        this.reason = reason;
        this.severity = severity;
        this.dismissed = dismissed;
        this.createdAt = createdAt;
        this.transactionTitle = transactionTitle;
        this.transactionAmount = transactionAmount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public boolean isDismissed() {
        return dismissed;
    }

    public void setDismissed(boolean dismissed) {
        this.dismissed = dismissed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTransactionTitle() {
        return transactionTitle;
    }

    public void setTransactionTitle(String transactionTitle) {
        this.transactionTitle = transactionTitle;
    }

    public Double getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(Double transactionAmount) {
        this.transactionAmount = transactionAmount;
    }
}
