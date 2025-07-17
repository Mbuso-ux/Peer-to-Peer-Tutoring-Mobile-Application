package com.example.peertut;

public class RatingNotification {
    private String bookingId;
    private String rateeId;

    public RatingNotification() {} // Required for Firebase

    public RatingNotification(String bookingId, String rateeId) {
        this.bookingId = bookingId;
        this.rateeId = rateeId;
    }

    // Getters
    public String getBookingId() { return bookingId; }
    public String getRateeId() { return rateeId; }
}