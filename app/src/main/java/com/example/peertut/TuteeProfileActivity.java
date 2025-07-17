package com.example.peertut;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class TuteeProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private TextView nameTextView, emailTextView, academicLevelTextView,
            institutionTextView, ageTextView, subjectsTextView, goalsTextView, ratingTextView;
    private RatingBar ratingBar;

    private DatabaseReference usersRef, ratingsRef;
    private String tuteeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutee_profile);

        tuteeId = getIntent().getStringExtra("tuteeId");
        usersRef = FirebaseDatabase.getInstance().getReference("users").child(tuteeId);
        ratingsRef = FirebaseDatabase.getInstance().getReference("ratings");

        initializeViews();
        loadTuteeProfile();
        loadRatingAverage();
    }

    private void initializeViews() {
        profileImageView = findViewById(R.id.profileImageView);
        nameTextView = findViewById(R.id.tuteeNameTextView);
        emailTextView = findViewById(R.id.tuteeEmailTextView);
        academicLevelTextView = findViewById(R.id.academicLevelTextView);
        institutionTextView = findViewById(R.id.institutionTextView);
        ageTextView = findViewById(R.id.ageTextView);
        subjectsTextView = findViewById(R.id.subjectsTextView);
        goalsTextView = findViewById(R.id.goalsTextView);
        ratingTextView = findViewById(R.id.ratingTextView);
        ratingBar = findViewById(R.id.ratingBar);
    }

    private void loadTuteeProfile() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    showToastAndFinish("Tutee profile not found");
                    return;
                }

                String name = getValueOrPlaceholder(snapshot, "name", "Tutee Name");
                String email = getValueOrPlaceholder(snapshot, "email", "No email");
                String academicLevel = getValueOrPlaceholder(snapshot, "academicLevel", "Not specified");
                String institution = getValueOrPlaceholder(snapshot, "institution", "Not specified");
                String age = getValueOrPlaceholder(snapshot, "age", "N/A");
                String goals = getValueOrPlaceholder(snapshot, "shortTermGoals", "No goals specified");
                String profilePicUrl = getValueOrPlaceholder(snapshot, "profilePicUrl", "");

                List<String> subjects = new ArrayList<>();
                for (DataSnapshot subjectSnapshot : snapshot.child("subjects").getChildren()) {
                    String subject = subjectSnapshot.getValue(String.class);
                    if (subject != null) subjects.add(subject);
                }

                nameTextView.setText(name);
                emailTextView.setText(email);
                academicLevelTextView.setText("Academic Level: " + academicLevel);
                institutionTextView.setText("Institution: " + institution);
                ageTextView.setText("Age: " + age);
                subjectsTextView.setText("Subjects: " + (subjects.isEmpty() ? "None" : String.join(", ", subjects)));
                goalsTextView.setText("Learning Goals:\n" + goals);

                if (!profilePicUrl.isEmpty()) {
                    Glide.with(TuteeProfileActivity.this)
                            .load(profilePicUrl)
                            .into(profileImageView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToastAndFinish("Error loading profile");
            }
        });
    }

    private void loadRatingAverage() {
        ratingsRef.orderByChild("rateeId").equalTo(tuteeId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        float totalRating = 0;
                        int count = 0;

                        for (DataSnapshot ratingSnapshot : snapshot.getChildren()) {
                            Rating rating = ratingSnapshot.getValue(Rating.class);
                            if (rating != null) {
                                totalRating += rating.getRatingValue();
                                count++;
                            }
                        }

                        if (count > 0) {
                            float average = totalRating / count;
                            ratingTextView.setText(String.format("Rating: %.1f (%d reviews)", average, count));
                            ratingBar.setRating(average);
                        } else {
                            ratingTextView.setText("No ratings yet");
                            ratingBar.setRating(0);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        ratingTextView.setText("Error loading rating");
                    }
                });
    }

    private String getValueOrPlaceholder(DataSnapshot snapshot, String path, String placeholder) {
        Object value = snapshot.child(path).getValue();
        return value != null ? value.toString() : placeholder;
    }

    private void showToastAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}
