package com.example.peertut;

public class Session {
    private String sessionId;
    private String tutorId;
    private String tutorName;
    private String tuteeId;
    private String subject;
    private String date;
    private String time;
    private String status;  // Added status field (e.g., "in-progress", "completed")

    public Session() {}

    public Session(String sessionId, String tutorId, String tutorName, String tuteeId, String subject, String date, String time,String status) {
        this.sessionId = sessionId;
        this.tutorId = tutorId;
        this.tutorName = tutorName;
        this.tuteeId = tuteeId;
        this.subject = subject;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getTutorId() { return tutorId; }
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }

    public String getTutorName() { return tutorName; }
    public void setTutorName(String tutorName) { this.tutorName = tutorName; }

    public String getTuteeId() { return tuteeId; }
    public void setTuteeId(String tuteeId) { this.tuteeId = tuteeId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
}
