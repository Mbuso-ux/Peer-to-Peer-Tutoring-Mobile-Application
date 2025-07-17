package com.example.peertut;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ViewBookingsActivity extends AppCompatActivity
        implements BookingsAdapter.BookingActionListener {

    private RecyclerView recyclerView;
    private BookingsAdapter adapter;
    private List<Booking> bookings = new ArrayList<>();

    private DatabaseReference bookingsRef;
    private DatabaseReference ratingsRef;
    private DatabaseReference notificationsRef;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_bookings);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        ratingsRef = FirebaseDatabase.getInstance().getReference("ratings");
        notificationsRef = FirebaseDatabase.getInstance().getReference("ratingNotifications");

        // Setup RecyclerView
        recyclerView = findViewById(R.id.bookingsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingsAdapter(bookings,
                new SimpleDateFormat("EEE, MMM d, h:mm a", Locale.getDefault()),
                this);
        recyclerView.setAdapter(adapter);

        loadBookings();
    }

    private void loadBookings() {
        if (auth.getCurrentUser() == null || getIntent() == null) {
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();
        String role = getIntent().getStringExtra("role");

        if (role == null) {
            Toast.makeText(this, "Role not specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Query query = role.equals("Tutee")
                ? bookingsRef.orderByChild("tuteeId").equalTo(currentUserId)
                : bookingsRef.orderByChild("tutorId").equalTo(currentUserId);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookings.clear();
                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                    Booking booking = bookingSnapshot.getValue(Booking.class);
                    if (booking != null) {
                        booking.setBookingId(bookingSnapshot.getKey());
                        if (booking.getStatus() == null) {
                            booking.setStatus("pending");
                        }
                        bookings.add(booking);
                    }
                }
                // Sort bookings by the start time (latest first)
                Collections.sort(bookings, (b1, b2) -> Long.compare(b2.getStartTime(), b1.getStartTime()));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewBookingsActivity.this,
                        "Failed to load bookings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCompleteBooking(Booking booking) {
        new AlertDialog.Builder(this)
                .setTitle("Complete Session")
                .setMessage("Mark this session as completed?")
                .setPositiveButton("Complete", (d, w) -> completeSession(booking))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void completeSession(Booking booking) {
        if (booking == null || booking.getBookingId() == null) {
            Toast.makeText(this, "Invalid booking", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "ended"); // Update status to "ended"
        updates.put("completed", true);
        updates.put("completedAt", ServerValue.TIMESTAMP);

        String notificationId = notificationsRef.child(booking.getTuteeId()).push().getKey();
        Map<String, Object> notification = new HashMap<>();
        notification.put("bookingId", booking.getBookingId());
        notification.put("tutorId", booking.getTutorId());
        notification.put("createdAt", ServerValue.TIMESTAMP);
        notification.put("viewed", false);

        bookingsRef.child(booking.getBookingId()).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    notificationsRef.child(booking.getTuteeId()).child(notificationId)
                            .setValue(notification)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(ViewBookingsActivity.this,
                                        "Session completed! Tutee can now rate.",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ViewBookingsActivity.this,
                            "Failed to complete session",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showRatingDialog(Booking booking) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rate_tutor, null);

        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText commentEdit = dialogView.findViewById(R.id.commentEdit);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Rate Your Session")
                .setView(dialogView)
                .setPositiveButton("Submit", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button submitButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            submitButton.setOnClickListener(view -> {
                float rating = ratingBar.getRating();
                String comment = commentEdit.getText().toString().trim();

                if (rating < 1) {
                    Toast.makeText(this, "Please select at least 1 star", Toast.LENGTH_SHORT).show();
                    return;
                }

                submitRating(booking, rating, comment);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void submitRating(Booking booking, float rating, String comment) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String ratingId = ratingsRef.push().getKey();
        String tuteeId = auth.getCurrentUser().getUid();

        if (ratingId == null || booking.getBookingId() == null || booking.getTutorId() == null) {
            Toast.makeText(this, "Error submitting rating", Toast.LENGTH_SHORT).show();
            return;
        }

        Rating ratingObj = new Rating(
                ratingId,
                booking.getBookingId(),
                tuteeId,
                booking.getTutorId(),
                rating,
                comment,
                System.currentTimeMillis()
        );

        Map<String, Object> updates = new HashMap<>();
        updates.put("ratings/" + ratingId, ratingObj);
        updates.put("bookings/" + booking.getBookingId() + "/rated", true);

        FirebaseDatabase.getInstance().getReference()
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                    updateTutorAverageRating(booking.getTutorId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit rating", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTutorAverageRating(String tutorId) {
        ratingsRef.orderByChild("tutorId").equalTo(tutorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        float total = 0;
                        int count = 0;

                        for (DataSnapshot ratingSnapshot : snapshot.getChildren()) {
                            Rating rating = ratingSnapshot.getValue(Rating.class);
                            if (rating != null) {
                                total += rating.getRatingValue();
                                count++;
                            }
                        }

                        if (count > 0) {
                            float average = total / count;
                            DatabaseReference tutorRef = FirebaseDatabase.getInstance()
                                    .getReference("users")
                                    .child(tutorId)
                                    .child("averageRating");

                            tutorRef.setValue(average);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Rating", "Failed to calculate average rating", error.toException());
                    }
                });
    }

    @Override
    public void onRateTutor(Booking booking) {
        if (booking != null && "ended".equals(booking.getStatus()) && !booking.isRated()) {
            showRatingDialog(booking);
        }
    }

    @Override
    public void onViewBooking(Booking booking) {
        new AlertDialog.Builder(this)
                .setTitle("Booking Details")
                .setMessage(getBookingDetails(booking))
                .setPositiveButton("OK", null)
                .show();
    }

    private String getBookingDetails(Booking booking) {
        if (booking == null) {
            return "No booking details available";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, h:mm a", Locale.getDefault());
        String dateStr = dateFormat.format(new Date(booking.getStartTime()));

        return String.format(Locale.getDefault(),
                "Subject: %s\nDate: %s\nDuration: %d mins\nStatus: %s%s",
                booking.getSubject() != null ? booking.getSubject() : "N/A",
                dateStr,
                booking.getDuration(),
                booking.getStatus() != null ? booking.getStatus() : "N/A",
                booking.getCompletionNotes() != null ? "\nNotes: " + booking.getCompletionNotes() : "");
    }
}
