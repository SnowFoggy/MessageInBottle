package com.example.messageinbottle.data.model;

public class AcceptedTask {

    private final long id;
    private final String title;
    private final double amount;
    private final String deadline;
    private String reviewStatus;
    private boolean completed;

    public AcceptedTask(long id, String title, double amount, String deadline, String reviewStatus, boolean completed) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.deadline = deadline;
        this.reviewStatus = reviewStatus;
        this.completed = completed;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public double getAmount() {
        return amount;
    }

    public String getDeadline() {
        return deadline;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
