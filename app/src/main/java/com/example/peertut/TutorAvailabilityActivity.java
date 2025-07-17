package com.example.peertut;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TutorAvailabilityActivity extends AppCompatActivity {

    private FloatingActionButton addSlotButton;
    private ListView slotsListView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyStateLayout;
    private Button dateSelectorButton;
    private DatabaseReference availabilityRef;
    private ArrayAdapter<AvailabilitySlot> slotsAdapter;
    private final List<AvailabilitySlot> slotsList = new ArrayList<>();
    private Calendar selectedDate = Calendar.getInstance();
    private Calendar startOfDay;
    private Calendar endOfDay;

    public static class AvailabilitySlot {
        private String slotId;
        private String tutorId;
        private long startTime;
        private long endTime;
        private String status;
        private String sessionType = "one-on-one";
        private String bookedSubject;

        public AvailabilitySlot() {}

        public AvailabilitySlot(String slotId, String tutorId, long startTime, long endTime) {
            this.slotId = slotId;
            this.tutorId = tutorId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.status = "available";
        }

        // Add getter and setter
        public String getBookedSubject() { return bookedSubject; }
        public void setBookedSubject(String bookedSubject) { this.bookedSubject = bookedSubject; }
        // Getters and setters
        public String getSlotId() { return slotId; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public void setSlotId(String slotId) {
            this.slotId= slotId;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_availability);

        initializeViews();
        setupFirebase();
        setupAdapter();
        setupListeners();
        loadAvailabilitySlots();
        initializeDayRange(); // Add this line
    }

    private void initializeViews() {
        addSlotButton = findViewById(R.id.addSlotButton);
        slotsListView = findViewById(R.id.slotsListView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        dateSelectorButton = findViewById(R.id.dateSelectorButton);
        updateDateButtonText();
    }

    private void initializeDayRange() {
        startOfDay = Calendar.getInstance();
        endOfDay = Calendar.getInstance();
        updateDayRange(); // Initialize based on selectedDate
    }

    private void setupFirebase() {
        String tutorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        availabilityRef = FirebaseDatabase.getInstance().getReference("availability").child(tutorId);
    }

    private void setupAdapter() {
        slotsAdapter = new ArrayAdapter<AvailabilitySlot>(this, R.layout.item_slot, R.id.slotTimeText, slotsList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                AvailabilitySlot slot = getItem(position);

                TextView slotTimeText = view.findViewById(R.id.slotTimeText);
                TextView slotDurationText = view.findViewById(R.id.slotDurationText);
                TextView slotStatusText = view.findViewById(R.id.slotStatusText);
                View slotStatusIndicator = view.findViewById(R.id.slotStatusIndicator);

                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                String timeRange = timeFormat.format(new Date(slot.getStartTime())) + " - " +
                        timeFormat.format(new Date(slot.getEndTime()));

                slotTimeText.setText(timeRange);
                long duration = (slot.getEndTime() - slot.getStartTime()) / (60 * 1000);
                slotDurationText.setText(duration + " min");
                String statusText = slot.getStatus().equals("available") ?
                        "Available" :
                        "Booked: " + (slot.getBookedSubject() != null ? slot.getBookedSubject() : "N/A");

                slotStatusText.setText(statusText);

                // Set background color based on status
                if (slot.getStatus().equals("booked")) {
                    view.setBackgroundColor(Color.parseColor("#FFF3E0")); // Light orange
                    slotStatusIndicator.setBackgroundColor(Color.parseColor("#F44336")); // Red
                } else {
                    view.setBackgroundColor(Color.WHITE);
                    slotStatusIndicator.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
                }

                return view;
            }
        };
        slotsListView.setAdapter(slotsAdapter);
    }

    private void setupListeners() {
        dateSelectorButton.setOnClickListener(v -> showDatePicker());
        addSlotButton.setOnClickListener(v -> showDatePickerForNewSlot());
        swipeRefreshLayout.setOnRefreshListener(this::loadAvailabilitySlots);

        slotsListView.setOnItemClickListener((parent, view, position, id) -> {
            AvailabilitySlot slot = slotsAdapter.getItem(position);
            showSlotOptionsDialog(slot);
        });
    }

    private void showDatePickerForNewSlot() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            if (selected.before(Calendar.getInstance())) {
                Toast.makeText(this, "Please select a future date", Toast.LENGTH_SHORT).show();
                return;
            }
            showTimePickerForNewSlot(selected);
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void showTimePickerForNewSlot(Calendar date) {
        final Calendar calendar = (Calendar) date.clone();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hour, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);

                    if (calendar.before(Calendar.getInstance())) {
                        Toast.makeText(this, "Please select a future time", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Set fixed 30-minute duration
                    long startTime = calendar.getTimeInMillis();
                    long endTime = startTime + (30 * 60 * 1000);
                    checkForDuplicateSlot(startTime, endTime);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void checkForDuplicateSlot(long startTime, long endTime) {
        availabilityRef.orderByChild("startTime").endAt(endTime)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot slotSnapshot : snapshot.getChildren()) {
                            Long existingStart = slotSnapshot.child("startTime").getValue(Long.class);
                            Long existingEnd = slotSnapshot.child("endTime").getValue(Long.class);

                            if (existingStart != null && existingEnd != null &&
                                    (startTime < existingEnd && endTime > existingStart)) {
                                Toast.makeText(TutorAvailabilityActivity.this,
                                        "Time slot overlaps with existing slot",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        addAvailabilitySlot(startTime, endTime);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(TutorAvailabilityActivity.this,
                                "Error checking slots", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addAvailabilitySlot(long startTime, long endTime) {
        String slotKey = availabilityRef.push().getKey();
        AvailabilitySlot newSlot = new AvailabilitySlot(
                slotKey,
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                startTime,
                endTime
        );

        availabilityRef.child(slotKey).setValue(newSlot)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Slot added", Toast.LENGTH_SHORT).show();
                    loadAvailabilitySlots();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add slot", Toast.LENGTH_SHORT).show());
    }

    private void loadAvailabilitySlots() {
        swipeRefreshLayout.setRefreshing(true);
        String tutorId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Load bookings for this tutor
        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        bookingsRef.orderByChild("tutorId").equalTo(tutorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {


                    @Override
                    public void onDataChange(@NonNull DataSnapshot bookingSnapshot) {
                        Map<String, String> slotSubjects = new HashMap<>();
                        for (DataSnapshot ds : bookingSnapshot.getChildren()) {
                            String slotId = ds.child("slotId").getValue(String.class);
                            String subject = ds.child("subject").getValue(String.class);
                            if (slotId != null && subject != null) {
                                slotSubjects.put(slotId, subject);
                            }
                        }

                        // Now load availability slots
                        availabilityRef.orderByChild("startTime")
                                .startAt(startOfDay.getTimeInMillis())
                                .endAt(endOfDay.getTimeInMillis())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        slotsList.clear();
                                        for (DataSnapshot slotSnapshot : snapshot.getChildren()) {
                                            AvailabilitySlot slot = slotSnapshot.getValue(AvailabilitySlot.class);
                                            if (slot != null) {
                                                slot.setSlotId(slotSnapshot.getKey());
                                                if (slot.getStatus().equals("booked")) {
                                                    slot.setBookedSubject(slotSubjects.get(slot.getSlotId()));
                                                }
                                                slotsList.add(slot);
                                            }
                                        }
                                        Collections.sort(slotsList, Comparator.comparingLong(AvailabilitySlot::getStartTime));
                                        slotsAdapter.notifyDataSetChanged();
                                        emptyStateLayout.setVisibility(slotsList.isEmpty() ? View.VISIBLE : View.GONE);
                                        swipeRefreshLayout.setRefreshing(false);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        // Handle error
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
    }

    private void showSlotOptionsDialog(AvailabilitySlot slot) {
        if (slot.getStatus().equals("booked")) {
            new AlertDialog.Builder(this)
                    .setTitle("Booked Slot")
                    .setItems(new String[]{"View Tutee Profile"}, (dialog, which) -> {
                        getTuteeInfo(slot.getSlotId());
                    })
                    .setNegativeButton("Close", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Slot Options")
                    .setItems(new String[]{"Delete", "Duplicate"}, (dialog, which) -> {
                        if (which == 0) deleteSlot(slot.getSlotId());
                        else duplicateSlot(slot);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void getTuteeInfo(String slotId) {
        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        bookingsRef.orderByChild("slotId").equalTo(slotId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                            String tuteeId = bookingSnapshot.child("tuteeId").getValue(String.class);
                            if (tuteeId != null) {
                                launchTuteeProfile(tuteeId);
                                return;
                            }
                        }
                        Toast.makeText(TutorAvailabilityActivity.this,
                                "Tutee information not found", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(TutorAvailabilityActivity.this,
                                "Error loading tutee info", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void launchTuteeProfile(String tuteeId) {
        Intent intent = new Intent(this, TuteeProfileActivity.class);
        intent.putExtra("tuteeId", tuteeId);
        startActivity(intent);
    }
    private void updateDayRange() {
        startOfDay.setTimeInMillis(selectedDate.getTimeInMillis());
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        endOfDay.setTimeInMillis(selectedDate.getTimeInMillis());
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);
    }

    // Remaining helper methods
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    updateDayRange(); // Add this line
                    updateDateButtonText();
                    loadAvailabilitySlots();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void updateDateButtonText() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        dateSelectorButton.setText(dateFormat.format(selectedDate.getTime()));
    }

    private void deleteSlot(String slotKey) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this slot?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    availabilityRef.child(slotKey).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Slot deleted", Toast.LENGTH_SHORT).show();
                                loadAvailabilitySlots();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to delete slot", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void duplicateSlot(AvailabilitySlot slot) {
        long newStartTime = slot.getStartTime() + (24 * 60 * 60 * 1000);
        long newEndTime = slot.getEndTime() + (24 * 60 * 60 * 1000);
        checkForDuplicateSlot(newStartTime, newEndTime);
    }
}