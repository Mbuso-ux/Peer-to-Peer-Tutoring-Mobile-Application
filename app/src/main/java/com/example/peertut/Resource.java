package com.example.peertut;

public class Resource {

    private String title;
    private String description;
    private String url;
    private String uploadedBy;
    private long timestamp;
    private String tutorName; // Add this field

    public Resource() {}

    public Resource(String title, String description, String url, String uploadedBy, long timestamp) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.uploadedBy = uploadedBy;
        this.timestamp = timestamp;
    }

    // Getters and setters for each field
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public String getTutorName() { return tutorName; }
    public void setTutorName(String tutorName) { this.tutorName = tutorName; }
}
