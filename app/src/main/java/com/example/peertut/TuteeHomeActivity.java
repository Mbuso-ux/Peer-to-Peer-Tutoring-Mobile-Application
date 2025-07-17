package com.example.peertut;

import static android.content.ContentValues.TAG;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TuteeHomeActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private TextView welcomeTextView;
    private TextInputLayout searchContainer;
    private EditText searchEditText;
    private ListView tutorsListView;
    private MaterialButton viewResourcesButton, viewQuizzesButton, viewBookingsButton;
    private FloatingActionButton fabAiAssistant;
    private ShapeableImageView profileImage;
    private BottomAppBar bottomAppBar;
    private Button joinClassBtn;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef, registrationsRef, availabilityRef, bookingsRef, ratingsRef, ratingNotificationsRef;
    private List<Tutor> tutorList = new ArrayList<>();
    private ArrayAdapter<Tutor> tutorAdapter;
    private Map<String, Set<String>> existingRegistrations = new HashMap<>();
    private String currentUserId;
    private DatabaseReference sessionsRef, notificationsRef;
    private Context context;
    private String activeBookingId;
    private TextView sessionInfo;

    private Booking booking;
    private Button manageProfileButton;
    private List<Tutor> allTutorsFull = new ArrayList<>();
    private ChildEventListener tutorsChildEventListener;
    private float averageRating;
    private String name;
    private boolean showingRegisteredTutors = false;
    private static final int AI_CHAT_REQUEST = 1001;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutee_home);



        // Initialize Firebase Auth and get current user ID
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in—cannot listen for invites.");
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        // Initialize UI components
        initializeViews();
        setupToolbar();
        setupFirebase();
        setupAdapters();
        setupListeners();

        // Load data
        displayUserName();
        loadExistingRegistrations();
        setupRatingNotifications();
        listenForSessions();
        setupRealTimeTutorListener();


    }
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        welcomeTextView = findViewById(R.id.welcomeTextView);
        searchContainer = findViewById(R.id.searchContainer);
        searchEditText = findViewById(R.id.searchEditText);
        tutorsListView = findViewById(R.id.tutorsListView);
        viewResourcesButton = findViewById(R.id.viewResourcesButton);
        viewQuizzesButton = findViewById(R.id.viewQuizzesButton);
        viewBookingsButton = findViewById(R.id.viewBookingsButton);
        fabAiAssistant = findViewById(R.id.fabAiAssistant);
        profileImage = findViewById(R.id.profileImage);
        bottomAppBar = findViewById(R.id.bottomAppBar);
        joinClassBtn = findViewById(R.id.joinClassBtn);
        joinClassBtn.setVisibility(View.GONE);
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AI_CHAT_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                // Get filters with null safety
                String filters = data.getStringExtra("searchFilters");
                // Handle null case and trim safely
                filterTutors(filters != null ? filters.trim() : "");
            } else {
                // Handle case where data intent is null
                filterTutors("");
            }
        }
    }

    private void setupFirebase() {
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        registrationsRef = FirebaseDatabase.getInstance().getReference("registrations");
        availabilityRef = FirebaseDatabase.getInstance().getReference("availability");
        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        ratingsRef = FirebaseDatabase.getInstance().getReference("ratings");
        ratingNotificationsRef = FirebaseDatabase.getInstance().getReference("ratingNotifications");
        sessionsRef = FirebaseDatabase.getInstance().getReference("sessions");
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
    }
    private void setupRealTimeTutorListener() {
        Query tutorsQuery = usersRef.orderByChild("role").equalTo("Tutor");
        tutorsChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Tutor tutor = parseTutorFromSnapshot(snapshot);
                if (tutor != null) {
                    allTutorsFull.removeIf(t -> t.getId().equals(tutor.getId()));
                    allTutorsFull.add(tutor);
                    applyCurrentFilter();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Tutor updatedTutor = parseTutorFromSnapshot(snapshot);
                if (updatedTutor != null) {
                    allTutorsFull.removeIf(t -> t.getId().equals(updatedTutor.getId()));
                    allTutorsFull.add(updatedTutor);
                    applyCurrentFilter();
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String tutorId = snapshot.getKey();
                allTutorsFull.removeIf(t -> t.getId().equals(tutorId));
                applyCurrentFilter();
            }

            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        tutorsQuery.addChildEventListener(tutorsChildEventListener);
    }
    private void filterTutors(String query) {
        query = query.trim().toLowerCase();
        List<String> parts = Arrays.asList(query.split("\\s+"));

        float minRating = 0f;
        List<String> keywords = new ArrayList<>();

        for (String part : parts) {
            try {
                float rating = Float.parseFloat(part);
                if (rating > minRating) minRating = rating;
            } catch (NumberFormatException e) {
                keywords.add(part);
            }
        }

        tutorList.clear();
        for (Tutor tutor : allTutorsFull) {
            if (tutor.getAverageRating() < minRating) continue;

            boolean subjectMatch = keywords.isEmpty();
            for (String keyword : keywords) {
                for (String subject : tutor.getSubjects()) {
                    if (subject.toLowerCase().contains(keyword)) {
                        subjectMatch = true;
                        break;
                    }
                }
                if (subjectMatch) break;
            }

            if (subjectMatch) tutorList.add(tutor);
        }

        tutorAdapter.notifyDataSetChanged();
    }

    private Tutor parseTutorFromSnapshot(DataSnapshot snapshot) {
        String id = snapshot.getKey();
        String email = snapshot.child("email").getValue(String.class);
        String name = snapshot.hasChild("name") ? snapshot.child("name").getValue(String.class) : "Tutor";
        Float avgRating = snapshot.child("averageRating").getValue(Float.class);
        List<String> subjects = new ArrayList<>();

        if (snapshot.hasChild("subjects")) {
            for (DataSnapshot subjSnapshot : snapshot.child("subjects").getChildren()) {
                String subject = subjSnapshot.getValue(String.class);
                if (subject != null) subjects.add(subject);
            }
        }

        if (id == null || email == null) return null;
        return new Tutor(id, name, email, subjects, avgRating != null ? avgRating : 0f);
    }

    private void setupAdapters() {
        tutorAdapter = new ArrayAdapter<Tutor>(this, R.layout.item_tutor, R.id.tutorNameTextView, tutorList) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                Tutor tutor = tutorList.get(position);

                TextView nameView = view.findViewById(R.id.tutorNameTextView);
                TextView emailView = view.findViewById(R.id.tutorEmailTextView);
                TextView subjectsView = view.findViewById(R.id.tutorSubjectsTextView);
                RatingBar ratingBar = view.findViewById(R.id.tutorRatingBar);
                TextView ratingText = view.findViewById(R.id.tutorRatingText);
                Button actionButton = view.findViewById(R.id.actionButton);

                nameView.setText(tutor.getDisplayName());
                emailView.setText(tutor.getEmail());
                subjectsView.setText("Subjects: " + String.join(", ", tutor.getSubjects()));
                ratingBar.setRating(tutor.getAverageRating());
                ratingText.setText(String.format(Locale.getDefault(), "%.1f", tutor.getAverageRating()));

                actionButton.setOnClickListener(v -> showTutorOptionsDialog(tutor));

                return view;
            }
        };
        tutorsListView.setAdapter(tutorAdapter);
    }


    private void displayUserName() {
        usersRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    welcomeTextView.setText(getString(R.string.welcome, name != null ? name : "User"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading user name", error.toException());
                welcomeTextView.setText(R.string.welcome);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersRef != null && tutorsChildEventListener != null) {
            usersRef.removeEventListener(tutorsChildEventListener);
        }
    }
    private void applyCurrentFilter() {
        String query = searchEditText.getText().toString().trim().toLowerCase();
        List<String> parts = Arrays.asList(query.split("\\s+"));

        float minRating = 0f;
        List<String> subjectTerms = new ArrayList<>();

        for (String part : parts) {
            try {
                float potentialRating = Float.parseFloat(part);
                if (potentialRating > minRating) {
                    minRating = potentialRating;
                }
            } catch (NumberFormatException e) {
                subjectTerms.add(part);
            }
        }

        List<Tutor> filtered = new ArrayList<>();
        for (Tutor tutor : allTutorsFull) {
            if (tutor.getAverageRating() < minRating) continue;

            boolean subjectMatch = subjectTerms.isEmpty();
            if (!subjectMatch) {
                for (String term : subjectTerms) {
                    for (String subject : tutor.getSubjects()) {
                        if (subject.toLowerCase().contains(term.toLowerCase())) {
                            subjectMatch = true;
                            break;
                        }
                    }
                    if (subjectMatch) break;
                }
            }

            if (subjectMatch) {
                filtered.add(tutor);
            }
        }

        tutorList.clear();
        tutorList.addAll(filtered);
        tutorAdapter.notifyDataSetChanged();
    }
    private void loadExistingRegistrations() {
        String tuteeId = mAuth.getCurrentUser().getUid();
        registrationsRef.child(tuteeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                existingRegistrations.clear();
                for (DataSnapshot tutorSnapshot : snapshot.getChildren()) {
                    String tutorId = tutorSnapshot.getKey();
                    Set<String> subjects = new HashSet<>();
                    for (DataSnapshot subjectSnapshot : tutorSnapshot.getChildren()) {
                        subjects.add(subjectSnapshot.getKey());
                    }
                    existingRegistrations.put(tutorId, subjects);
                }
                loadAllTutors();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TuteeHome", "Error loading registrations", error.toException());
                loadAllTutors();
            }
        });
    }

    private void loadAllTutors() {
        usersRef.orderByChild("role").equalTo("Tutor")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allTutorsFull.clear(); // Fix: apply filter on full list
                        for (DataSnapshot ts : snapshot.getChildren()) {
                            String id = ts.getKey();
                            String email = ts.child("email").getValue(String.class);
                            String name = ts.hasChild("name") ? ts.child("name").getValue(String.class) : "Tutor";


                            if (id == null || email == null) continue;

                            List<String> subjects = new ArrayList<>();
                            if (ts.hasChild("subjects")) {
                                for (DataSnapshot subSnap : ts.child("subjects").getChildren()) {
                                    Object raw = subSnap.getValue();
                                    String subj = (raw instanceof String) ? (String) raw : subSnap.getKey();
                                    subjects.add(subj);
                                }
                            }

                            Tutor tutor = new Tutor(id, name, email, subjects, 0f);
                            allTutorsFull.add(tutor);

                            loadTutorRating(tutor); // Rating loads async
                        }

                        applyCurrentFilter(); // ✅ Use filter to update display
                    }

                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        Log.e("TuteeHome", "Error loading tutors", e.toException());
                    }
                });
    }




    private void loadTutorRating(Tutor tutor) {
        ratingsRef.orderByChild("rateeId").equalTo(tutor.getId())
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
                            tutor.setAverageRating(total / count);
                            applyCurrentFilter(); // ✅ Refresh list with updated rating
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("TuteeHome", "Error loading ratings", error.toException());
                    }
                });
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
                        : "Rate Your Tutor";

                AlertDialog.Builder builder = new AlertDialog.Builder(TuteeHomeActivity.this);
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

    private void listenForSessions() {
        bookingsRef.orderByChild("tuteeId").equalTo(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        activeBookingId = null;
                        joinClassBtn.setVisibility(View.GONE);

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Booking booking = ds.getValue(Booking.class);
                            if (booking == null) continue;

                            long now = System.currentTimeMillis();
                            boolean isWithinTime = now >= booking.getStartTime() && now <= booking.getEndTime();

                            // Immediate activation when tutor views
                            if ("confirmed".equals(booking.getStatus()) && isWithinTime) {
                                ds.getRef().child("status").setValue("active");
                            }

                            if ("active".equals(booking.getStatus()) && isWithinTime) {
                                activeBookingId = booking.getBookingId();
                                joinClassBtn.setVisibility(View.VISIBLE);
                                break;
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void setupListeners() {
        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTutors(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Logout functionality
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_logout) {
                mAuth.signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }
            return false;
        });

        // Tutor list item click
        tutorsListView.setOnItemClickListener((parent, view, position, id) -> {
            Tutor selectedTutor = tutorAdapter.getItem(position);
            showTutorOptionsDialog(selectedTutor);
        });

        // Bottom navigation buttons
        viewResourcesButton.setOnClickListener(v ->
                startActivity(new Intent(this, ViewResourcesActivity.class)));

        viewQuizzesButton.setOnClickListener(v ->
                startActivity(new Intent(this, ViewQuizzesActivity.class)));

        viewBookingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewBookingsActivity.class);
            intent.putExtra("role", "Tutee");
            startActivity(intent);
        });

        // AI Assistant FAB
        fabAiAssistant.setOnClickListener(v ->
                startActivityForResult(new Intent(this, AiChatActivity.class), AI_CHAT_REQUEST));

        // Profile image click
        profileImage.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        // Join class button
        joinClassBtn.setOnClickListener(v -> {
            if (activeBookingId != null) {
                checkAndJoinSession();
            }
        });
    }

    private void checkAndJoinSession() {
        DatabaseReference bookingRef = bookingsRef.child(activeBookingId);
        bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Booking booking = snapshot.getValue(Booking.class);
                if (booking != null && "active".equals(booking.getStatus())) {
                    Intent intent = new Intent(TuteeHomeActivity.this, VideoCallActivity.class);
                    intent.putExtra("bookingId", activeBookingId);
                    startActivity(intent);
                } else {
                    Toast.makeText(TuteeHomeActivity.this, "Session not active yet", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Booking check failed", error.toException());
            }
        });
    }
    private void showTutorOptionsDialog(Tutor tutor) {
        boolean isRegistered = existingRegistrations.containsKey(tutor.getId()) &&
                !existingRegistrations.get(tutor.getId()).isEmpty();

        String[] options = isRegistered ?
                new String[]{"Book Session", "Register More Subjects", "View Profile"} :
                new String[]{"Register Subjects", "View Profile"};

        // Inflate custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_tutor_dialog, null);

        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Get references to views
        TextView title = dialogView.findViewById(R.id.dialog_title);
        LinearLayout optionsContainer = dialogView.findViewById(R.id.options_container);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);

        // Add options dynamically
        for (String option : options) {
            TextView optionView = new TextView(this);
            optionView.setText(option);
            optionView.setTextColor(Color.BLACK);
            optionView.setTextSize(16);
            optionView.setPadding(
                    getResources().getDimensionPixelSize(R.dimen.dialog_option_padding),
                    getResources().getDimensionPixelSize(R.dimen.dialog_option_padding),
                    getResources().getDimensionPixelSize(R.dimen.dialog_option_padding),
                    getResources().getDimensionPixelSize(R.dimen.dialog_option_padding)
            );
            optionView.setGravity(Gravity.CENTER);
            optionView.setBackground(ContextCompat.getDrawable(this, R.drawable.option_selector));
            optionView.setOnClickListener(v -> {
                // Handle option selection directly here
                switch (option) {
                    case "Book Session":
                        showDatePickerForSlots(tutor);
                        break;
                    case "Register More Subjects":
                    case "Register Subjects":
                        showTutorSubjectsDialog(tutor);
                        break;
                    case "View Profile":
                        viewTutorProfile(tutor);
                        break;
                }
                dialog.dismiss();
            });

            optionsContainer.addView(optionView);
        }

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Set dialog window properties
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.5f);
            window.getAttributes().windowAnimations = R.style.DialogAnimation;
        }

        dialog.show();
    }

