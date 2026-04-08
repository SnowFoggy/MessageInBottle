package com.example.messageinbottle.dto;

public class MessageBoxResponse {

    private Long id;
    private Long userId;
    private Long taskId;
    private String type;
    private String title;
    private String content;
    private Long createdAt;

    public MessageBoxResponse() {
    }

    public MessageBoxResponse(Long id, Long userId, Long taskId, String type, String title, String content, Long createdAt) {
        this.id = id;
        this.userId = userId;
        this.taskId = taskId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
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

    public Long getCreatedAt() {
        return createdAt;
    }
}

