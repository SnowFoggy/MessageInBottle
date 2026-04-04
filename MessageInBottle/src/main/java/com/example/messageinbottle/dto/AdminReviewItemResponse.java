package com.example.messageinbottle.dto;

public class AdminReviewItemResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final String category;
    private final Double amount;
    private final String deadline;
    private final Long publisherId;
    private final String publisherName;
    private final Long accepterId;
    private final String reviewStatus;
    private final Boolean completed;
    private final Long createdAt;

    public AdminReviewItemResponse(Long id, String title, String description, String category, Double amount, String deadline,
                                   Long publisherId, String publisherName, Long accepterId, String reviewStatus,
                                   Boolean completed, Long createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.deadline = deadline;
        this.publisherId = publisherId;
        this.publisherName = publisherName;
        this.accepterId = accepterId;
        this.reviewStatus = reviewStatus;
        this.completed = completed;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public Double getAmount() {
        return amount;
    }

    public String getDeadline() {
        return deadline;
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public Long getAccepterId() {
        return accepterId;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public Long getCreatedAt() {
        return createdAt;
    }
}