// Add to res/values/dimens.xml


    private void viewTutorProfile(Tutor tutor) {
        Intent intent = new Intent(this, TutorProfileActivity.class);
        intent.putExtra("tutorId", tutor.getId());
        startActivity(intent);
    }

    private void handleOptionSelection(String option, Tutor tutor) {
        switch (option) {
            case "Book Session":
                showDatePickerForSlots(tutor);
                break;
            case "Register More Subjects":
            case "Register Subjects":
                showTutorSubjectsDialog(tutor);
                break;
            case "View Profile":
                viewTutorProfile(tutor);
                break;
            default:
                Log.w("Dialog", "Unknown option selected: " + option);
        }
    }

    private void showDatePickerForSlots(Tutor tutor) {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            showAvailableSlots(tutor, selected.getTimeInMillis());
        },
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void showAvailableSlots(Tutor tutor, long selectedDateMillis) {
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.setTimeInMillis(selectedDateMillis);
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);

        Calendar endOfDay = Calendar.getInstance();
        endOfDay.setTimeInMillis(selectedDateMillis);
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);

        availabilityRef.child(tutor.getId()).orderByChild("startTime")
                .startAt(startOfDay.getTimeInMillis())
                .endAt(endOfDay.getTimeInMillis())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<AvailabilitySlot> oneOnOneSlots = new ArrayList<>();
                        List<AvailabilitySlot> quickHelpSlots = new ArrayList<>();
                        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, hh:mm a", Locale.getDefault());

                        for (DataSnapshot slotSnapshot : snapshot.getChildren()) {
                            if ("available".equals(slotSnapshot.child("status").getValue(String.class))) {
                                long startTime = slotSnapshot.child("startTime").getValue(Long.class);
                                long endTime = slotSnapshot.child("endTime").getValue(Long.class);
                                String sessionType = slotSnapshot.child("sessionType").getValue(String.class);

                                AvailabilitySlot slot = new AvailabilitySlot(
                                        slotSnapshot.getKey(),
                                        tutor.getId(),
                                        startTime,
                                        endTime,
                                        sessionType != null ? sessionType : "one-on-one"
                                );

                                if ("quick-help".equals(sessionType)) {
                                    quickHelpSlots.add(slot);
                                } else {
                                    oneOnOneSlots.add(slot);
                                }
                            }
                        }

                        if (oneOnOneSlots.isEmpty() && quickHelpSlots.isEmpty()) {
                            Toast.makeText(TuteeHomeActivity.this,
                                    "No available slots", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        showSessionTypeSelection(tutor, oneOnOneSlots, quickHelpSlots);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(TuteeHomeActivity.this, "Failed to load slots", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showSessionTypeSelection(Tutor tutor, List<AvailabilitySlot> oneOnOneSlots, List<AvailabilitySlot> quickHelpSlots) {
        List<String> options = new ArrayList<>();
        if (!oneOnOneSlots.isEmpty()) options.add("One-on-One Session (30 min)");

        new AlertDialog.Builder(this)
                .setTitle("Choose Session Type")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    String selected = options.get(which);
                        showOneOnOneSlotSelection(tutor, oneOnOneSlots);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showOneOnOneSlotSelection(Tutor tutor, List<AvailabilitySlot> slots) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, hh:mm a", Locale.getDefault());
        String[] slotStrings = new String[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            AvailabilitySlot slot = slots.get(i);
            slotStrings[i] = sdf.format(new Date(slot.getStartTime())) + " - " +
                    sdf.format(new Date(slot.getEndTime())) + " (30 min)";
        }

        new AlertDialog.Builder(this)
                .setTitle("Available One-on-One Slots")
                .setItems(slotStrings, (dialog, which) ->
                        showBookingDialog(tutor, slots.get(which)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBookingDialog(Tutor tutor, AvailabilitySlot slot) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppDialogTheme);

        // Create custom view with maintained colors
        View view = getLayoutInflater().inflate(R.layout.dialog_booking, null);
        TextView title = view.findViewById(R.id.dialogTitle);
        TextView message = view.findViewById(R.id.dialogMessage);
        ListView subjectsList = view.findViewById(R.id.subjectsList);

        title.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        message.setTextColor(ContextCompat.getColor(this, R.color.colorTextPrimary));

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, hh:mm a", Locale.getDefault());
        String slotTime = sdf.format(new Date(slot.getStartTime())) + " - " +
                sdf.format(new Date(slot.getEndTime())) + " (30 min)";

        message.setText("Select a subject for your session with " + tutor.getDisplayName() + ":\n\n" + slotTime);

        final String[] subjectsArray = tutor.getSubjects().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.item_subject, R.id.subjectText, subjectsArray);
        subjectsList.setAdapter(adapter);

        builder.setView(view)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    int pos = subjectsList.getCheckedItemPosition();
                    if (pos != ListView.INVALID_POSITION) {
                        createOneOnOneBooking(tutor.getId(), slot, subjectsArray[pos]);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void createOneOnOneBooking(String tutorId, AvailabilitySlot slot, String subject) {
        String tuteeId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String bookingId = FirebaseDatabase.getInstance().getReference().child("bookings").push().getKey();

        // Step 1: Load tutor User from Firebase
        FirebaseDatabase.getInstance().getReference("users").child(tutorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User tutorUser = snapshot.getValue(User.class);
                        if (tutorUser == null || !"Tutor".equals(tutorUser.getRole())) {
                            Toast.makeText(getApplicationContext(), "Invalid tutor.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Step 2: Create Booking using tutorUser
                        Booking booking = new Booking();
                        booking.setBookingId(bookingId);
                        booking.setTuteeId(tuteeId);
                        booking.setTutorId(tutorId);
                        booking.setSubject(subject);
                        booking.setSlotId(slot.getSlotId());
                        booking.setStartTime(slot.getStartTime());
                        booking.setEndTime(slot.getEndTime());
                        booking.setDuration(30);
                        booking.setSessionType("one-on-one");
                        booking.setStatus("confirmed");
                        booking.setCreatedAt(System.currentTimeMillis());
                        booking.setCompleted(false);
                        booking.setRated(false);
                        booking.setTeamsJoinUrl("https://teams.microsoft.com/l/meetup-join/19%3ameeting_N2Q5YjBjNzUtYTU2NS00ZGFkLWFiMWQtYWUzMmJjNzBjYjRk%40thread.v2/0?context=%7b%22Tid%22%3a%224b1930d1-12f4-40b5-b48c-bd86117429d8%22%2c%22Oid%22%3a%221f548ce5-cf56-43d3-a2a1-85851101964d%22%7d");
// ✅ fixed line

                        // Step 3: Write booking and mark slot booked
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("/bookings/" + bookingId, booking);
                        updates.put("/availability/" + tutorId + "/" + slot.getSlotId() + "/status", "booked");

                        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getApplicationContext(), "Booking confirmed!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getApplicationContext(), "Booking failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("Booking", "Error creating booking", e);
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getApplicationContext(), "Failed to load tutor", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void showTutorSubjectsDialog(Tutor tutor) {
        Set<String> alreadyRegistered = existingRegistrations.getOrDefault(tutor.getId(), new HashSet<>());
        List<String> availableSubjects = new ArrayList<>();
        for (String subject : tutor.getSubjects()) {
            if (!alreadyRegistered.contains(subject)) availableSubjects.add(subject);
        }

        if (availableSubjects.isEmpty()) {
            Toast.makeText(this, "Already registered for all subjects", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean[] checkedItems = new boolean[availableSubjects.size()];
        new AlertDialog.Builder(this)
                .setTitle("Register for Subjects")
                .setMultiChoiceItems(
                        availableSubjects.toArray(new String[0]),
                        checkedItems,
                        (dialog, which, isChecked) -> checkedItems[which] = isChecked
                )
                .setPositiveButton("Register", (dialog, which) -> {
                    List<String> selectedSubjects = new ArrayList<>();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            selectedSubjects.add(availableSubjects.get(i));
                        }
                    }
                    registerForSubjects(tutor.getId(), selectedSubjects);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void registerForSubjects(String tutorId, List<String> subjects) {
        if (subjects.isEmpty()) return;

        String tuteeId = mAuth.getCurrentUser().getUid();
        DatabaseReference registrationRef = registrationsRef.child(tuteeId).child(tutorId);

        Map<String, Object> updates = new HashMap<>();
        for (String subject : subjects) updates.put(subject, true);

        registrationRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Set<String> registered = existingRegistrations.computeIfAbsent(tutorId, k -> new HashSet<>());
                    registered.addAll(subjects);
                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show());
    }

    public static class Tutor {
        private String id,name, email;
        private List<String> subjects;
        private float averageRating;
        private String teamsMeetingUrl;

        public Tutor() {} // Required for Firebase

        public Tutor(String id, String name, String email, List<String> subjects, float averageRating, String teamsMeetingUrl) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.subjects = subjects;
            this.averageRating = averageRating;
            this.teamsMeetingUrl = teamsMeetingUrl;
        }


        public Tutor(String id,String name, String email, List<String> subjects, float averageRating) {
            this.id = id;
            this.email = email;
            this.name = name;
            this.subjects = subjects;
            this.averageRating = averageRating;
        }

        public String getId() { return id; }
        public String getEmail() { return email; }
        public List<String> getSubjects() { return subjects; }
        public float getAverageRating() { return averageRating; }
        public void setAverageRating(float averageRating) {
            this.averageRating = averageRating;
        }

        public String getDisplayName(){
            return name;
        }

        public String getTeamsMeetingUrl() {
            return teamsMeetingUrl;
        }

        public void setTeamsMeetingUrl(String teamsMeetingUrl) {
            this.teamsMeetingUrl = teamsMeetingUrl;
        }

    }

    public static class AvailabilitySlot {
        private String slotId, tutorId, status, sessionType;
        private long startTime, endTime;

        public AvailabilitySlot(String slotId, String tutorId, long startTime, long endTime, String sessionType) {
            this.slotId = slotId;
            this.tutorId = tutorId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.status = "available";
            this.sessionType = sessionType;
        }
        public void setSlotId(String slotId) {this.slotId=slotId;}
        public String getSlotId() { return slotId; }
        public String getTutorId() { return tutorId; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public String getSessionType() { return sessionType; }
    }
}