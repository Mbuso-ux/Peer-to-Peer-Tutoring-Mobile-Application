package com.example.peertut;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class TakeQuizActivity extends AppCompatActivity {

    private TextView quizTitleText, questionText, progressText;
    private RadioGroup optionsRadioGroup;
    private Button nextButton;
    private DatabaseReference quizzesRef;
    private UploadQuizActivity.Quiz quiz;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private int currentAttempt = 1;
    private int maxAttempts = 1;
    private boolean showAnswers;
    private Map<Integer, Integer> userAnswers = new HashMap<>();
    private String quizId, tuteeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_quiz);
        initializeFirebase();
        verifyUserSession();
        initializeViews();
        loadQuizData();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void initializeFirebase() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        tuteeId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        quizzesRef = FirebaseDatabase.getInstance().getReference("quizzes");
    }

    private void verifyUserSession() {
        if (tuteeId == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void initializeViews() {
        quizTitleText = findViewById(R.id.quizTitleText);
        questionText = findViewById(R.id.questionText);
        optionsRadioGroup = findViewById(R.id.optionsRadioGroup);
        nextButton = findViewById(R.id.nextButton);
        progressText = findViewById(R.id.progressText);
        nextButton.setOnClickListener(v -> handleNextButton());
        quizId = getIntent().getStringExtra("quizId");
    }

    private void loadQuizData() {
        quizzesRef.child(quizId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                quiz = snapshot.getValue(UploadQuizActivity.Quiz.class);
                validateQuizData();
                initializeQuizState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleLoadError();
            }
        });
    }

    private void validateQuizData() {
        if (quiz == null || quiz.questions == null || quiz.questions.isEmpty()) {
            Toast.makeText(this, "Quiz data not available", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeQuizState() {
        maxAttempts = quiz.maxAttempts;
        showAnswers = quiz.showAnswers;
        initializeMaps();
        checkAttempts();
    }

    private void initializeMaps() {
        if (quiz.attempts == null) quiz.attempts = new HashMap<>();
        if (quiz.scores == null) quiz.scores = new HashMap<>();
    }

    private void checkAttempts() {
        int attemptsUsed = quiz.attempts.getOrDefault(tuteeId, 0);
        currentAttempt = attemptsUsed + 1;

        if (attemptsUsed >= maxAttempts || quiz.dueDate < System.currentTimeMillis()) {
            showQuizCompletedDialog();
        } else {
            startQuiz();
        }
    }

    private void startQuiz() {
        quizTitleText.setText(quiz.title != null ? quiz.title : "Untitled Quiz");
        displayQuestion(currentQuestionIndex);
    }

    private void displayQuestion(int index) {
        if (index < 0 || index >= quiz.questions.size()) return;

        UploadQuizActivity.QuizQuestion question = quiz.questions.get(index);
        questionText.setText(question.question != null ? question.question : "No question text");
        progressText.setText((index + 1) + "/" + quiz.questions.size());
        optionsRadioGroup.removeAllViews();
        createRadioButtons(question);
        nextButton.setText(index == quiz.questions.size() - 1 ? "Submit" : "Next");
    }

    private void createRadioButtons(UploadQuizActivity.QuizQuestion question) {
        for (int i = 0; i < question.options.size(); i++) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(question.options.get(i) != null ?
                    question.options.get(i) : "Option " + (i + 1));
            radioButton.setId(i);
            optionsRadioGroup.addView(radioButton);
        }
    }

    private void handleNextButton() {
        int selectedId = optionsRadioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show();
            return;
        }

        saveAnswer(selectedId);
        handleQuestionNavigation();
    }

    private void saveAnswer(int selectedId) {
        userAnswers.put(currentQuestionIndex, selectedId);
        UploadQuizActivity.QuizQuestion currentQuestion = quiz.questions.get(currentQuestionIndex);
        if (selectedId == currentQuestion.correctAnswerIndex) score++;
    }

    private void handleQuestionNavigation() {
        if (currentQuestionIndex < quiz.questions.size() - 1) {
            currentQuestionIndex++;
            displayQuestion(currentQuestionIndex);
            optionsRadioGroup.clearCheck();
        } else {
            saveResults();
        }
    }

    private void saveResults() {
        float percentage = ((float) score / quiz.questions.size()) * 100;
        DatabaseReference quizRef = quizzesRef.child(quizId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("attempts/" + tuteeId, currentAttempt);

        Integer previousBest = quiz.scores.get(tuteeId);
        if (previousBest == null || percentage > previousBest) {
            updates.put("scores/" + tuteeId, Math.round(percentage));
        }

        quizRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                showResults(Math.round(percentage), previousBest);
            } else {
                handleSaveError();
            }
        });
    }

    private void showResults(int percentage, Integer previousBest) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quiz Results")
                .setMessage(createResultMessage(percentage, previousBest))
                .setNeutralButton("Finish", (dialog, which) -> finish());

        if (currentAttempt < maxAttempts) {
            builder.setNegativeButton("Try Again", (dialog, which) -> retakeQuiz());
        }
        if (showAnswers) {
            builder.setPositiveButton("View Answers", (dialog, which) -> showAnswers());
        }

        builder.show();
    }

    private String createResultMessage(int percentage, Integer previousBest) {
        String message = "Score: " + score + "/" + quiz.questions.size() +
                " (" + percentage + "%)\n";
        if (previousBest != null) message += "Previous best: " + previousBest + "%\n";
        message += "\nAttempts remaining: " + (maxAttempts - currentAttempt);
        return message;
    }

    private void retakeQuiz() {
        currentAttempt++;
        currentQuestionIndex = 0;
        score = 0;
        userAnswers.clear();
        displayQuestion(currentQuestionIndex);
    }

    private void showQuizCompletedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Quiz Completed")
                .setMessage("You have completed all attempts")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showAnswers() {
        Intent intent = new Intent(this, ViewAnswersActivity.class);
        intent.putExtra("quiz", quiz);
        intent.putExtra("userAnswers", new HashMap<>(userAnswers));
        startActivity(intent);
        finish();
    }

    private void handleLoadError() {
        Toast.makeText(this, "Failed to load quiz", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void handleSaveError() {
        Toast.makeText(this, "Failed to save results", Toast.LENGTH_SHORT).show();
        finish();
    }
}