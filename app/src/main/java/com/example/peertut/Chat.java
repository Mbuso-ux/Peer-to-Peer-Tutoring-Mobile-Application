package com.example.peertut;

public class Chat {
    public String chatId;
    public String lastMessage;
    public long timestamp;

    // Required empty constructor
    public Chat() {}

    public Chat(String chatId, String lastMessage, long timestamp) {
        this.chatId      = chatId;
        this.lastMessage = lastMessage;
        this.timestamp   = timestamp;
    }
}
