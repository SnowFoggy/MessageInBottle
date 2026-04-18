package com.example.messageinbottle.dto;

public class HomeTaskResponse {

    private Long id;
    private Double amount;
    private String title;
    private String publishTime;
    private String category;
    private String description;
    private String deadline;
    private Long publisherId;
    private String publisher;
    private String taskImageUrl;

    public HomeTaskResponse() {
    }

    public HomeTaskResponse(Long id, String title, Double amount, String publishTime, String category, String description,
                            String deadline, Long publisherId, String publisher, String taskImageUrl) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.publishTime = publishTime;
        this.category = category;
        this.description = description;
        this.deadline = deadline;
        this.publisherId = publisherId;
        this.publisher = publisher;
        this.taskImageUrl = taskImageUrl;
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

    public String getPublishTime() {
        return publishTime;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getDeadline() {
        return deadline;
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getTaskImageUrl() {
        return taskImageUrl;
    }
}
