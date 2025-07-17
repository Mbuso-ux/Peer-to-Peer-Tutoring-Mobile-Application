package com.example.peertut;

/**
 * Model class representing a tutoring session booking.
 */
public class Booking {
    private String bookingId;
    private String tuteeId;
    private String tutorId;
    private String subject;
    private String slotId;
    private long startTime;
    private long endTime;
    private int duration;
    private String status;
    private long createdAt;
    private boolean completed;
    private long completedAt;
    private String completionNotes;
    private boolean isRated;
    private boolean notified;

    private String sessionType;
    private String teamsJoinUrl; // ← Add this field

    /**
     * Required empty constructor for Firebase deserialization.
     */
    public Booking() { }


    public Booking(String bookingId, String tuteeId, String tutorId,
                   long createdAt, int duration,
                   boolean completed, long completedAt, long endTime,
                   String teamsJoinUrl) {
        this.bookingId = bookingId;
        this.tuteeId = tuteeId;
        this.tutorId = tutorId;
        this.createdAt = createdAt;
        this.duration = duration;
        this.completed = completed;
        this.completedAt = completedAt;
        this.endTime = endTime;
        this.teamsJoinUrl = teamsJoinUrl;
    }
    /**
     * Full constructor.
     */
    public Booking(String bookingId,
                   String tuteeId,
                   String tutorId,
                   String subject,
                   String slotId,
                   long startTime,
                   long endTime,
                   int duration,
                   String status,
                   long createdAt,
                   boolean completed,
                   long completedAt,
                   String completionNotes,
                   boolean isRated) {
        this.bookingId = bookingId;
        this.tuteeId = tuteeId;
        this.tutorId = tutorId;
        this.subject = subject;
        this.slotId = slotId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.status = status;
        this.createdAt = createdAt;
        this.completed = completed;
        this.completedAt = completedAt;
        this.completionNotes = completionNotes;
        this.isRated = isRated;
    }

    // Getters and setters for all fields

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getTuteeId() { return tuteeId; }
    public void setTuteeId(String tuteeId) { this.tuteeId = tuteeId; }

    public String getTutorId() { return tutorId; }
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public String getCompletionNotes() { return completionNotes; }
    public void setCompletionNotes(String completionNotes) { this.completionNotes = completionNotes; }

    public boolean isRated() { return isRated; }
    public void setRated(boolean rated) { this.isRated = rated; }

    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }

    public String getSessionType() {
        return sessionType;
    }

    public boolean isNotified() {
        return notified;
    }
    public void setNotified(boolean notified) { this.notified = notified; }

    public String getTeamsJoinUrl() { return teamsJoinUrl; }
    public void setTeamsJoinUrl(String teamsJoinUrl) { this.teamsJoinUrl = teamsJoinUrl; }
}
