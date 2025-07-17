package com.example.peertut;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // If user is not signed in, go to login screen
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Get user ID and check their role in the database
        String uid = currentUser.getUid();
        dbRef = FirebaseDatabase.getInstance().getReference("users");

        dbRef.child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String role = task.getResult().child("role").getValue(String.class);
                Intent intent;

                if ("Tutor".equalsIgnoreCase(role)) {
                    intent = new Intent(MainActivity.this, TutorHomeActivity.class);
                } else {
                    intent = new Intent(MainActivity.this, TuteeHomeActivity.class);
                }

                startActivity(intent);
                finish();
            } else {
                Toast.makeText(MainActivity.this, "User role not found", Toast.LENGTH_SHORT).show();
                FirebaseAuth.getInstance().signOut(); // Force logout if data missing
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
}
