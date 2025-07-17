package com.example.peertut;

public class Message {
    private String senderId;
    private String message;
    private long timestamp;

    public Message() {}

    public Message(String senderId, String message) {
        this.senderId = senderId;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getSenderId() { return senderId; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}