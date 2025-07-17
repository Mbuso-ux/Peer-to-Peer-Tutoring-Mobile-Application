package com.example.peertut;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class TutorSetupActivity extends AppCompatActivity {

    private EditText subjectsEditText;
    private Button saveSubjectsButton;
    private DatabaseReference usersRef;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_setup);

        subjectsEditText = findViewById(R.id.subjectsEditText);
        saveSubjectsButton = findViewById(R.id.saveSubjectsButton);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        saveSubjectsButton.setOnClickListener(v -> {
            String subjectsInput = subjectsEditText.getText().toString().trim();
            if (subjectsInput.isEmpty()) {
                Toast.makeText(this, "Please enter at least one subject", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] subjectArray = subjectsInput.split(",");
            for (int i = 0; i < subjectArray.length; i++) {
                subjectArray[i] = subjectArray[i].trim();
            }

            String uid = auth.getCurrentUser().getUid();
            usersRef.child(uid).child("subjects").setValue(subjectArray)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Subjects saved!", Toast.LENGTH_SHORT).show();
                        // Navigate to main Tutor screen
                        startActivity(new Intent(TutorSetupActivity.this, TutorHomeActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save subjects", Toast.LENGTH_SHORT).show()
                    );
        });
    }
}
