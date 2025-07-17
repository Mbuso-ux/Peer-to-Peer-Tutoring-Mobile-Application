package com.example.peertut;

public class AiChatEntry {
    private final String content;
    private final boolean isFromUser;

    public AiChatEntry(String content, boolean isFromUser) {
        this.content = content;
        this.isFromUser = isFromUser;
    }

    // Match these method names to what's used in AiChatActivity
    public String getText() { return content; }
    public boolean isUser() { return isFromUser; }

    // Optional: Keep original getters if needed elsewhere
    public String getContent() { return content; }
    public boolean isFromUser() { return isFromUser; }
}