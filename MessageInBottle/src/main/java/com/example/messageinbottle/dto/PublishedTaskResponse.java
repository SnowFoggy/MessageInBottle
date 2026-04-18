package com.example.messageinbottle.dto;

public class PublishedTaskResponse {

    private Long id;
    private String title;
    private String description;
    private Double amount;
    private String deadline;
    private String progress;
    private String taskImageUrl;
    private String completionProofUrl;

    public PublishedTaskResponse(Long id, String title, String description, Double amount, String deadline, String progress,
                                 String taskImageUrl, String completionProofUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.deadline = deadline;
        this.progress = progress;
        this.taskImageUrl = taskImageUrl;
        this.completionProofUrl = completionProofUrl;
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

    public String getTaskImageUrl() {
        return taskImageUrl;
    }

    public String getCompletionProofUrl() {
        return completionProofUrl;
    }
}
