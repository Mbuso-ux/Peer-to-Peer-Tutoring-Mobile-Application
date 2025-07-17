package com.example.peertut;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class ViewResourcesActivity extends AppCompatActivity {

    private RecyclerView resourcesRecyclerView;
    private ProgressBar progressBar;
    private TextView noResourcesText;
    private ResourceAdapter adapter;
    private List<Resource> resourceList = new ArrayList<>();
    private DatabaseReference resourcesRef, registrationsRef;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_resources);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        resourcesRecyclerView = findViewById(R.id.resourcesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        noResourcesText = findViewById(R.id.noResourcesText);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Setup filter button
        MaterialButton filterButton = findViewById(R.id.filterButton);
        filterButton.setOnClickListener(v -> showFilterDialog());

        // Firebase setup
        auth = FirebaseAuth.getInstance();
        resourcesRef = FirebaseDatabase.getInstance().getReference("resources");
        registrationsRef = FirebaseDatabase.getInstance().getReference("registrations");

        // Setup RecyclerView
        resourcesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResourceAdapter(resourceList);
        resourcesRecyclerView.setAdapter(adapter);

        loadRegisteredSubjectsResources();
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter Resources");

        // Inflate the filter layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_resources, null);
        builder.setView(dialogView);

        // Initialize filter views
        Spinner subjectSpinner = dialogView.findViewById(R.id.subjectSpinner);
        Spinner typeSpinner = dialogView.findViewById(R.id.typeSpinner);

        // Get unique subjects and types from resources
        Set<String> subjects = new HashSet<>();
        Set<String> types = new HashSet<>();
        for (Resource resource : resourceList) {
            subjects.add(resource.getSubject());
            types.add(resource.getType());
        }

        // Convert sets to lists and sort them
        List<String> subjectList = new ArrayList<>(subjects);
        List<String> typeList = new ArrayList<>(types);
        Collections.sort(subjectList);
        Collections.sort(typeList);

        // Add "All" option at the beginning
        subjectList.add(0, "All Subjects");
        typeList.add(0, "All Types");

        // Set up adapters
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, subjectList);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjectSpinner.setAdapter(subjectAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, typeList);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        // Set positive button
        builder.setPositiveButton("Apply", (dialog, which) -> {
            String selectedSubject = subjectSpinner.getSelectedItem().toString();
            String selectedType = typeSpinner.getSelectedItem().toString();

            // Filter the resource list
            List<Resource> filteredList = new ArrayList<>();
            for (Resource resource : resourceList) {
                boolean subjectMatch = selectedSubject.equals("All Subjects") ||
                        resource.getSubject().equals(selectedSubject);
                boolean typeMatch = selectedType.equals("All Types") ||
                        resource.getType().equals(selectedType);

                if (subjectMatch && typeMatch) {
                    filteredList.add(resource);
                }
            }

            // Update adapter with filtered list
            adapter = new ResourceAdapter(filteredList);
            resourcesRecyclerView.setAdapter(adapter);

            if (filteredList.isEmpty()) {
                noResourcesText.setVisibility(View.VISIBLE);
                noResourcesText.setText("No resources match your filters");
                resourcesRecyclerView.setVisibility(View.GONE);
            } else {
                noResourcesText.setVisibility(View.GONE);
                resourcesRecyclerView.setVisibility(View.VISIBLE);
            }
        });

        // Set negative button
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Set neutral button for reset
        builder.setNeutralButton("Reset", (dialog, which) -> {
            // Reset to original list
            adapter = new ResourceAdapter(resourceList);
            resourcesRecyclerView.setAdapter(adapter);
            noResourcesText.setVisibility(resourceList.isEmpty() ? View.VISIBLE : View.GONE);
            resourcesRecyclerView.setVisibility(resourceList.isEmpty() ? View.GONE : View.VISIBLE);
        });

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void loadRegisteredSubjectsResources() {
        String tuteeId = auth.getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);
        noResourcesText.setVisibility(View.GONE);

        // First get all subjects the tutee is registered for
        registrationsRef.child(tuteeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> registeredSubjects = new HashSet<>();
                for (DataSnapshot tutorSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot subjectSnapshot : tutorSnapshot.getChildren()) {
                        registeredSubjects.add(subjectSnapshot.getKey());
                    }
                }

                if (registeredSubjects.isEmpty()) {
                    showNoResources();
                    return;
                }

                // Now get resources for these subjects
                resourcesRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        resourceList.clear();
                        for (DataSnapshot resourceSnapshot : snapshot.getChildren()) {
                            Resource resource = resourceSnapshot.getValue(Resource.class);
                            if (resource != null && registeredSubjects.contains(resource.getSubject())) {
                                resourceList.add(resource);
                            }
                        }

                        if (resourceList.isEmpty()) {
                            showNoResources();
                        } else {
                            Collections.reverse(resourceList); // Newest first
                            adapter.notifyDataSetChanged();
                            progressBar.setVisibility(View.GONE);
                            resourcesRecyclerView.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showNoResources();
                        Toast.makeText(ViewResourcesActivity.this, "Failed to load resources", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showNoResources();
                Toast.makeText(ViewResourcesActivity.this, "Failed to load registrations", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showNoResources() {
        progressBar.setVisibility(View.GONE);
        resourcesRecyclerView.setVisibility(View.GONE);
        noResourcesText.setVisibility(View.VISIBLE);
        noResourcesText.setText("No resources available for your registered subjects");
    }

    // Resource Model Class
    public static class Resource {
        private String title;
        private String description;
        private String fileUri;
        private String type;
        private String subject;
        private String uploadedBy;
        private long timestamp;
        private String resourceId;

        // Add getters and setters
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getFileUri() { return fileUri; }
        public String getType() { return type; }
        public String getSubject() { return subject; }
        public String getUploadedBy() { return uploadedBy; }
        public long getTimestamp() { return timestamp; }
        public String getResourceId() { return resourceId; }
    }

    // RecyclerView Adapter
    private class ResourceAdapter extends RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder> {

        private List<Resource> resources;

        public ResourceAdapter(List<Resource> resources) {
            this.resources = resources;
        }

        @NonNull
        @Override
        public ResourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_resource, parent, false);
            return new ResourceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ResourceViewHolder holder, int position) {
            Resource resource = resources.get(position);
            holder.titleText.setText(resource.getTitle());
            holder.descriptionText.setText(resource.getDescription());
            holder.typeText.setText(resource.getType());
            holder.subjectText.setText(resource.getSubject());

            holder.itemView.setOnClickListener(v -> {
                // Open the resource (you'll need to handle different file types)
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(resource.getFileUri()));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(ViewResourcesActivity.this,
                            "No app found to open this file", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return resources.size();
        }

        class ResourceViewHolder extends RecyclerView.ViewHolder {
            TextView titleText, descriptionText, typeText, subjectText, uploadedByText;

            public ResourceViewHolder(@NonNull View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.resourceTitle);
                descriptionText = itemView.findViewById(R.id.resourceDescription);
                typeText = itemView.findViewById(R.id.resourceType);
                subjectText = itemView.findViewById(R.id.resourceSubject);
                uploadedByText = itemView.findViewById(R.id.uploadedByText);
            }
        }
    }
}