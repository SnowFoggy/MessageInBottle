package com.example.messageinbottle.data.model;

public class TaskItem {

    private long id;
    private long publisherId;
    private String title;
    private String description;
    private String category;
    private double reward;
    private long deadline;
    private String status;
    private long createdAt;

    public TaskItem() {
    }

    public TaskItem(long id, long publisherId, String title, String description, String category, double reward, long deadline, String status, long createdAt) {
        this.id = id;
        this.publisherId = publisherId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.reward = reward;
        this.deadline = deadline;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(long publisherId) {
        this.publisherId = publisherId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getReward() {
        return reward;
    }

    public void setReward(double reward) {
        this.reward = reward;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

