package com.example.messageinbottle.data.remote;

public class AuthPayload {

    private long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private long createdAt;

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
