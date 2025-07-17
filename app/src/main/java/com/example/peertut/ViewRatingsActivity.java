package com.example.peertut;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ViewRatingsActivity extends AppCompatActivity {

    private RecyclerView ratingsRecyclerView;
    private TextView emptyTextView;
    private RatingsAdapter adapter;
    private DatabaseReference ratingsRef, usersRef;
    private String rateeId;
    private Map<String, String> emailCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_ratings);

        // Initialize views
        ratingsRecyclerView = findViewById(R.id.ratingsRecyclerView);
        emptyTextView = findViewById(R.id.emptyTextView);

        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Your Ratings");
        }

        // Get rateeId from intent or current user
        rateeId = getIntent().getStringExtra("rateeId");
        if (rateeId == null) {
            rateeId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Setup RecyclerView
        ratingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RatingsAdapter(new ArrayList<>());
        ratingsRecyclerView.setAdapter(adapter);

        // Firebase references
        ratingsRef = FirebaseDatabase.getInstance().getReference("ratings");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        loadRatings();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadRatings() {
        ratingsRef.orderByChild("rateeId").equalTo(rateeId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Rating> ratings = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Rating rating = ds.getValue(Rating.class);
                            if (rating != null) {
                                ratings.add(rating);
                                cacheUserEmail(rating.getRaterId());
                            }
                        }
                        // Sort by timestamp (newest first)
                        Collections.sort(ratings, (r1, r2) ->
                                Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                        updateAdapter(ratings);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        emptyTextView.setText("Error loading ratings");
                        emptyTextView.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void cacheUserEmail(String userId) {
        if (emailCache.containsKey(userId)) return;

        usersRef.child(userId).child("email")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String email = snapshot.getValue(String.class);
                        if (email != null) {
                            emailCache.put(userId, email);
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
    }

    private void updateAdapter(List<Rating> ratings) {
        adapter.setRatings(ratings);
        emptyTextView.setVisibility(ratings.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private class RatingsAdapter extends RecyclerView.Adapter<RatingsAdapter.RatingViewHolder> {
        private List<Rating> ratings;

        public RatingsAdapter(List<Rating> ratings) {
            this.ratings = ratings;
        }

        public void setRatings(List<Rating> ratings) {
            this.ratings = ratings;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public RatingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_rating, parent, false);
            return new RatingViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RatingViewHolder holder, int position) {
            Rating rating = ratings.get(position);
            String email = emailCache.getOrDefault(rating.getRaterId(), "Anonymous");

            holder.raterName.setText(email);
            holder.ratingBar.setRating(rating.getRatingValue()); // This will now work
            holder.commentText.setText(rating.getComment());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            holder.dateText.setText(sdf.format(new Date(rating.getTimestamp())));
        }

        @Override
        public int getItemCount() {
            return ratings.size();
        }

        class RatingViewHolder extends RecyclerView.ViewHolder {
            TextView raterName, commentText, dateText;
            RatingBar ratingBar;

            public RatingViewHolder(@NonNull View itemView) {
                super(itemView);
                raterName = itemView.findViewById(R.id.raterName);
                ratingBar = itemView.findViewById(R.id.ratingBar);
                commentText = itemView.findViewById(R.id.commentText);
                dateText = itemView.findViewById(R.id.dateText);
            }
        }
    }
}