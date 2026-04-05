package com.example.messageinbottle.data.model;

public class MineTaskRecord {

    private final long id;
    private final String title;
    private final String description;
    private final double amount;
    private final String deadline;
    private final String type;
    private final String completionProofUrl;

    public MineTaskRecord(long id, String title, String description, double amount, String deadline, String type,
                          String completionProofUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.deadline = deadline;
        this.type = type;
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

    public String getType() {
        return type;
    }

    public String getCompletionProofUrl() {
        return completionProofUrl;
    }
}
