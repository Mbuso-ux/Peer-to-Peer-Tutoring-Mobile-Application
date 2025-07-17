package com.example.peertut;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 101;

    private ImageView profileImage;
    private EditText nameInput, institutionInput, subjectsInput, academicLevelInput, ageInput, goalsInput;
    private EditText degreesInput, institutionsInput, experienceInput, philosophyInput;
    private TextView emailText, roleText;
    private Button saveBtn, deleteBtn, uploadPicBtn;
    private ProgressBar progressBar;

    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private String profileImageBase64;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        initializeViews();
        fetchUserProfile();

        uploadPicBtn.setOnClickListener(v -> openImagePicker());
        saveBtn.setOnClickListener(v -> saveProfileChanges());
        deleteBtn.setOnClickListener(v -> confirmDeleteAccount());
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.profileImage);
        nameInput = findViewById(R.id.nameInput);
        institutionInput = findViewById(R.id.institutionInput);
        subjectsInput = findViewById(R.id.subjectsInput);
        academicLevelInput = findViewById(R.id.academicLevelInput);
        ageInput = findViewById(R.id.ageInput);
        goalsInput = findViewById(R.id.goalsInput);
        degreesInput = findViewById(R.id.degreesInput);
        institutionsInput = findViewById(R.id.institutionsAttendedInput);
        experienceInput = findViewById(R.id.experienceInput);
        philosophyInput = findViewById(R.id.philosophyInput);
        emailText = findViewById(R.id.emailText);
        roleText = findViewById(R.id.roleText);
        saveBtn = findViewById(R.id.saveBtn);
        deleteBtn = findViewById(R.id.deleteBtn);
        uploadPicBtn = findViewById(R.id.uploadPicBtn);
        progressBar = findViewById(R.id.progressBar);
    }

    private void fetchUserProfile() {
        showLoading(true);
        userRef.get().addOnSuccessListener(snapshot -> {
            showLoading(false);
            if (snapshot.exists()) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    populateUI(user);
                }
            }
        }).addOnFailureListener(e -> {
            showLoading(false);
            Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
        });
    }

    private void populateUI(User user) {
        nameInput.setText(user.getName());
        institutionInput.setText(user.getInstitution());
        emailText.setText(user.getEmail());
        roleText.setText(user.getRole());
        profileImageBase64 = user.getProfileImage();

        if (profileImageBase64 != null) {
            byte[] decoded = Base64.decode(profileImageBase64, Base64.DEFAULT);
            profileImage.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length));
        }

        if ("Tutee".equals(user.getRole())) {
            subjectsInput.setText(user.getSubjects() != null ? String.join(", ", user.getSubjects()) : "");
            academicLevelInput.setText(user.getAcademicLevel());
            ageInput.setText(String.valueOf(user.getAge()));
            goalsInput.setText(user.getShortTermGoals());
        } else {
            degreesInput.setText(String.join(", ", user.getDegrees()));
            institutionsInput.setText(String.join(", ", user.getInstitutionsAttended()));
            experienceInput.setText(user.getTeachingExperience());
            philosophyInput.setText(user.getTeachingPhilosophy());
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            profileImage.setImageURI(selectedImageUri);
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] imageBytes = baos.toByteArray();
                profileImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            } catch (IOException e) {
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveProfileChanges() {
        showLoading(true);
        userRef.child("name").setValue(nameInput.getText().toString().trim());
        userRef.child("institution").setValue(institutionInput.getText().toString().trim());
        userRef.child("profileImage").setValue(profileImageBase64);

        if (roleText.getText().toString().equals("Tutee")) {
            List<String> subjectList = Arrays.asList(subjectsInput.getText().toString().trim().split(",[ ]*"));
            userRef.child("subjects").setValue(subjectList);
            userRef.child("academicLevel").setValue(academicLevelInput.getText().toString().trim());
            userRef.child("age").setValue(Integer.parseInt(ageInput.getText().toString().trim()));
            userRef.child("shortTermGoals").setValue(goalsInput.getText().toString().trim());
        } else {
            userRef.child("degrees").setValue(Arrays.asList(degreesInput.getText().toString().split(",[ ]*")));
            userRef.child("institutionsAttended").setValue(Arrays.asList(institutionsInput.getText().toString().split(",[ ]*")));
            userRef.child("teachingExperience").setValue(experienceInput.getText().toString().trim());
            userRef.child("teachingPhilosophy").setValue(philosophyInput.getText().toString().trim());
        }

        showLoading(false);
        Toast.makeText(this, "Profile updated.", Toast.LENGTH_SHORT).show();
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account?")
                .setPositiveButton("Yes", (dialog, which) -> deleteAccount())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteAccount() {
        showLoading(true);
        userRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                currentUser.delete().addOnCompleteListener(del -> {
                    showLoading(false);
                    Toast.makeText(this, "Account deleted.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                });
            } else {
                showLoading(false);
                Toast.makeText(this, "Failed to delete account.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveBtn.setEnabled(!loading);
        deleteBtn.setEnabled(!loading);
    }
}
