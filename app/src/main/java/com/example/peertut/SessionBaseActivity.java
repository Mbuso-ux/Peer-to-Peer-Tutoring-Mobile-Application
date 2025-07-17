package com.example.peertut;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

public abstract class SessionBaseActivity extends AppCompatActivity {
    protected String bookingId;
    protected String otherUserId;
    protected DatabaseReference sessionRef;
    protected boolean isTutor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        bookingId = getIntent().getStringExtra("bookingId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        isTutor = getIntent().getBooleanExtra("isTutor", false);
        
        sessionRef = FirebaseDatabase.getInstance().getReference("sessions").child(bookingId);
        setupSessionTimer();
    }

    private void setupSessionTimer() {
        sessionRef.child("startTime").setValue(ServerValue.TIMESTAMP);
        
        sessionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long endTime = snapshot.child("endTime").getValue(Long.class);
                if (endTime != null && endTime < System.currentTimeMillis()) {
                    endSession();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SessionBase", "Error listening to session end", error.toException());
            }
        });
    }

    protected void endSession() {
        sessionRef.child("endTime").setValue(ServerValue.TIMESTAMP);
        finish();
    }

    protected abstract void setupUI();
    protected abstract void cleanupResources();
}