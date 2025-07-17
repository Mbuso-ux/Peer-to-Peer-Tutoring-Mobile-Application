package com.example.peertut;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;

public class EditSubjectsActivity extends AppCompatActivity {

    private TextInputEditText subjectsEditText;
    private MaterialButton saveSubjectsButton;
    private DatabaseReference usersRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_subjects);

        // Initialize views
        subjectsEditText = findViewById(R.id.subjectsEditText);
        saveSubjectsButton = findViewById(R.id.saveSubjectsButton);

        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Subjects");
        }

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        saveSubjectsButton.setOnClickListener(v -> {
            String input = subjectsEditText.getText().toString().trim();
            if (!input.isEmpty()) {
                List<String> subjectsList = Arrays.asList(input.split(","));
                String uid = mAuth.getCurrentUser().getUid();

                // Clear previous subjects
                usersRef.child(uid).child("subjects").removeValue();

                // Add new subjects
                for (int i = 0; i < subjectsList.size(); i++) {
                    usersRef.child(uid).child("subjects")
                            .child(String.valueOf(i))
                            .setValue(subjectsList.get(i).trim());
                }

                Toast.makeText(this, "Subjects saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Please enter at least one subject", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}