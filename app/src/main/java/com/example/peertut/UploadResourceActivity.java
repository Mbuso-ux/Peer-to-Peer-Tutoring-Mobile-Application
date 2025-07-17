package com.example.peertut;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class UploadResourceActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 101;

    private EditText titleInput, descriptionInput;
    private Spinner subjectSpinner, typeSpinner;
    private Button selectFileButton, uploadButton;
    private Uri fileUri;
    private FirebaseAuth auth;
    private DatabaseReference usersRef, resourcesRef;
    private List<String> subjectList = new ArrayList<>();
    private ArrayAdapter<String> subjectAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_resource);

        // Initialize views
        titleInput = findViewById(R.id.resourceTitleInput);
        descriptionInput = findViewById(R.id.resourceDescriptionInput);
        subjectSpinner = findViewById(R.id.subjectSpinner);
        typeSpinner = findViewById(R.id.resourceTypeSpinner);
        selectFileButton = findViewById(R.id.selectFileButton);
        uploadButton = findViewById(R.id.uploadResourceButton);

        // Firebase setup
        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        resourcesRef = FirebaseDatabase.getInstance().getReference("resources");

        // Setup spinners
        setupSpinners();
        loadTutorSubjects();

        selectFileButton.setOnClickListener(v -> openFileSelector());
        uploadButton.setOnClickListener(v -> uploadResource());
    }

    private void setupSpinners() {
        // Subject spinner
        subjectAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, subjectList);
        subjectAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        subjectSpinner.setAdapter(subjectAdapter);

        // Apply spinner theme
        subjectSpinner.setPopupBackgroundResource(R.color.colorPrimaryLight);

        // Type spinner
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                Arrays.asList("Lecture Notes", "Practice Problems", "Study Guide", "Video")
        );
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setPopupBackgroundResource(R.color.colorPrimaryLight);
    }

    private void loadTutorSubjects() {
        String uid = auth.getCurrentUser().getUid();
        usersRef.child(uid).child("subjects").addListenerForSingleValueEvent(new ValueEventListener() {
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
                Toast.makeText(UploadResourceActivity.this, "Failed to load subjects", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFileSelector() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "video/*",
                "image/*"
        });
        startActivityForResult(intent, FILE_SELECT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            fileUri = data.getData();
            selectFileButton.setText("File Selected: " + fileUri.getLastPathSegment());
        }
    }

    private void uploadResource() {
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String subject = (String) subjectSpinner.getSelectedItem();
        String type = (String) typeSpinner.getSelectedItem();

        if (TextUtils.isEmpty(title) || fileUri == null || TextUtils.isEmpty(subject)) {
            Toast.makeText(this, "Title, File, and Subject are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String tutorUid = auth.getCurrentUser().getUid();
        String resourceId = resourcesRef.push().getKey();

        // Store both download URL and file URI (you'll need to implement file upload to Firebase Storage)
        Map<String, Object> resource = new HashMap<>();
        resource.put("title", title);
        resource.put("description", description);
        resource.put("fileUri", fileUri.toString());
        resource.put("type", type);
        resource.put("subject", subject);
        resource.put("uploadedBy", tutorUid);
        resource.put("timestamp", System.currentTimeMillis());
        resource.put("resourceId", resourceId);

        resourcesRef.child(resourceId).setValue(resource)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Resource uploaded successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}