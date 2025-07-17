package com.example.peertut;

import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.*;

public class ViewScoresActivity extends AppCompatActivity {

    private RecyclerView scoresRecyclerView;
    private TextView noScoresText; // Add this line
    private DatabaseReference quizzesRef;
    private String subject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_scores);

        // Initialize the noScoresText view
        noScoresText = findViewById(R.id.noScoresText);
        scoresRecyclerView = findViewById(R.id.scoresRecyclerView);
        scoresRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        subject = getIntent().getStringExtra("subject");
        quizzesRef = FirebaseDatabase.getInstance().getReference("quizzes");
        loadScores();
    }

    private void loadScores() {
        quizzesRef.orderByChild("subject").equalTo(subject)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<ScoreItem> scores = new ArrayList<>();
                        String tuteeId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        for (DataSnapshot quizSnapshot : snapshot.getChildren()) {
                            UploadQuizActivity.Quiz quiz = quizSnapshot.getValue(UploadQuizActivity.Quiz.class);
                            if (quiz != null && quiz.scores != null && quiz.scores.containsKey(tuteeId)) {
                                scores.add(new ScoreItem(quiz.title, quiz.scores.get(tuteeId)));
                            }
                        }

                        if (scores.isEmpty()) {
                            showNoScores();
                        } else {
                            scoresRecyclerView.setAdapter(new ScoresAdapter(scores));
                            noScoresText.setVisibility(View.GONE);
                            scoresRecyclerView.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showNoScores();
                    }
                });
    }

    private void showNoScores() {
        noScoresText.setVisibility(View.VISIBLE);
        scoresRecyclerView.setVisibility(View.GONE);
        noScoresText.setText("No quiz scores available for this subject");
    }

    private static class ScoreItem {
        String quizTitle;
        int score;

        public ScoreItem(String quizTitle, int score) {
            this.quizTitle = quizTitle;
            this.score = score;
        }
    }

    private class ScoresAdapter extends RecyclerView.Adapter<ScoresAdapter.ScoreViewHolder> {
        private List<ScoreItem> scores;

        public ScoresAdapter(List<ScoreItem> scores) {
            this.scores = scores;
        }

        @NonNull
        @Override
        public ScoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_score, parent, false);
            return new ScoreViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ScoreViewHolder holder, int position) {
            ScoreItem item = scores.get(position);
            holder.quizTitleText.setText(item.quizTitle);
            holder.scoreText.setText(item.score + "%");

            // Set color based on score
            if (item.score >= 80) {
                holder.scoreText.setTextColor(Color.parseColor("#4CAF50")); // Green
            } else if (item.score >= 50) {
                holder.scoreText.setTextColor(Color.parseColor("#FFC107")); // Yellow
            } else {
                holder.scoreText.setTextColor(Color.parseColor("#F44336")); // Red
            }
        }

        @Override
        public int getItemCount() {
            return scores.size();
        }

        class ScoreViewHolder extends RecyclerView.ViewHolder {
            TextView quizTitleText, scoreText;

            public ScoreViewHolder(@NonNull View itemView) {
                super(itemView);
                quizTitleText = itemView.findViewById(R.id.quizTitleText);
                scoreText = itemView.findViewById(R.id.scoreText);
            }
        }
    }
}