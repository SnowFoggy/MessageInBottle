package com.example.messageinbottle.data.remote;

public class RegisterRequest {

    private final String username;
    private final String nickname;
    private final String password;

    public RegisterRequest(String username, String nickname, String password) {
        this.username = username;
        this.nickname = nickname;
        this.password = password;
    }
}


