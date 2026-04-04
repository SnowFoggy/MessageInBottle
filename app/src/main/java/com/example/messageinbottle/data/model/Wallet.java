package com.example.messageinbottle.data.model;

public class Wallet {

    private long id;
    private long userId;
    private double balance;
    private long updatedAt;

    public Wallet() {
    }

    public Wallet(long id, long userId, double balance, long updatedAt) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}