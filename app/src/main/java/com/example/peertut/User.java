package com.example.peertut;

import java.util.List;

public class User {

    private String uid;
    private String name;
    private String email;
    private String role;

    // Common fields
    private String institution;
    private String profileImage; // Base64 image

    // Tutee-specific fields
    private List<String> subjects;

    private String academicLevel;
    private int age;
    private String shortTermGoals;

    // Tutor-specific fields
    private List<String> degrees;
    private List<String> institutionsAttended;
    private String teachingExperience;
    private String teachingPhilosophy;
    private double rating;
    private float averageRating;
    private String teamsMeetingUrl;


    // Firebase requires empty constructor
    public User() {}

    public User(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // Getters and setters for all fields
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    // Tutee getters/setters
    public List<String> getSubjects() { return subjects; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }


    public String getAcademicLevel() { return academicLevel; }
    public void setAcademicLevel(String academicLevel) { this.academicLevel = academicLevel; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getShortTermGoals() { return shortTermGoals; }
    public void setShortTermGoals(String shortTermGoals) { this.shortTermGoals = shortTermGoals; }

    // Tutor getters/setters
    public List<String> getDegrees() { return degrees; }
    public void setDegrees(List<String> degrees) { this.degrees = degrees; }

    public List<String> getInstitutionsAttended() { return institutionsAttended; }
    public void setInstitutionsAttended(List<String> institutionsAttended) {
        this.institutionsAttended = institutionsAttended;
    }

    public String getTeachingExperience() { return teachingExperience; }
    public void setTeachingExperience(String teachingExperience) { this.teachingExperience = teachingExperience; }

    public String getTeachingPhilosophy() { return teachingPhilosophy; }
    public void setTeachingPhilosophy(String teachingPhilosophy) { this.teachingPhilosophy = teachingPhilosophy; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public float getAverageRating() { return averageRating; }
    public void setAverageRating(float averageRating) { this.averageRating = averageRating; }

    public String getTeamsMeetingUrl() {
        return teamsMeetingUrl;
    }

    public void setTeamsMeetingUrl(String teamsMeetingUrl) {
        this.teamsMeetingUrl = teamsMeetingUrl;
    }

}
