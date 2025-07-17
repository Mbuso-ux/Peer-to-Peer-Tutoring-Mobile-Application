package com.example.peertut;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class CalendarAvailabilityActivity extends AppCompatActivity {

    private DatabaseReference availabilityRef;
    private LinearLayout calendarContainer;
    private String tutorUid = "YOUR_TUTOR_UID"; // Replace or pass via Intent

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calender);

        calendarContainer = findViewById(R.id.calendarContainer);

        availabilityRef = FirebaseDatabase.getInstance().getReference("availability").child(tutorUid);

        fetchAndDisplayAvailability();
    }

    private void fetchAndDisplayAvailability() {
        availabilityRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                TreeMap<String, Map<String, Object>> sortedSlots = new TreeMap<>();
                for (DataSnapshot slotSnap : snapshot.getChildren()) {
                    sortedSlots.put(slotSnap.getKey(), (Map<String, Object>) slotSnap.getValue());
                }

                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale.getDefault());

                String currentDay = "";
                LinearLayout dayLayout = null;

                for (Map.Entry<String, Map<String, Object>> entry : sortedSlots.entrySet()) {
                    try {
                        Calendar slotCal = Calendar.getInstance();
                        slotCal.setTime(fullFormat.parse(entry.getKey()));
                        String day = dayFormat.format(slotCal.getTime());

                        if (!day.equals(currentDay)) {
                            currentDay = day;

                            dayLayout = new LinearLayout(CalendarAvailabilityActivity.this);
                            dayLayout.setOrientation(LinearLayout.VERTICAL);
                            dayLayout.setPadding(0, 40, 0, 10);

                            TextView dayTitle = new TextView(CalendarAvailabilityActivity.this);
                            dayTitle.setText(day);
                            dayTitle.setTextSize(20f);
                            dayTitle.setTextColor(Color.BLACK);
                            dayTitle.setPadding(0, 0, 0, 10);

                            dayLayout.addView(dayTitle);
                            calendarContainer.addView(dayLayout);
                        }

                        String time = timeFormat.format(slotCal.getTime());
                        boolean booked = Boolean.TRUE.equals(entry.getValue().get("booked"));

                        TextView timeView = new TextView(CalendarAvailabilityActivity.this);
                        timeView.setText(time + (booked ? " (Booked)" : " (Available)"));
                        timeView.setTextSize(16f);
                        timeView.setPadding(20, 10, 20, 10);
                        timeView.setBackgroundColor(booked ? Color.LTGRAY : Color.parseColor("#A5D6A7"));

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(10, 5, 10, 5);
                        timeView.setLayoutParams(params);

                        if (dayLayout != null) {
                            dayLayout.addView(timeView);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (sortedSlots.isEmpty()) {
                    Toast.makeText(CalendarAvailabilityActivity.this, "No availability found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(CalendarAvailabilityActivity.this, "Failed to load availability", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
