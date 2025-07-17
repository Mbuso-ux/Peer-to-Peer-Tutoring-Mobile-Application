package com.example.peertut;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ViewQuizzesActivity extends AppCompatActivity {

    private RecyclerView quizzesRecyclerView;
    private ProgressBar progressBar;
    private TextView noQuizzesText;
    private QuizAdapter adapter;
    private List<UploadQuizActivity.Quiz> quizList = new ArrayList<>();
    private DatabaseReference quizzesRef, registrationsRef;
    private FirebaseAuth auth;
    private String currentUserId;
    private Set<String> registeredSubjects = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_quizzes);

        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        quizzesRef = FirebaseDatabase.getInstance().getReference("quizzes");
        registrationsRef = FirebaseDatabase.getInstance().getReference("registrations");

        quizzesRecyclerView = findViewById(R.id.quizzesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        noQuizzesText = findViewById(R.id.noQuizzesText);

        quizzesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuizAdapter(quizList);
        quizzesRecyclerView.setAdapter(adapter);

        loadRegisteredSubjects();
    }

    private void loadRegisteredSubjects() {
        if (currentUserId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        noQuizzesText.setVisibility(View.GONE);

        registrationsRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                registeredSubjects.clear();
                for (DataSnapshot tutorSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot subjectSnapshot : tutorSnapshot.getChildren()) {
                        String subject = subjectSnapshot.getKey();
                        if (subject != null) {
                            registeredSubjects.add(subject.toLowerCase().trim());
                        }
                    }
                }
                loadQuizzesForRegisteredSubjects();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                noQuizzesText.setText("Failed to load subjects");
                noQuizzesText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadQuizzesForRegisteredSubjects() {
        quizzesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                quizList.clear();
                for (DataSnapshot quizSnapshot : snapshot.getChildren()) {
                    UploadQuizActivity.Quiz quiz = quizSnapshot.getValue(UploadQuizActivity.Quiz.class);
                    if (quiz != null && isValidQuiz(quiz)) {
                        quizList.add(quiz);
                    }
                }

                Collections.sort(quizList, (q1, q2) -> Long.compare(q1.dueDate, q2.dueDate));
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleLoadError("Failed to load quizzes");
            }
        });
    }

    private boolean isValidQuiz(UploadQuizActivity.Quiz quiz) {
        return quiz.subject != null &&
                registeredSubjects.contains(quiz.subject.toLowerCase().trim()) &&
                quiz.dueDate > System.currentTimeMillis() &&
                quiz.questions != null &&
                !quiz.questions.isEmpty();
    }

    private void updateUI() {
        if (quizList.isEmpty()) {
            noQuizzesText.setText("No available quizzes");
            noQuizzesText.setVisibility(View.VISIBLE);
            quizzesRecyclerView.setVisibility(View.GONE);
        } else {
            adapter.notifyDataSetChanged();
            quizzesRecyclerView.setVisibility(View.VISIBLE);
            noQuizzesText.setVisibility(View.GONE);
        }
        progressBar.setVisibility(View.GONE);
    }

    private void handleLoadError(String message) {
        progressBar.setVisibility(View.GONE);
        noQuizzesText.setText(message);
        noQuizzesText.setVisibility(View.VISIBLE);
        quizzesRecyclerView.setVisibility(View.GONE);
    }

    class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> {
        private List<UploadQuizActivity.Quiz> quizzes;

        public QuizAdapter(List<UploadQuizActivity.Quiz> quizzes) {
            this.quizzes = quizzes;
        }

        @NonNull
        @Override
        public QuizViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quiz, parent, false);
            return new QuizViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull QuizViewHolder holder, int position) {
            UploadQuizActivity.Quiz quiz = quizzes.get(position);
            setupQuizItem(holder, quiz);
            setupClickListeners(holder, quiz);
        }

        private void setupQuizItem(QuizViewHolder holder, UploadQuizActivity.Quiz quiz) {
            holder.quizTitle.setText(quiz.title != null ? quiz.title : "Untitled Quiz");
            holder.quizSubject.setText(quiz.subject != null ? quiz.subject : "No Subject");

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.quizDueDate.setText("Due: " + sdf.format(new Date(quiz.dueDate)));

            int attemptsUsed = quiz.attempts != null ?
                    quiz.attempts.getOrDefault(currentUserId, 0) : 0;
            holder.quizAttempts.setText("Attempts left: " + (quiz.maxAttempts - attemptsUsed));

            updateQuizStatus(holder, quiz, attemptsUsed);
        }

        private void updateQuizStatus(QuizViewHolder holder, UploadQuizActivity.Quiz quiz, int attemptsUsed) {
            boolean quizClosed = attemptsUsed >= quiz.maxAttempts;
            boolean quizExpired = quiz.dueDate < System.currentTimeMillis();

            if (quizClosed || quizExpired) {
                holder.itemView.setAlpha(0.6f);
                holder.quizStatus.setText(quizExpired ? "Expired" : "Completed");
                holder.quizStatus.setVisibility(View.VISIBLE);
            } else {
                holder.itemView.setAlpha(1f);
                holder.quizStatus.setVisibility(View.GONE);
            }
        }

        private void setupClickListeners(QuizViewHolder holder, UploadQuizActivity.Quiz quiz) {
            holder.itemView.setOnClickListener(v -> {
                if (quiz.dueDate < System.currentTimeMillis()) {
                    Toast.makeText(ViewQuizzesActivity.this,
                            "This quiz has expired", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (auth.getCurrentUser() == null) {
                    promptReLogin();
                    return;
                }

                launchQuizActivity(quiz);
            });
        }

        private void promptReLogin() {
            Toast.makeText(ViewQuizzesActivity.this,
                    "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
        }

        private void launchQuizActivity(UploadQuizActivity.Quiz quiz) {
            Intent intent = new Intent(ViewQuizzesActivity.this, TakeQuizActivity.class);
            intent.putExtra("quizId", quiz.quizId);
            startActivity(intent);
        }

        @Override
        public int getItemCount() {
            return quizzes.size();
        }

        class QuizViewHolder extends RecyclerView.ViewHolder {
            TextView quizTitle, quizSubject, quizDueDate, quizAttempts, quizStatus;

            public QuizViewHolder(@NonNull View itemView) {
                super(itemView);
                quizTitle = itemView.findViewById(R.id.quizTitle);
                quizSubject = itemView.findViewById(R.id.quizSubject);
                quizDueDate = itemView.findViewById(R.id.quizDueDate);
                quizAttempts = itemView.findViewById(R.id.quizAttempts);
                quizStatus = itemView.findViewById(R.id.quizStatus);
            }
        }
    }
}
