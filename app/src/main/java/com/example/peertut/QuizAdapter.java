package com.example.peertut;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> {

    private List<UploadQuizActivity.Quiz> quizzes;
    private String currentUserId;
    private OnQuizClickListener quizClickListener;

    public interface OnQuizClickListener {
        void onQuizClick(UploadQuizActivity.Quiz quiz);
    }

    public QuizAdapter(List<UploadQuizActivity.Quiz> quizzes, String currentUserId, OnQuizClickListener listener) {
        this.quizzes = quizzes;
        this.currentUserId = currentUserId;
        this.quizClickListener = listener;
    }

    @Override
    public QuizViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quiz, parent, false);
        return new QuizViewHolder(view);
    }

    @Override
    public void onBindViewHolder(QuizViewHolder holder, int position) {
        UploadQuizActivity.Quiz quiz = quizzes.get(position);

        // Set basic info
        holder.quizTitle.setText(quiz.title != null ? quiz.title : "Untitled Quiz");
        holder.quizSubject.setText(quiz.subject != null ? quiz.subject : "No Subject");

        // Set due date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        holder.quizDueDate.setText("Due: " + sdf.format(new Date(quiz.dueDate)));

        // Calculate attempts
        Map<String, Integer> attempts = quiz.attempts != null ? quiz.attempts : Map.of();
        int attemptsUsed = attempts.getOrDefault(currentUserId, 0);
        int attemptsLeft = quiz.maxAttempts - attemptsUsed;
        holder.quizAttempts.setText("Attempts left: " + attemptsLeft);

        // Handle quiz status
        boolean isExpired = quiz.dueDate < System.currentTimeMillis();
        boolean isCompleted = attemptsUsed >= quiz.maxAttempts;

        if (isExpired || isCompleted) {
            holder.itemView.setAlpha(0.6f);
            holder.quizStatus.setText(isExpired ? "Expired" : "Completed");
            holder.quizStatus.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setAlpha(1f);
            holder.quizStatus.setVisibility(View.GONE);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (isExpired) {
                Toast.makeText(v.getContext(), "This quiz has expired", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isCompleted) {
                Toast.makeText(v.getContext(), "No attempts remaining", Toast.LENGTH_SHORT).show();
                return;
            }
            quizClickListener.onQuizClick(quiz);
        });
    }

    @Override
    public int getItemCount() {
        return quizzes.size();
    }

    public static class QuizViewHolder extends RecyclerView.ViewHolder {
        TextView quizTitle, quizSubject, quizDueDate, quizAttempts, quizStatus;

        public QuizViewHolder(View itemView) {
            super(itemView);
            quizTitle = itemView.findViewById(R.id.quizTitle);
            quizSubject = itemView.findViewById(R.id.quizSubject);
            quizDueDate = itemView.findViewById(R.id.quizDueDate);
            quizAttempts = itemView.findViewById(R.id.quizAttempts);
            quizStatus = itemView.findViewById(R.id.quizStatus);
        }
    }
}