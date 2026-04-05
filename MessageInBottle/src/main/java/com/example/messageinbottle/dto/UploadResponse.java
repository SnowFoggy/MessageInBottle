package com.example.messageinbottle.dto;

public class UploadResponse {

    private final String fileUrl;
    private final String message;

    public UploadResponse(String fileUrl, String message) {
        this.fileUrl = fileUrl;
        this.message = message;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getMessage() {
        return message;
    }
}

