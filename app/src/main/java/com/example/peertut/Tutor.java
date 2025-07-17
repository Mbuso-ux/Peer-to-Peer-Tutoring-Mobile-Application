package com.example.peertut;

import java.util.List;
import java.util.Map;

public class Tutor {
    private String id;
    private String name;
    private String email;
    private List<String> subjects;
    private double rating;
    private Map<String, Boolean> availability;
    private String teachingExperience;
    private String teachingPhilosophy;
    private List<String> degrees;
    private List<String> institutionsAttended;

    // Required empty constructor for Firebase
    public Tutor() {}

    public Tutor(String id, String name, String email, List<String> subjects,
                 double rating, Map<String, Boolean> availability,
                 String teachingExperience, String teachingPhilosophy,
                 List<String> degrees, List<String> institutionsAttended) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.subjects = subjects;
        this.rating = rating;
        this.availability = availability;
        this.teachingExperience = teachingExperience;
        this.teachingPhilosophy = teachingPhilosophy;
        this.degrees = degrees;
        this.institutionsAttended = institutionsAttended;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public List<String> getSubjects() { return subjects; }
    public double getRating() { return rating; }
    public Map<String, Boolean> getAvailability() { return availability; }
    public String getTeachingExperience() { return teachingExperience; }
    public String getTeachingPhilosophy() { return teachingPhilosophy; }
    public List<String> getDegrees() { return degrees; }
    public List<String> getInstitutionsAttended() { return institutionsAttended; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }
    public void setRating(double rating) { this.rating = rating; }
    public void setAvailability(Map<String, Boolean> availability) { this.availability = availability; }
    public void setTeachingExperience(String teachingExperience) { this.teachingExperience = teachingExperience; }
    public void setTeachingPhilosophy(String teachingPhilosophy) { this.teachingPhilosophy = teachingPhilosophy; }
    public void setDegrees(List<String> degrees) { this.degrees = degrees; }
    public void setInstitutionsAttended(List<String> institutionsAttended) { this.institutionsAttended = institutionsAttended; }

    // Helper method for availability check
    public boolean isAvailable(String day) {
        return availability != null &&
                availability.containsKey(day) &&
                availability.get(day);
    }
}