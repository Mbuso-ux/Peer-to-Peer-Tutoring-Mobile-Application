package com.example.peertut;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class BookSessionActivity extends AppCompatActivity {
    private Spinner tutorSpinner, subjectSpinner, slotSpinner;
    private Button bookButton;
    private DatabaseReference usersRef, bookingsRef;
    private FirebaseAuth auth;

    private List<String> tutorList = new ArrayList<>();
    private List<String> subjectList = new ArrayList<>();
    private List<String> slotList = new ArrayList<>();

    private ArrayAdapter<String> tutorAdapter, subjectAdapter, slotAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_session);

        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        String currentUid = FirebaseAuth.getInstance().getUid();

        tutorSpinner = findViewById(R.id.tutorSpinner);
        subjectSpinner = findViewById(R.id.subjectSpinner);
        slotSpinner = findViewById(R.id.slotSpinner);
        bookButton = findViewById(R.id.bookButton);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");

        tutorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tutorList);
        subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjectList);
        slotAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, slotList);

        tutorSpinner.setAdapter(tutorAdapter);
        subjectSpinner.setAdapter(subjectAdapter);
        slotSpinner.setAdapter(slotAdapter);

        generateTimeSlots();
        loadTutors();

        tutorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String tutorUid = tutorList.get(position);
                loadSubjectsForTutor(tutorUid);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        bookButton.setOnClickListener(v -> bookSession());
    }

    private void generateTimeSlots() {
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri"};
        for (String day : days) {
            for (int h = 8; h < 11; h++) {
                slotList.add(day + " " + h + ":00");
                slotList.add(day + " " + h + ":30");
            }
            slotList.add(day + " 12:00");
            slotList.add(day + " 12:30");
            slotList.add(day + " 13:00");
            slotList.add(day + " 13:30");
        }
        slotAdapter.notifyDataSetChanged();
    }

    private void loadTutors() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tutorList.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    if (s.hasChild("subjects")) tutorList.add(s.getKey());
                }
                tutorAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadSubjectsForTutor(String tutorUid) {
        usersRef.child(tutorUid).child("subjects").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                subjectList.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    subjectList.add(s.getValue(String.class));
                }
                subjectAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void bookSession() {
        String tuteeUid = auth.getCurrentUser().getUid();
        String tutorUid = (String) tutorSpinner.getSelectedItem();
        String subject = (String) subjectSpinner.getSelectedItem();
        String slot = (String) slotSpinner.getSelectedItem();

        if (tutorUid == null || subject == null || slot == null) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        long startTime = convertSlotToTimestamp(slot); // you define this
        if (startTime == -1) {
            Toast.makeText(this, "Invalid slot", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> session = new HashMap<>();
        session.put("tuteeId", tuteeUid);
        session.put("tutorId", tutorUid);
        session.put("subject", subject);
        session.put("slot", slot);
        session.put("startTime", startTime);
        session.put("status", "upcoming");

        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        bookingsRef.push().setValue(session)
                .addOnSuccessListener(a -> Toast.makeText(this, "Session booked", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private long convertSlotToTimestamp(String slot) {
        try {
            Calendar calendar = Calendar.getInstance();
            String[] parts = slot.split(" ");
            String day = parts[0];
            String time = parts[1];

            Map<String, Integer> dayMap = new HashMap<>();
            dayMap.put("Mon", Calendar.MONDAY);
            dayMap.put("Tue", Calendar.TUESDAY);
            dayMap.put("Wed", Calendar.WEDNESDAY);
            dayMap.put("Thu", Calendar.THURSDAY);
            dayMap.put("Fri", Calendar.FRIDAY);

            int dayOfWeek = dayMap.getOrDefault(day, -1);
            if (dayOfWeek == -1) return -1;

            calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
            String[] hm = time.split(":");
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hm[0]));
            calendar.set(Calendar.MINUTE, Integer.parseInt(hm[1]));
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // Ensure it's in the future — shift to next week if past
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
            }

            return calendar.getTimeInMillis();
        } catch (Exception e) {
            return -1;
        }
    }

}
