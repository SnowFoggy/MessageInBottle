package com.example.messageinbottle.dto;

public class AcceptedTaskResponse {

    private Long id;
    private String title;
    private Double amount;
    private String deadline;
    private String reviewStatus;
    private boolean completed;

    public AcceptedTaskResponse() {
    }

    public AcceptedTaskResponse(Long id, String title, Double amount, String deadline, String reviewStatus, boolean completed) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.deadline = deadline;
        this.reviewStatus = reviewStatus;
        this.completed = completed;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Double getAmount() {
        return amount;
    }

    public String getDeadline() {
        return deadline;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public boolean isCompleted() {
        return completed;
    }
}

