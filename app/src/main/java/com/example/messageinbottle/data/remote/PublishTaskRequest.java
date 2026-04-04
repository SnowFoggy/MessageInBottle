package com.example.messageinbottle.data.remote;

public class PublishTaskRequest {

    private final long userId;
    private final String title;
    private final String category;
    private final String description;
    private final double amount;
    private final String deadline;

    public PublishTaskRequest(long userId, String title, String category, String description, double amount, String deadline) {
        this.userId = userId;
        this.title = title;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.deadline = deadline;
    }
}


