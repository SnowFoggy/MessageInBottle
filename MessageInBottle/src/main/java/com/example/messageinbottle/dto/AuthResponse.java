package com.example.messageinbottle.dto;

public class AuthResponse {

    private Long id;
    private String username;
    private String nickname;
    private Long createdAt;

    public AuthResponse() {
    }

    public AuthResponse(Long id, String username, String nickname, Long createdAt) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}

