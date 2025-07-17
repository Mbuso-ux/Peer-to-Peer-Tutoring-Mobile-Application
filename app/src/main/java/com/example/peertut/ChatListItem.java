package com.example.peertut;

public class ChatListItem {
    private String chatId;
    private String otherUserId;
    private String otherUserName;

    public ChatListItem(String chatId, String otherUserId, String otherUserName) {
        this.chatId = chatId;
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
    }

    // Getters
    public String getChatId() { return chatId; }
    public String getOtherUserId() { return otherUserId; }
    public String getOtherUserName() { return otherUserName; }
}