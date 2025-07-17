package com.example.peertut;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SignupActivity extends AppCompatActivity {

    private EditText nameInput, emailInput, passwordInput;
    private EditText institutionInput, subjectsInput, academicLevelInput, ageInput, goalsInput;
    private EditText degreesInput, institutionsAttendedInput, experienceInput, philosophyInput;
    private RadioGroup roleGroup;
    private LinearLayout tuteeFields, tutorFields;
    private MaterialButton sendVerificationBtn;
    private TextView verificationStatus;
    private ProgressBar progressBar;
    private String userRole = "";

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("users");

        initializeViews();
        setupRoleVisibility();

        sendVerificationBtn.setOnClickListener(v -> registerAndSendVerification());
    }

    private void initializeViews() {
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        institutionInput = findViewById(R.id.institutionInput);
        subjectsInput = findViewById(R.id.subjectsInput);
        academicLevelInput = findViewById(R.id.academicLevelInput);
        ageInput = findViewById(R.id.ageInput);
        goalsInput = findViewById(R.id.goalsInput);
        degreesInput = findViewById(R.id.degreesInput);
        institutionsAttendedInput = findViewById(R.id.institutionsAttendedInput);
        experienceInput = findViewById(R.id.experienceInput);
        philosophyInput = findViewById(R.id.philosophyInput);

        roleGroup = findViewById(R.id.roleGroup);
        tuteeFields = findViewById(R.id.tuteeFields);
        tutorFields = findViewById(R.id.tutorFields);
        sendVerificationBtn = findViewById(R.id.sendVerificationBtn);
        verificationStatus = findViewById(R.id.verificationStatus);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRoleVisibility() {
        roleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioTutor) {
                userRole = "Tutor";
                tutorFields.setVisibility(View.VISIBLE);
                tuteeFields.setVisibility(View.GONE);
            } else if (checkedId == R.id.radioTutee) {
                userRole = "Tutee";
                tutorFields.setVisibility(View.GONE);
                tuteeFields.setVisibility(View.VISIBLE);
            }
        });
    }

    private void registerAndSendVerification() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || userRole.isEmpty()) {
            showToast("All fields and role selection are required");
            return;
        }
        if (!email.endsWith("@dut4life.ac.za")) {
            showToast("Use a DUT email address");
            return;
        }

        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                if (firebaseUser != null) {
                    saveUserToDatabase(firebaseUser, () -> {
                        firebaseUser.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                            if (verifyTask.isSuccessful()) {
                                showToast("Verification email sent. Please check your inbox.");
                                verificationStatus.setText("\u2713 Verification Sent");
                                verificationStatus.setTextColor(ContextCompat.getColor(this, R.color.green));
                                verificationStatus.setVisibility(View.VISIBLE);
                                mAuth.signOut();
                                finish();
                            } else {
                                firebaseUser.delete();
                                showToast("Failed to send verification email.");
                            }
                            showLoading(false);
                        });
                    });
                }
            } else {
                showLoading(false);
                showToast("Signup failed: " + task.getException().getMessage());
            }
        });
    }

    private void saveUserToDatabase(FirebaseUser firebaseUser, Runnable onSuccess) {
        User user = new User();
        user.setUid(firebaseUser.getUid());
        user.setName(nameInput.getText().toString().trim());
        user.setEmail(emailInput.getText().toString().trim());
        user.setRole(userRole);
        user.setInstitution(institutionInput.getText().toString().trim());

        if (userRole.equals("Tutee")) {
            user.setSubjects(Collections.singletonList(subjectsInput.getText().toString().trim()));
            user.setAcademicLevel(academicLevelInput.getText().toString().trim());
            try {
                user.setAge(Integer.parseInt(ageInput.getText().toString().trim()));
            } catch (NumberFormatException ignored) {}
            user.setShortTermGoals(goalsInput.getText().toString().trim());
        } else if (userRole.equals("Tutor")) {
            user.setDegrees(parseList(degreesInput.getText().toString()));
            user.setInstitutionsAttended(parseList(institutionsAttendedInput.getText().toString()));
            user.setTeachingExperience(experienceInput.getText().toString().trim());
            user.setTeachingPhilosophy(philosophyInput.getText().toString().trim());
            user.setRating(0.0);
        }

        dbRef.child(firebaseUser.getUid()).setValue(user)
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(e -> {
                    showToast("Failed to save profile: " + e.getMessage());
                    mAuth.getCurrentUser().delete();
                    showLoading(false);
                });
    }

    private List<String> parseList(String input) {
        return Arrays.asList(input.trim().split(",[ ]*"));
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        sendVerificationBtn.setEnabled(!loading);
    }
}
