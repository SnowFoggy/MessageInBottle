package com.example.messageinbottle.dto;

public class WalletResponse {

    private Long id;
    private Long userId;
    private Double balance;
    private Long updatedAt;

    public WalletResponse() {
    }

    public WalletResponse(Long id, Long userId, Double balance, Long updatedAt) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Double getBalance() {
        return balance;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }
}

