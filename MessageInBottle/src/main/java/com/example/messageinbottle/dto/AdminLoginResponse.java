package com.example.messageinbottle.dto;

public class AdminLoginResponse {

    private final String username;
    private final String role;

    public AdminLoginResponse(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}




