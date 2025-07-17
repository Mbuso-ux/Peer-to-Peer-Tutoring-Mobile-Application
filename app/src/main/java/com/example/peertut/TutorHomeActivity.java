package com.example.peertut;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.widget.ImageButton;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TutorHomeActivity extends AppCompatActivity {

    // UI Components
    private TextView welcomeTextView, availabilityStatusTextView,
            averageRatingTextView;
    private ImageButton profileButton;

    private MaterialButton editSubjectsButton, manageAvailabilityButton, viewRatingsButton;
    private MaterialButton uploadResourceButton, uploadQuizButton, joinClassBtn;
    // Firebase References
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef, availabilityRef, bookingsRef, ratingsRef, ratingNotificationsRef;
    private String currentUserId;

    // Session Management
    private String activeBookingId;
    private boolean isSessionEnding = false;
    private ChildEventListener ratingNotificationsListener;
    private DatabaseReference activeBookingRef;
    private Map<String, Object> sessionUpdates;
    private ChipGroup subjectsChipGroup;
    private ImageButton chatButton;
    private MaterialToolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_home);

        initializeFirebase();
        initializeViews();
        setupButtonListeners();
        setupDataListeners();
        setupToolbar();
    }
    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        availabilityRef = FirebaseDatabase.getInstance().getReference("availability");
        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        ratingsRef = FirebaseDatabase.getInstance().getReference("ratings");
    }

    private void initializeViews() {
        welcomeTextView = findViewById(R.id.welcomeTextView);
        availabilityStatusTextView = findViewById(R.id.availabilityStatusTextView);
        averageRatingTextView = findViewById(R.id.averageRatingTextView);

        toolbar = findViewById(R.id.toolbar);
        profileButton = findViewById(R.id.profileButton);
        editSubjectsButton = findViewById(R.id.editSubjectsButton);
        uploadResourceButton = findViewById(R.id.uploadResourceButton);
        uploadQuizButton = findViewById(R.id.uploadQuizButton);
        viewRatingsButton = findViewById(R.id.viewRatingsButton);
        joinClassBtn = findViewById(R.id.joinClassBtn);
        manageAvailabilityButton = findViewById(R.id.manageAvailabilityButton);
        subjectsChipGroup = findViewById(R.id.subjectsChipGroup);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Add menu item click listener
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_logout) {
                mAuth.signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupButtonListeners() {
        editSubjectsButton.setOnClickListener(v ->
                startActivity(new Intent(this, EditSubjectsActivity.class)));

        uploadResourceButton.setOnClickListener(v ->
                startActivity(new Intent(this, UploadResourceActivity.class)));

        uploadQuizButton.setOnClickListener(v ->
                startActivity(new Intent(this, UploadQuizActivity.class)));


        viewRatingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewRatingsActivity.class);
            intent.putExtra("tutorId", currentUserId);
            startActivity(intent);
        });

        manageAvailabilityButton.setOnClickListener(v ->
                startActivity(new Intent(this, TutorAvailabilityActivity.class)));

        profileButton.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        joinClassBtn.setOnClickListener(v -> handleJoinClass());
    }

    private void handleJoinClass() {
        if (activeBookingId == null) return;

        bookingsRef.child(activeBookingId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Booking booking = snapshot.getValue(Booking.class);
                if (booking != null && "active".equals(booking.getStatus())) {
                    startActivity(new Intent(TutorHomeActivity.this, VideoCallActivity.class)
                            .putExtra("bookingId", activeBookingId));
                } else {
                    showToast("Session not active yet");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                logError("Booking check failed", error);
            }
        });
    }

    private void setupDataListeners() {
        setupUserProfile();
        setupSessionListener();
        setupRatingViews();
        checkAvailabilityStatus();
    }

    private void setupUserProfile() {
        usersRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    welcomeTextView.setText("Welcome, " + (name != null ? name : "Tutor"));

                    subjectsChipGroup.removeAllViews();
                    if (snapshot.hasChild("subjects")) {
                        for (DataSnapshot subjectSnap : snapshot.child("subjects").getChildren()) {
                            String subject = subjectSnap.getValue(String.class);
                            if (subject != null) {
                                addSubjectChip(subject);
                            }
                        }
                    }
                }
            }

            private void addSubjectChip(String subject) {
                Chip chip = new Chip(TutorHomeActivity.this);
                chip.setText(subject);
                chip.setChipBackgroundColorResource(R.color.tutor_chip_background);
                chip.setTextColor(ContextCompat.getColor(TutorHomeActivity.this, R.color.tutor_primary));
                chip.setChipStrokeColor(ColorStateList.valueOf(
                        ContextCompat.getColor(TutorHomeActivity.this, R.color.tutor_primary)));
                chip.setChipStrokeWidth(2f);
                subjectsChipGroup.addView(chip);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                logError("Error loading profile", error);
            }
        });
    }
    private void setupSessionListener() {
        bookingsRef.orderByChild("tutorId").equalTo(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        activeBookingId = null;
                        joinClassBtn.setVisibility(View.GONE);

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Booking booking = ds.getValue(Booking.class);
                            if (booking == null) continue;

                            long now = System.currentTimeMillis();
                            boolean isActive = now >= booking.getStartTime() && now <= booking.getEndTime();

                            if ("confirmed".equals(booking.getStatus()) && isActive) {
                                ds.getRef().child("status").setValue("active");
                            }

                            if ("active".equals(booking.getStatus())) {
                                if (isActive) {
                                    activeBookingId = booking.getBookingId();
                                    joinClassBtn.setVisibility(View.VISIBLE);
                                } else {
                                    ds.getRef().child("status").setValue("completed");
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        logError("Session listener cancelled", error);
                    }
                });
    }


    private void handleSessionStatus(DataSnapshot ds, Booking booking, boolean isActive) {
        if ("confirmed".equals(booking.getStatus()) && isActive) {
            ds.getRef().child("status").setValue("active");
        }

        if ("active".equals(booking.getStatus())) {
            if (isActive) {
                activeBookingId = booking.getBookingId();
                joinClassBtn.setVisibility(View.VISIBLE);
            } else {
                ds.getRef().child("status").setValue("completed");
            }
        }
    }

    private void showRatingDialogWithDelay(Booking booking) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing() && !isDestroyed() && booking != null) {
                showTuteeRatingDialog(booking.getTuteeId(), booking.getBookingId());
            }
        }, 500);
    }

    private void showTuteeRatingDialog(String rateeId, String bookingId) {
        runOnUiThread(() -> {
            try {
                FragmentManager fm = getSupportFragmentManager();
                if (fm.isDestroyed()) return;

                RatingDialogFragment dialog = RatingDialogFragment.newInstance(rateeId, bookingId);
                FragmentTransaction ft = fm.beginTransaction();
                ft.add(dialog, "ratingDialog");
                ft.commitAllowingStateLoss();
            } catch (IllegalStateException e) {
                Log.e("TutorHome", "Error showing rating dialog", e);
            }
        });
    }

    public static class RatingDialogFragment extends DialogFragment {
        private static final String ARG_RATEE_ID = "ratee_id";
        private static final String ARG_BOOKING_ID = "booking_id";

        public static RatingDialogFragment newInstance(String rateeId, String bookingId) {
            RatingDialogFragment fragment = new RatingDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_RATEE_ID, rateeId);
            args.putString(ARG_BOOKING_ID, bookingId);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public AlertDialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    new ContextThemeWrapper(requireContext(),
                            android.R.style.Theme_DeviceDefault_Light_Dialog)
            );

            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_rate_tutor, null);

            RatingBar ratingBar = view.findViewById(R.id.ratingBar);
            EditText commentEdit = view.findViewById(R.id.commentEdit);

            return builder.setView(view)
                    .setTitle("Rate Student")
                    .setPositiveButton("Submit", (d, w) -> {
                        submitRating(
                                ratingBar.getRating(),
                                commentEdit.getText().toString()
                        );
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
        }

        private void submitRating(float rating, String comment) {
            if (getActivity() == null || getActivity().isFinishing()) return;

            String rateeId = getArguments().getString(ARG_RATEE_ID);
            String bookingId = getArguments().getString(ARG_BOOKING_ID);

            if (rateeId == null || bookingId == null) {
                showToast("Rating submission failed: Missing data");
                return;
            }

            DatabaseReference ratingsRef = FirebaseDatabase.getInstance()
                    .getReference("ratings");

            String ratingId = ratingsRef.push().getKey();
            if (ratingId == null) return;

            ratingsRef.child(ratingId).setValue(new Rating(
                    bookingId,
                    FirebaseAuth.getInstance().getCurrentUser().getUid(),
                    rateeId,
                    rating,
                    comment,
                    System.currentTimeMillis()
            )).addOnCompleteListener(task -> {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    showToast(task.isSuccessful() ? "Rating submitted!" : "Failed to submit rating");
                }
            });
        }

        private void showToast(String message) {
            if (getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            if (getDialog() != null && getDialog().isShowing()) {
                getDialog().dismiss();
            }
        }
    }

    private void setupRatingViews() {
        ratingsRef.orderByChild("rateeId").equalTo(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        float total = 0;
                        int count = 0;
                        for (DataSnapshot rs : snap.getChildren()) {
                            Rating r = rs.getValue(Rating.class);
                            if (r != null) {
                                total += r.getRatingValue();
                                count++;
                            }
                        }
                        if (count > 0) {
                            averageRatingTextView.setText(String.format(Locale.getDefault(),
                                    "Avg: %.1f/5", total / count));
                        } else {
                            averageRatingTextView.setText("No ratings");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        logError("Rating load failed", error);
                    }
                });
    }
    private void checkAvailabilityStatus() {
        availabilityRef.child(currentUserId).orderByChild("status").equalTo("available")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        long slots = snap.getChildrenCount();
                        availabilityStatusTextView.setText(slots > 0 ?
                                slots + " slots available" : "No slots");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        logError("Availability check failed", error);
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void logError(String message, DatabaseError error) {
        Log.e("TutorHome", message, error.toException());
    }

    // Modify the rating notification setup
    private void setupRatingNotifications() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return;
        }

        currentUserId = auth.getCurrentUser().getUid();
        ratingNotificationsRef = FirebaseDatabase.getInstance().getReference("ratingNotifications");

        ratingNotificationsRef.child(currentUserId).addChildEventListener(
                new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, String prevChildKey) {
                        RatingNotification notification = snapshot.getValue(RatingNotification.class);
                        if (notification != null) {
                            // Remove notification after showing to avoid duplication
                            snapshot.getRef().removeValue();

                            // Show the rating dialog
                            showRatingDialog(notification.getRateeId(), notification.getBookingId());
                        }
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("RatingNotification", "Failed to listen for rating notifications", error.toException());
                    }
                }
        );
    }
    private void showRatingDialog(String tutorId, String bookingId) {
        bookingsRef.child(bookingId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String sessionType = snapshot.child("sessionType").getValue(String.class);
                String title = "quick-help".equals(sessionType)
                        ? "Rate Your Quick Help Session"
                        : "Rate Your Tutee";

                AlertDialog.Builder builder = new AlertDialog.Builder(TutorHomeActivity.this);
                builder.setTitle(title);

                View view = getLayoutInflater().inflate(R.layout.dialog_rate_tutor, null);
                RatingBar ratingBar = view.findViewById(R.id.ratingBar);
                EditText commentEditText = view.findViewById(R.id.commentEdit);

                builder.setView(view);
                builder.setPositiveButton("Submit", (dialog, which) -> {
                    float rating = ratingBar.getRating();
                    String comment = commentEditText.getText().toString();
                    submitRating(tutorId, bookingId, rating, comment);
                });
                builder.setNegativeButton("Later", null);
                builder.setCancelable(false);
                builder.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TuteeHome", "Error loading booking info", error.toException());
            }
        });
    }
    private void submitRating(String rateeId, String bookingId, float rating, String comment) {
        String raterId = mAuth.getCurrentUser().getUid();
        String ratingId = ratingsRef.push().getKey();

        Rating ratingObj = new Rating(
                bookingId,
                raterId,
                rateeId,
                rating,
                comment,
                System.currentTimeMillis()
        );

        ratingsRef.child(ratingId).setValue(ratingObj)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Rating submitted!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to submit rating", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ratingNotificationsRef != null && ratingNotificationsListener != null) {
            ratingNotificationsRef.removeEventListener(ratingNotificationsListener);
        }
    }
}