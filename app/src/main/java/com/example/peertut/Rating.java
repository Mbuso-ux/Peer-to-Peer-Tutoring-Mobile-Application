// Rating.java
package com.example.peertut;

public class Rating {
    private String ratingId;
    private String bookingId;
    private String raterId;
    private String rateeId;
    private float ratingValue;
    private String comment;
    private long timestamp;

    public Rating(){}

    public Rating(String bookingId, String raterId, String rateeId,
                  float ratingValue, String comment, long timestamp) {
        this.bookingId = bookingId;
        this.raterId = raterId;
        this.rateeId = rateeId;
        this.ratingValue = ratingValue;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public Rating(String ratingId, long timestamp, float ratingValue, String comment, String rateeId, String raterId, String bookingId) {
        this.ratingId = ratingId;
        this.timestamp = timestamp;
        this.ratingValue = ratingValue;
        this.comment = comment;
        this.rateeId = rateeId;
        this.raterId = raterId;
        this.bookingId = bookingId;
    }

    public Rating(String ratingId, String bookingId, String tuteeId, String tutorId, float rating, String comment, long l) {

    }

    // Getters and setters
    public String getRatingId() { return ratingId; }
    public void setRatingId(String ratingId) { this.ratingId = ratingId; }
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public String getRaterId() { return raterId; }
    public void setRaterId(String raterId) { this.raterId = raterId; }
    public String getRateeId() { return rateeId; }
    public void setRateeId(String rateeId) { this.rateeId = rateeId; }
    public float getRatingValue() { return ratingValue; }
    public void setRatingValue(float ratingValue) { this.ratingValue = ratingValue; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}