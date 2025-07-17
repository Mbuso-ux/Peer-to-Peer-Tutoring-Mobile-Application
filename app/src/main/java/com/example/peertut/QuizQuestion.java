package com.example.peertut;

import java.util.List;

public class QuizQuestion {
    public String question;
    public List<String> options;
    public int correctAnswerIndex;

    public QuizQuestion() { }

    public QuizQuestion(String question, List<String> options, int correctAnswerIndex) {
        this.question = question;
        this.options = options;
        this.correctAnswerIndex = correctAnswerIndex;
    }
}
