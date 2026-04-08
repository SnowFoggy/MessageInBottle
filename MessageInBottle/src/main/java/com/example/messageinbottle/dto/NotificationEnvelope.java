package com.example.messageinbottle.dto;

public class NotificationEnvelope {

    private String event;
    private MessageBoxResponse message;

    public NotificationEnvelope() {
    }

    public NotificationEnvelope(String event, MessageBoxResponse message) {
        this.event = event;
        this.message = message;
    }

    public String getEvent() {
        return event;
    }

    public MessageBoxResponse getMessage() {
        return message;
    }
}

