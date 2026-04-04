package com.example.messageinbottle.dto;

import jakarta.validation.constraints.NotNull;

public class AcceptTaskRequest {

    @NotNull
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}

