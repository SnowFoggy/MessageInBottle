package com.example.messageinbottle.data.model;

public class HomeTask {

    private final long id;
    private final String title;
    private final double amount;
    private final String publishTime;
    private final String category;
    private final String description;
    private final String deadline;
    private final String publisher;

    public HomeTask(long id, String title, double amount, String publishTime, String category, String description, String deadline, String publisher) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.publishTime = publishTime;
        this.category = category;
        this.description = description;
        this.deadline = deadline;
        this.publisher = publisher;
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

    public String getPublisher() {
        return publisher;
    }
}
