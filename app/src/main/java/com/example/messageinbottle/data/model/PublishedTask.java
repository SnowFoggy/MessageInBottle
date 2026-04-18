package com.example.messageinbottle.data.model;

public class PublishedTask {

    private final long id;
    private final String title;
    private final String description;
    private final double amount;
    private final String deadline;
    private final String progress;
    private final String taskImageUrl;
    private final String completionProofUrl;

    public PublishedTask(long id, String title, String description, double amount, String deadline, String progress,
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

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
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
