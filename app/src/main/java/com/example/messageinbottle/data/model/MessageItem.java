package com.example.messageinbottle.data.model;

public class MessageItem {

    private long id;
    private long userId;
    private Long taskId;
    private String type;
    private String title;
    private String content;
    private long createdAt;

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}

