package com.example.peertut;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ManageBookingsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BookingsAdapter adapter;
    private DatabaseReference bookingsRef;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_bookings);

        role = getIntent().getStringExtra("role");  // "Tutor" or "Tutee"
        recyclerView = findViewById(R.id.manageBookingsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BookingsAdapter(
                new ArrayList<>(),
                new SimpleDateFormat("EEE, MMM d, h:mm a", Locale.getDefault()),
                new BookingsAdapter.BookingActionListener() {
                    @Override
                    public void onCompleteBooking(Booking b) {
                        if (b.getBookingId() != null) {
                            bookingsRef.child(b.getBookingId()).child("status").setValue("ended")
                                    .addOnSuccessListener(aVoid -> loadBookings());
                        }
                    }

                    @Override
                    public void onRateTutor(Booking b) {
                        // TODO: Show rating UI
                    }

                    @Override
                    public void onViewBooking(Booking b) {
                        // TODO: Show booking details
                    }
                }
        );

        recyclerView.setAdapter(adapter);
        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        loadBookings();
    }

    private void loadBookings() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String key = role.equals("Tutor") ? "tutorId" : "tuteeId";

        bookingsRef.orderByChild(key).equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Booking> list = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Booking b = ds.getValue(Booking.class);
                            if (b != null) {
                                b.setBookingId(ds.getKey());
                                list.add(b);
                            }
                        }

                        // Sort: latest bookings first
                        Collections.sort(list, (b1, b2) -> Long.compare(b2.getStartTime(), b1.getStartTime()));

                        adapter.updateBookings(list);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error if needed
                    }
                });
    }
}
