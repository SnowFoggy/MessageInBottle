package com.example.messageinbottle.dto;

public class PublishedTaskResponse {

    private Long id;
    private String title;
    private String description;
    private Double amount;
    private String deadline;
    private String progress;


    public PublishedTaskResponse(Long id, String title, String description, Double amount, String deadline, String progress) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.deadline = deadline;
        this.progress = progress;
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

    public Double getAmount() {
        return amount;
    }

    public String getDeadline() {
        return deadline;
    }

    public String getProgress() {
        return progress;
    }
}
