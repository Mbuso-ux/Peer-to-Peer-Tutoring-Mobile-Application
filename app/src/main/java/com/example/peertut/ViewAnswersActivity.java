package com.example.peertut;

import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class ViewAnswersActivity extends AppCompatActivity {

    private RecyclerView answersRecyclerView;
    private UploadQuizActivity.Quiz quiz;
    private Map<Integer, Integer> userAnswers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_answers);

        // Get quiz and answers from intent
        quiz = getIntent().getParcelableExtra("quiz");
        userAnswers = (Map<Integer, Integer>) getIntent().getSerializableExtra("userAnswers");

        if (quiz == null || quiz.questions == null || userAnswers == null) {
            Toast.makeText(this, "Error loading answers", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        answersRecyclerView = findViewById(R.id.answersRecyclerView);
        answersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        answersRecyclerView.setAdapter(new AnswersAdapter());
    }

    private class AnswersAdapter extends RecyclerView.Adapter<AnswersAdapter.AnswerViewHolder> {
        @Override
        public AnswerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_answer, parent, false);
            return new AnswerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(AnswerViewHolder holder, int position) {
            UploadQuizActivity.QuizQuestion question = quiz.questions.get(position);
            Integer userAnswer = userAnswers.get(position);

            holder.questionText.setText((position + 1) + ". " + question.question);

            // Display all options
            holder.option1.setText(question.options.get(0));
            holder.option2.setText(question.options.get(1));
            holder.option3.setText(question.options.get(2));
            holder.option4.setText(question.options.get(3));

            // Reset all options to default appearance
            resetOptionAppearance(holder.option1);
            resetOptionAppearance(holder.option2);
            resetOptionAppearance(holder.option3);
            resetOptionAppearance(holder.option4);

            // Highlight correct answer in green
            switch (question.correctAnswerIndex) {
                case 0: highlightOption(holder.option1, true); break;
                case 1: highlightOption(holder.option2, true); break;
                case 2: highlightOption(holder.option3, true); break;
                case 3: highlightOption(holder.option4, true); break;
            }

            // Highlight user's answer if incorrect in red
            if (userAnswer != null && userAnswer != question.correctAnswerIndex) {
                switch (userAnswer) {
                    case 0: highlightOption(holder.option1, false); break;
                    case 1: highlightOption(holder.option2, false); break;
                    case 2: highlightOption(holder.option3, false); break;
                    case 3: highlightOption(holder.option4, false); break;
                }
            }
        }

        private void resetOptionAppearance(TextView option) {
            option.setBackgroundColor(Color.TRANSPARENT);
            option.setTextColor(Color.BLACK);
        }

        private void highlightOption(TextView option, boolean isCorrect) {
            option.setBackgroundColor(isCorrect ? Color.GREEN : Color.RED);
            option.setTextColor(Color.WHITE);
        }

        @Override
        public int getItemCount() {
            return quiz.questions.size();
        }

        class AnswerViewHolder extends RecyclerView.ViewHolder {
            TextView questionText, option1, option2, option3, option4;

            AnswerViewHolder(View itemView) {
                super(itemView);
                questionText = itemView.findViewById(R.id.questionText);
                option1 = itemView.findViewById(R.id.option1);
                option2 = itemView.findViewById(R.id.option2);
                option3 = itemView.findViewById(R.id.option3);
                option4 = itemView.findViewById(R.id.option4);
            }
        }
    }
}