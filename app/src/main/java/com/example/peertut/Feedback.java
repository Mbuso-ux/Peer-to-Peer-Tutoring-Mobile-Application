// Feedback.java
package com.example.peertut;

public class Feedback {
    private String feedbackId;
    private String bookingId;
    private String senderId;
    private String receiverId;
    private String message;
    private long timestamp;
    private boolean isPublic;

    public Feedback() {
        // Default constructor required for Firebase
    }

    public Feedback(String bookingId, String senderId, String receiverId, String message, long timestamp, boolean isPublic) {
        this.bookingId = bookingId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.isPublic = isPublic;
    }

    // Getters and setters
    public String getFeedbackId() { return feedbackId; }
    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }
}