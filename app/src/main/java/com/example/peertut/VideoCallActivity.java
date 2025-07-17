package com.example.peertut;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class VideoCallActivity extends AppCompatActivity {

    private Button btnJoinTeams, btnEndSession;
    private TextView tvInfo;
    private String bookingId, teamsJoinUrl, currentUid;
    private DatabaseReference bookingRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        initializeComponents();
        setupFirebase();
        loadBookingAndTutorInfo();
        setupClickListeners();
    }

    private void initializeComponents() {
        bookingId = getIntent().getStringExtra("bookingId");
        if (bookingId == null) {
            finish();
            return;
        }

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        btnJoinTeams = findViewById(R.id.btnJoinTeams);
        btnEndSession = findViewById(R.id.btnEndSession);
        tvInfo = findViewById(R.id.tvInfo);
    }

    private void setupFirebase() {
        bookingRef = FirebaseDatabase.getInstance().getReference("bookings").child(bookingId);
    }

    private void loadBookingAndTutorInfo() {
        bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Booking booking = snapshot.getValue(Booking.class);
                if (booking == null) {
                    tvInfo.setText("Booking not found.");
                    btnJoinTeams.setEnabled(false);
                    return;
                }

                // Try using teamsJoinUrl directly from booking first
                String urlFromBooking = booking.getTeamsJoinUrl();

                if (urlFromBooking != null && !urlFromBooking.trim().isEmpty() && !urlFromBooking.equalsIgnoreCase("null")) {
                    teamsJoinUrl = urlFromBooking;
                    tvInfo.setText("Click the button to join your Teams meeting:");
                    btnJoinTeams.setEnabled(true);
                } else {
                    // If not found, fallback to loading from tutor's profile
                    loadTutorTeamsUrl(booking.getTutorId());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(VideoCallActivity.this, "Error loading booking data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTutorTeamsUrl(String tutorId) {
        FirebaseDatabase.getInstance().getReference("users").child(tutorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && "Tutor".equals(user.getRole())
                                && user.getTeamsMeetingUrl() != null
                                && !user.getTeamsMeetingUrl().trim().isEmpty()) {
                            teamsJoinUrl = user.getTeamsMeetingUrl();
                            tvInfo.setText("Click the button to join your Teams meeting:");
                            btnJoinTeams.setEnabled(true);
                        } else {
                            tvInfo.setText("No meeting link was provided for this session.");
                            btnJoinTeams.setEnabled(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(VideoCallActivity.this, "Failed to load tutor info.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void setupClickListeners() {
        btnJoinTeams.setOnClickListener(v -> launchTeamsMeeting());
        btnEndSession.setOnClickListener(v -> endSession());
    }

    private void launchTeamsMeeting() {
        try {
            String teamsDeepLink = teamsJoinUrl
                    .replace("https://", "msteams://")
                    .replace("teams.microsoft.com/l/", "teams.microsoft.com/l/");

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(teamsDeepLink));
            intent.setPackage("com.microsoft.teams");
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            handleTeamsNotInstalled();
        } catch (Exception e) {
            handleJoinError(e);
        }
    }

    private void handleTeamsNotInstalled() {
        Toast.makeText(this, "Microsoft Teams not installed", Toast.LENGTH_LONG).show();
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=com.microsoft.teams")));
    }

    private void handleJoinError(Exception e) {
        Toast.makeText(this, "Couldn't open meeting link", Toast.LENGTH_SHORT).show();
        e.printStackTrace();
    }

    private void endSession() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "completed");
        updates.put("endTime", System.currentTimeMillis());

        bookingRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                triggerRatingNotifications();
                finish();
            } else {
                Toast.makeText(VideoCallActivity.this, "Error ending session", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void triggerRatingNotifications() {
        bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Booking booking = snapshot.getValue(Booking.class);
                if (booking == null) return;

                DatabaseReference notifRef = FirebaseDatabase.getInstance()
                        .getReference("ratingNotifications");

                String tuteeId = booking.getTuteeId();
                String tutorId = booking.getTutorId();
                String bId = booking.getBookingId();

                if (tuteeId != null && tutorId != null && bId != null) {
                    sendRatingNotification(notifRef, tuteeId, tutorId, bId);
                    sendRatingNotification(notifRef, tutorId, tuteeId, bId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(VideoCallActivity.this, "Error triggering ratings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendRatingNotification(DatabaseReference notifRef,
                                        String userId, String targetId, String bookingId) {
        notifRef.child(userId).push()
                .setValue(new RatingNotification(bookingId, targetId));
    }
}
