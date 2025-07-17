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

public class TutorProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private TextView nameTextView, emailTextView, degreesTextView,
            institutionsTextView, experienceTextView, philosophyTextView,
            subjectsTextView, ratingTextView;
    private RatingBar ratingBar;

    private DatabaseReference usersRef, ratingsRef;
    private String tutorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_profile);

        tutorId = getIntent().getStringExtra("tutorId");
        usersRef = FirebaseDatabase.getInstance().getReference("users").child(tutorId);
        ratingsRef = FirebaseDatabase.getInstance().getReference("ratings");

        initializeViews();
        loadTutorProfile();
        loadRatingAverage();
    }

    private void initializeViews() {
        profileImageView = findViewById(R.id.tutorProfileImageView);
        nameTextView = findViewById(R.id.tutorNameTextView);
        emailTextView = findViewById(R.id.tutorEmailTextView);
        degreesTextView = findViewById(R.id.tutorDegreesTextView);
        experienceTextView = findViewById(R.id.tutorExperienceTextView);
        philosophyTextView = findViewById(R.id.tutorPhilosophyTextView);
        subjectsTextView = findViewById(R.id.tutorSubjectsTextView);
        ratingTextView = findViewById(R.id.tutorRatingTextView);
        ratingBar = findViewById(R.id.tutorRatingBar);
    }

    private void loadTutorProfile() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    showToastAndFinish("Tutor profile not found");
                    return;
                }

                String name = getValueOrPlaceholder(snapshot, "name", "Tutor Name");
                String email = getValueOrPlaceholder(snapshot, "email", "No email");
                String degrees = getValueOrPlaceholder(snapshot, "degrees", "No degrees listed");
                String experience = getValueOrPlaceholder(snapshot, "teachingExperience", "No experience listed");
                String philosophy = getValueOrPlaceholder(snapshot, "teachingPhilosophy", "No philosophy specified");
                String profilePicUrl = getValueOrPlaceholder(snapshot, "profilePicUrl", "");

                StringBuilder subjects = new StringBuilder("Teaching Subjects:\n");
                if (snapshot.hasChild("subjects")) {
                    for (DataSnapshot subjectSnapshot : snapshot.child("subjects").getChildren()) {
                        subjects.append("• ").append(subjectSnapshot.getValue(String.class)).append("\n");
                    }
                } else {
                    subjects.append("No subjects listed");
                }

                nameTextView.setText(name);
                emailTextView.setText(email);
                degreesTextView.setText("Degrees:\n" + degrees);
                experienceTextView.setText("Teaching Experience:\n" + experience);
                philosophyTextView.setText("Teaching Philosophy:\n" + philosophy);
                subjectsTextView.setText(subjects.toString());

                if (!profilePicUrl.isEmpty()) {
                    Glide.with(TutorProfileActivity.this)
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
        ratingsRef.orderByChild("rateeId").equalTo(tutorId)
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
                            float avg = totalRating / count;
                            ratingTextView.setText(String.format("Rating: %.1f (%d reviews)", avg, count));
                            ratingBar.setRating(avg);
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
