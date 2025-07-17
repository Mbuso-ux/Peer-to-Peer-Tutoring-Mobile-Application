package com.example.peertut;

public class ChatMessage {
    public String sender;
    public String message;
    public long timestamp;

    public ChatMessage() {} // Required for Firebase

    public ChatMessage(String sender, String message, long timestamp) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }
}
